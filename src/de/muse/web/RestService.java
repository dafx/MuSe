/*
 * Copyright (C) 2014 University of Freiburg.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.muse.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import de.muse.api.Recommender;
import de.muse.config.ApplicationConfig;
import de.muse.config.RecommenderConfig;
import de.muse.config.RecommenderConfig.Config;
import de.muse.data.charts.NeighborCharts;
import de.muse.data.charts.TagCharts;
import de.muse.data.social.lastfm.LastFmConnector;
import de.muse.evaluation.Activity;
import de.muse.evaluation.Evaluation;
import de.muse.evaluation.EvaluationData;
import de.muse.evaluation.Group;
import de.muse.evaluation.Statistics;
import de.muse.evaluation.result.EvaluationDataProvider;
import de.muse.evaluation.result.EvaluationResults;
import de.muse.recommendation.ListComposer;
import de.muse.recommendation.MuseRecommendation;
import de.muse.recommendation.RecommenderData;
import de.muse.user.MuseUser;
import de.muse.user.Option;
import de.muse.user.UserData;
import de.muse.utility.Database;

/**
 * Rest interface for the frontend-backend communication.
 */

@Path("")
public class RestService {
	private static final String JAR_PATH = ApplicationConfig.PERM_DIR;
	private static final Type MAP_TYPE = new TypeToken<HashMap<Integer, Config>>() {
	}.getType();
	private static final Logger LOG = LoggerFactory.getLogger(RestService.class
			.getName());

	/**
	 * Receive a recommendation list from the Server. Invoked when client calls
	 * the corresponding path (/MuSe/API/recommendations/getRecommendations) and
	 * specifies JSON as accept type.
	 * 
	 * @param name
	 *            Name of the user.
	 * @return List of recommendations represented as JSON array.
	 */
	@GET
	@Path("getRecommendations/user/{name}")
	@Produces("application/json")
	public String getRecommendations(@PathParam("name") String name) {
		Gson gson = new Gson();
		List<MuseRecommendation> recs;
		try {
			recs = RecommenderData.getCurrentRecommendationList(name);
		} catch (SQLException e) {
			LOG.warn("Couldn't get recommendation list for: " + name, e);
			throw new MuseWebException("Fetching from database failed.");
		}
		LOG.info("API: Got list for user: " + name);
		// Check if recommendations are all used
		if (recs.isEmpty()) {
			LOG.warn("Recommendations for user '" + name + "' are out.");
		}
		String recommendations = gson.toJson(recs);
		return recommendations;
	}

	/**
	 * Create a recommendation list for a certain user. Invoked when client
	 * calls the corresponding path.
	 * (/MuSe/API/recommendations/createRecommendations).
	 * 
	 * @param user
	 *            Name of the user.
	 * @param options
	 *            Map of options to option values.
	 */
	@POST
	@Path("createRecommendations/user/{name}")
	public void createRecommendations(@PathParam("name") String name) {
		try {
			MuseUser user = new MuseUser(name);
			// Fetch options of the user
			Option opt = UserData.fetchOptions(name);
			if (opt.getRecommenders().isEmpty()) {
				throw new MuseWebException(
						"Please choose a recommender in the settings.");
			}

			// Check for evaluation participant
			int evalId = EvaluationData.getIdForParticipant(name);

			ListComposer.createRecommendationList(user, opt.getBehavior(),
					evalId, opt.getRecommenders());
		} catch (SQLException e) {
			LOG.warn("Couldn't save recommendations to database for: " + name,
					e);
			throw new MuseWebException("Creating recommendations failed.");
		}
		LOG.info("API: Created new list for user: " + name);
	}

	/**
	 * Update recommender options of a user.
	 * 
	 * @param name
	 *            Name of the user
	 * @param options
	 *            JSON Serialized options
	 */
	@PUT
	@Path("updateOptions/user/{name}")
	@Consumes("application/json")
	public void updateOptions(@PathParam("name") String name, String options) {
		// Parse JSON string to option object
		Option opt = new Gson().fromJson(options, Option.class);
		try {
			UserData.saveOptions(opt.getBehavior(), opt.getRecommenders(), name);
		} catch (SQLException e) {
			LOG.warn("Couldn't update behavior for user: " + name, e);
			throw new MuseWebException("Saving to database failed.");
		}
		LOG.info("API: Saved options for user: " + name);
	}

	/**
	 * Receive options of a user.
	 * 
	 * @param name
	 *            Name of the user
	 * @param options
	 *            JSON Serialized options
	 */
	@GET
	@Path("getOptions/user/{name}")
	@Produces("application/json")
	public String getOptions(@PathParam("name") String name) {
		String recommenders = new Gson().toJson(UserData.fetchOptions(name));
		return recommenders;
	}

	/**
	 * Get the recommender configuration.
	 * 
	 * @return List of config items as JSON array. A config item is represented
	 *         by id, classpath and status.
	 */
	@GET
	@Path("getConfig")
	@Produces("application/json")
	public String getConfig() {
		Gson gson = new GsonBuilder().excludeFieldsWithModifiers(
				java.lang.reflect.Modifier.TRANSIENT).create();
		HashMap<Integer, Config> config = gson.fromJson(
				RecommenderConfig.getConfigString(), MAP_TYPE);
		HashMap<Integer, Recommender> recs = new HashMap<Integer, Recommender>(
				RecommenderConfig.getRecommenders());

		// Serialize config information
		List<Config> confList = new ArrayList<Config>();
		for (Integer id : config.keySet()) {
			Config conf = config.get(id);
			conf.setName(recs.get(id).getName());
			confList.add(conf);
		}
		return gson.toJson(confList);
	}

	/**
	 * Update the recommender configuration.
	 * 
	 * @param updateMap
	 *            Map of recommenderID -> status
	 */
	@PUT
	@Path("updateConfig")
	@Consumes("application/x-www-form-urlencoded")
	public void updateConfig(MultivaluedMap<String, String> updateMap) {
		try {
			RecommenderConfig.updateStatus(updateMap);
		} catch (IOException e) {
			LOG.warn("Couldn't update config.", e);
			throw new MuseWebException(
					"Updating Config failed. Please try again.");
		} catch (URISyntaxException e) {
			LOG.warn("Couldn't update config.", e);
			throw new MuseWebException(
					"Updating Config failed. Please try again.");
		}
	}

	@POST
	@Path("/addRecommender")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public void uploadFile(
			@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
			@FormDataParam("classname") String classname) {
		int nextID;
		try {
			nextID = RecommenderData.getNextRecommenderId();
		} catch (SQLException e) {
			LOG.warn("Fetching next recommender ID failed.", e);
			throw new MuseWebException("Uploading recommender failed.");
		}
		// Save the file to the server
		String filePath = JAR_PATH + nextID;
		try {
			OutputStream outpuStream = new FileOutputStream(new File(filePath));
			int read = 0;
			byte[] bytes = new byte[1024];

			outpuStream = new FileOutputStream(new File(filePath));
			while ((read = fileInputStream.read(bytes)) != -1) {
				outpuStream.write(bytes, 0, read);
			}
			outpuStream.flush();
			outpuStream.close();
		} catch (IOException e) {
			LOG.warn("Uploading recommender failed.", e);
			throw new MuseWebException("Uploading recommender failed.");
		}

		Recommender rec;
		try {
			// Load jar to classpath
			rec = RecommenderConfig.addRecommender(filePath, classname, nextID);
		} catch (MalformedURLException e) {
			LOG.warn("Jar URL couldn't be loaded", e.getMessage());
			throw new MuseWebException("Jar faulty. Couldn't be processed.");
		} catch (ClassNotFoundException e) {
			LOG.warn("Class couldn't be loaded", e.getMessage());
			throw new MuseWebException("Class couldn't be loaded.");
		} catch (InstantiationException e) {
			LOG.warn("Class couldn't be instantiated", e.getMessage());
			throw new MuseWebException("Class couldn't be instantiated.");
		} catch (Exception e) {
			LOG.warn("Class couldn't be instantiated", e.getMessage());
			throw new MuseWebException("Class couldn't be accessed.");
		}

		// Check for errors, remove if not working properly
		try {
			rec.getName();
			rec.getExplanation();
			rec.getTagDistribution();
		} catch (Exception e) {
			LOG.warn("Recommender not working as exptected.", e);
			LOG.warn("Removing recommender.", e);
			RecommenderConfig.removeRecommender(rec.getID());
			throw new MuseWebException("Recommender not working as exptected.");
		}
		LOG.warn("Recommender jar saved to server location : " + filePath);
	}

	@POST
	@Path("/removeRecommender")
	@Consumes(MediaType.TEXT_PLAIN)
	public void removeRecommender(String recid) {
		List<String> evals = RecommenderConfig.usedInEvaluations(Integer
				.valueOf(recid));
		if (!evals.isEmpty()) {
			String evalString = "";
			for (String eval : evals) {
				evalString += "<li>" + eval + "</li>";
			}
			throw new MuseWebException(
					"Recommender is used in evaluations. <br><ul>" + evalString
							+ "</ul>.");
		}
		try {
			// Get recommender id
			int id = Integer.valueOf(recid);
			// Remove recommender from recommender map
			RecommenderConfig.removeRecommender(id);
		} catch (NumberFormatException e) {
			LOG.warn("Couldn't parse recommender id.");
			throw new MuseWebException("Recommender id not found.");
		}
	}

	@POST
	@Path("/updateRecommender")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public void updateRecommender(
			@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
			@FormDataParam("classname") String classname,
			@FormDataParam("updateId") String updateId) {

		String filePath = JAR_PATH + updateId;

		// Save the file to the server
		try {
			OutputStream outpuStream = new FileOutputStream(new File(filePath));
			int read = 0;
			byte[] bytes = new byte[1024];

			outpuStream = new FileOutputStream(new File(filePath));
			while ((read = fileInputStream.read(bytes)) != -1) {
				outpuStream.write(bytes, 0, read);
			}
			outpuStream.flush();
			outpuStream.close();
		} catch (IOException e) {
			LOG.warn("Uploading recommender failed.", e);
			throw new MuseWebException("Uploading recommender failed.");
		}

		try {
			// Load jar to classpath
			RecommenderConfig.addRecommender(filePath, classname,
					Integer.valueOf(updateId));
		} catch (MalformedURLException e) {
			LOG.warn("Jar URL couldn't be loaded", e.getMessage());
			throw new MuseWebException("Jar faulty. Couldn't be processed.");
		} catch (ClassNotFoundException e) {
			LOG.warn("Class couldn't be loaded", e.getMessage());
			throw new MuseWebException("Class couldn't be loaded.");
		} catch (InstantiationException e) {
			LOG.warn("Class couldn't be instantiated", e.getMessage());
			throw new MuseWebException("Class couldn't be instantiated.");
		} catch (Exception e) {
			LOG.warn("Class couldn't be instantiated", e.getMessage());
			throw new MuseWebException("Class couldn't be accessed.");
		}
		LOG.warn("Recommender updated: " + updateId);
	}

	/**
	 * Receive a list of available recommenders from the Server. Invoked when
	 * client calls the corresponding path
	 * (/MuSe/API/recommendations/getRecommenders) and specifies JSON as accept
	 * type.
	 * 
	 * @return List of metrics represented as JSON array. A recommender is
	 *         represented as a combination of "name" and "description".
	 */
	@GET
	@Path("getRecommenders/filter/{filter}")
	@Produces("application/json")
	public String getRecommenders(@PathParam("filter") String filter) {
		Gson gson = new Gson();
		HashMap<Integer, JsonObject> recs = new HashMap<Integer, JsonObject>();
		HashMap<Integer, Recommender> recommenders;
		if (filter.equals("active")) {
			recommenders = RecommenderConfig.getActiveRecommenders();
		} else {
			recommenders = RecommenderConfig.getRecommenders();
		}

		// Serialize recommender information
		for (Integer id : recommenders.keySet()) {
			Recommender recommender = recommenders.get(id);
			JsonObject rec = new JsonObject();
			rec.add("NAME", new JsonPrimitive(recommender.getName()));
			rec.add("EXPLANATION",
					new JsonPrimitive(recommender.getExplanation()));
			rec.add("tagDistribution",
					gson.toJsonTree(recommender.getTagDistribution()));
			rec.add("ID", new JsonPrimitive(id));
			recs.put(id, rec);
		}
		return gson.toJson(recs);
	}

	/**
	 * Receive a statistic object containing several metric result of the MuSe
	 * service. Invoked when client calls the corresponding path
	 * (/MuSe/API/recommendations/getStatistics) and specifies JSON as accept
	 * type.
	 * 
	 * @return List of metrics represented as JSON array. A metric is
	 *         represented as a combination of "name" and "description".
	 */
	@GET
	@Path("getStatistics/from/{from}/to/{to}")
	@Produces("application/json")
	public String getStatistics(@PathParam("from") String from,
			@PathParam("to") String to) {
		Statistics stats = null;
		if (!from.equals("none") & !to.equals("none")) {
			stats = new Statistics(from, to);
		} else if (!from.equals("none")) {
			stats = new Statistics(from, null);
		} else if (!to.equals("none")) {
			stats = new Statistics(null, to);
		} else {
			stats = new Statistics();
		}
		stats.getAllMetrics();
		return new Gson().toJson(stats);
	}

	/**
	 * Get ratings of a recommendation list and save the ratings to the
	 * database. Invoked when client calls the corresponding path
	 * (/MuSe/API/recommendations/putRatins) and posts a rating array.
	 * 
	 * @param inFormParams
	 *            Map of recommendation IDs to ratings
	 */
	@PUT
	@Path("putRatings")
	@Consumes("application/x-www-form-urlencoded")
	public void putRatings(MultivaluedMap<String, String> inFormParams) {
		try {
			RecommenderData.putRatings(inFormParams);
		} catch (SQLException e) {
			LOG.warn("Couldn't save ratings.", e);
			throw new MuseWebException(
					"Saving ratings failed. Please try again.");
		}
	}

	/**
	 * Save user activities to the database.
	 * 
	 * @param user
	 *            Name of the user
	 * @param activities
	 *            Map of Recommendation IDs to Event types.
	 */
	@PUT
	@Path("putActivity")
	@Consumes("application/json")
	public void putActivity(String activity) {
		UserData.putActivity(new Gson().fromJson(activity, Activity.class));
	}

	/**
	 * Save user to the database (Registration).
	 * 
	 * @param user
	 *            User object in JSON notation.
	 * 
	 * @return String "success" if registration was successful, "name" if the
	 *         name is already taken or "fail" if some error occured.
	 */
	@PUT
	@Path("putUser")
	@Consumes("application/json")
	@Produces("text/plain")
	public String putUser(String userJSON) {
		LOG.info("Registering new user.");
		// Parse user object from JSON
		MuseUser user = new Gson().fromJson(userJSON, MuseUser.class);

		// Check for unique user name
		try {
			if (!UserData.checkUserName(user.getName())) {
				// Name is already taken
				return "name";
			}
		} catch (SQLException e) {
			LOG.warn(
					"Something went wrong while checking for duplicate usernames.",
					e);
			throw new MuseWebException("Checking user data failed.");
		}

		// Check for duplicate user names and lfm account
		boolean lfm;
		try {
			if (!user.getLfmaccount().equals("")
					&& !LastFmConnector.validateAccount(user.getLfmaccount())) {
				return "lfm";
			}
			lfm = UserData.checkDuplicateAccount(user.getLfmaccount());
		} catch (SQLException e) {
			LOG.warn("Couldn't check for duplicate last.fm account.", e);
			throw new MuseWebException("Checking user data failed.");
		}

		// Adapt options to Last.fm attribute
		Option opt;
		if (!user.getLfmaccount().equals("")) {
			ArrayList<Integer> ids = new ArrayList<Integer>(Arrays.asList(3, 4,
					5, 6, 7));
			opt = new Option("mixed", ids);
			user.setOptions(opt);
		} else {
			ArrayList<Integer> ids = new ArrayList<Integer>(Arrays.asList(3, 6,
					7));
			opt = new Option("mixed", ids);
			user.setOptions(opt);
		}

		// Save user to database
		try {
			// This is a transaction
			user.saveToDB();

			// Create first recommendation list
			ListComposer.createRecommendationList(user, opt.getBehavior(), 0,
					opt.getRecommenders());
		} catch (SQLException e) {
			LOG.warn("Couldn't save user " + user.getName() + " to databse.", e);
			throw new MuseWebException("Saving user to database failed.");
		}
		LOG.info("API: Registered new user: " + user.getName());

		// Saving successful => Fetch Lastfm data
		String lfmaccount = user.getLfmaccount();
		if (lfmaccount != null && !lfm) {
			// Start thread to fetch last.fm data
			NeighborCharts nCharts = new NeighborCharts();
			nCharts.setLfmUser(lfmaccount);
			new Thread(nCharts).start();

			TagCharts tCharts = new TagCharts();
			tCharts.setLfmUser(lfmaccount);
			new Thread(tCharts).start();
		}

		return "success";
	}

	/**
	 * Used to authenticate users.
	 * 
	 * @param credentials
	 *            JSON string representing typed in credentials
	 */
	@POST
	@Path("/checkLogin")
	@Consumes("application/json")
	@Produces("text/plain")
	public String checkLogin(String credentials) {
		Gson gson = new Gson();
		MuseUser user = gson.fromJson(credentials, MuseUser.class);
		String result;
		try {
			result = user.login();
		} catch (SQLException e) {
			LOG.warn("Couldn't check credentials for: " + user.getName(), e);
			throw new MuseWebException("Checking credentials failed.");
		}
		return result;
	}

	/**
	 * Delete user account fitting the given credentials
	 * 
	 * @param credentials
	 * @return
	 */
	@DELETE
	@Path("/deleteUser")
	@Consumes("application/json")
	@Produces("text/plain")
	public String deleteAccount(String credentials) {
		Gson gson = new Gson();
		MuseUser user = gson.fromJson(credentials, MuseUser.class);

		// Check credentials
		try {
			if (user.login().equals("none")) {
				return "credentials";
			}
		} catch (SQLException e) {
			LOG.warn("Couldn't check credentials for: " + user.getName(), e);
			throw new MuseWebException("Checking credentials failed.");
		}

		// Delete account
		try {
			user.delete();
			return "success";
		} catch (SQLException e) {
			LOG.warn("Couldn't delete user: " + user.getName(), e);
			throw new MuseWebException("Deleting user from database failed.");
		}
	}

	/**
	 * Change password of the given user.
	 * 
	 * @param credentials
	 * @return
	 */
	@POST
	@Path("/changeProfile")
	@Consumes("application/json")
	@Produces("text/plain")
	public String changeProfile(String credentials) {
		Gson gson = new Gson();
		MuseUser user = gson.fromJson(credentials, MuseUser.class);

		// Check credentials
		try {
			if (user.login().equals("none")) {
				return "password";
			}
		} catch (SQLException e) {
			LOG.warn("Couldn't check credentials for: " + user.getName(), e);
			throw new MuseWebException("Checking credentials failed.");
		}

		// Login is populating object with given data we don't want to have in
		// the change object. Therefore parse received user data again to the
		// object.
		user = gson.fromJson(credentials, MuseUser.class);
		try {
			user.changeProfile();
			return "success";
		} catch (SQLException e) {
			LOG.warn("Couldn't change profile for user: " + user.getName());
			throw new MuseWebException("Updating database fields failed.");
		}
	}

	/**
	 * Initiate forgot password process for the given user.
	 * 
	 * @param credentials
	 * @return
	 */
	@POST
	@Path("/forgotPassword")
	@Consumes("application/json")
	public String forgotPassword(String data) {
		Gson gson = new Gson();
		MuseUser user = gson.fromJson(data, MuseUser.class);
		try {
			return user.forgotPassword();
		} catch (AddressException e) {
			LOG.warn("Sending forgot password mail failed.", e);
			throw new MuseWebException("Not a valid address.");
		} catch (MessagingException e) {
			LOG.warn("Sending forgot password mail failed.", e);
			throw new MuseWebException("Sending mail failed.");
		} catch (SQLException e) {
			LOG.warn("Failed to save password recovery request.", e);
			throw new MuseWebException("Database not reachable.");
		}
	}

	/**
	 * Recover password process for the given user.
	 * 
	 * @param credentials
	 * @return
	 */
	@POST
	@Path("/recoverPassword")
	@Consumes("application/json")
	@Produces("application/json")
	public String recoverPassword(String data) {
		Gson gson = new Gson();
		MuseUser user = gson.fromJson(data, MuseUser.class);

		try {
			return gson.toJson(user.recoverPassword());
		} catch (SQLException e) {
			LOG.warn("Couldn't recover password for user: " + user.getName(), e);
			throw new MuseWebException("Updating database failed.");
		}
	}

	/**
	 * Get the content of the log file.
	 * 
	 * @throws IOException
	 */
	@GET
	@Path("getLog")
	@Produces("application/json")
	public String getLog() throws IOException {
		String log;
		BufferedReader br = new BufferedReader(new FileReader(JAR_PATH
				+ "/log/muse.log"));
		try {
			StringBuilder logfile = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				logfile.append(line);
				logfile.append("<br>");
				line = br.readLine();
			}
			log = logfile.toString();
		} finally {
			br.close();
		}
		return new Gson().toJson(log);
	}

	/**
	 * Create an Evaluation phase.
	 * 
	 * @param data
	 *            Parsed form data from the front end that specifies the
	 *            Evaluation.
	 */
	@PUT
	@Path("createEvaluation")
	@Consumes("application/json")
	public void createEvaluation(String data) {
		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
		Evaluation eval = gson.fromJson(data, Evaluation.class);
		eval.saveToDatabase();
	}

	/**
	 * Create an evaluation summary containing current users matched to groups.
	 * 
	 * @param data
	 *            Parsed form data from the front end that specifies the
	 *            Evaluation.
	 */
	@POST
	@Path("getEvaluationSummary")
	@Consumes("application/json")
	@Produces("application/json")
	public String getEvaluationSummary(String data) {
		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
		Evaluation eval = gson.fromJson(data, Evaluation.class);
		List<String> users = UserData.getAllUsers();
		Map<String, Integer> userNameGroups = eval.matchUserToGroup(users);

		// Replace user names with user objects including necessary information.
		HashMap<String, Integer> userGroups = new HashMap<String, Integer>();

		// Connect to database
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.prepareStatement("SELECT birthyear, sex, language "
					+ "FROM consumer, consumer_language "
					+ "WHERE name = consumer_name AND name = ?");

			for (String userName : userNameGroups.keySet()) {
				stmt.setString(1, userName);
				result = stmt.executeQuery();
				int i = 0;
				List<String> langs = new ArrayList<String>();
				int birthyear = 0;
				String gender = "";
				while (result.next()) {
					if (i == 0) {
						birthyear = result.getInt("birthyear");
						gender = result.getString("sex");
					}
					langs.add(result.getString("language"));
				}
				MuseUser user = new MuseUser(userName, birthyear, gender, langs);
				userGroups.put(gson.toJson(user), userNameGroups.get(userName));
			}
		} catch (SQLException e) {
			LOG.warn("Selecting user information from DB failed.", e);
			throw new MuseWebException("Database not reachable.");
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return gson.toJson(userGroups);
	}

	/**
	 * Get information about the evaluation with the given id.
	 * 
	 */
	@GET
	@Path("getEvaluation/id/{id}")
	@Produces("application/json")
	public String getEvaluation(@PathParam("id") int id) {
		try {
			Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
			return gson.toJson(EvaluationData.getById(id));
		} catch (SQLException e) {
			LOG.warn("Couldn't get evaluation with id " + id, e);
			throw new MuseWebException("Database not reachable.");
		}
	}

	/**
	 * Get the history of evaluations.
	 * 
	 */
	@GET
	@Path("getEvaluationHistory")
	@Produces("application/json")
	public String getEvaluationHistory() {
		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
		try {
			return gson.toJson(EvaluationData.getHistory());
		} catch (SQLException e) {
			throw new MuseWebException("Database not reachable.");
		}
	}

	/**
	 * Process the answer of an evaluation invite.
	 */
	@POST
	@Path("processEvaluationInvite/user/{name}")
	@Consumes("application/json")
	@Produces("application/json")
	public String processEvaluationInvite(@PathParam("name") String name,
			String accepted) {
		Gson gson = new Gson();

		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = Database.getConnection();
			// Start transaction
			conn.setAutoCommit(false);

			// Check if there is a running Evaluation
			if (!EvaluationData.isRunning())
				return gson.toJson("eval");

			if (accepted.equals("yes")) {

				// Get the information about the running evaluation
				Evaluation currentEval = EvaluationData.getRunning();
				if (currentEval == null)
					return gson.toJson("eval");

				// Match user to group
				int matchedGroup = currentEval.matchUserToGroup(name);
				// Check if user was successfully matched
				if (matchedGroup == -1)
					return gson.toJson("match");

				// Add user to matched group
				stmt = conn
						.prepareStatement("INSERT INTO evaluation_participants VALUES(?,?,?, CURRENT_TIMESTAMP, NULL)");
				stmt.setInt(1, currentEval.getId());
				stmt.setInt(2, matchedGroup);
				stmt.setString(3, name);
				stmt.execute();

				// Delete precomputations for this user
				stmt = conn
						.prepareStatement("DELETE FROM user_track_score WHERE user_name = ?");
				stmt.setString(1, name);
				stmt.executeUpdate();
				stmt = conn
						.prepareStatement("DELETE FROM user_user_score WHERE user_name = ?");
				stmt.setString(1, name);
				stmt.executeUpdate();

				// Create NEWCOMER recommendations list with preliminary
				// NEWCOMER
				// settings.
				ArrayList<Integer> recs = new ArrayList<Integer>(
						Arrays.asList(3));
				UserData.saveOptions("mixed", recs, name);
				MuseUser user = new MuseUser(name);
				ListComposer.createRecommendationList(user, "mixed",
						currentEval.getId(), recs);
			}

			// Set participant flag
			String accept = (accepted.equals("yes")) ? "Y" : "N";
			String newcomer = (accept.equals("Y")) ? ", newcomer = 'Y'" : "";
			String query = "UPDATE consumer SET eval_participant = ? "
					+ newcomer + " WHERE name = ?";

			// Add participant
			stmt = conn.prepareStatement(query);
			stmt.setString(1, accept);
			stmt.setString(2, name);
			stmt.executeUpdate();

			// Transaction succeeded. Commit it.
			conn.commit();
			LOG.info("Added participant " + name + " to evaluation.");
		} catch (SQLException e) {
			LOG.warn("- Rollback - Processing invitation failed.", e);
			Database.quietRollback(conn);
			throw new MuseWebException("Database not reachable.");
		} finally {
			Database.resetAutoCommit(conn);
			Database.quietClose(stmt);
			Database.quietClose(conn);
		}

		return gson.toJson("success");

	}

	/**
	 * Remove participant from the running evaluation.
	 */
	@POST
	@Path("quitEvaluation")
	@Consumes("application/json")
	public void quitEvaluation(String name) {
		Evaluation eval;
		try {
			eval = EvaluationData.getRunning();
			eval.quit(name);
			LOG.info("Removed participant " + name + " from evaluation "
					+ eval.getId());
		} catch (SQLException e) {
			LOG.warn("Couldn't remove user " + name + " from evaluation.", e);
			throw new MuseWebException("Database not reachable.");
		}
	}

	/**
	 * Delete the evaluation with the given ID.
	 */
	@POST
	@Path("deleteEvaluation")
	@Consumes("application/json")
	public void deleteEvaluation(String data) {
		Gson gson = new Gson();
		int id = gson.fromJson(data, int.class);
		try {
			EvaluationData.deleteById(id);
		} catch (SQLException e) {
			LOG.warn(
					"Couldn't delete evaluation with id " + String.valueOf(id),
					e);
			throw new MuseWebException("Database error.");
		}
	}

	/**
	 * Change settings of the evaluation with the given ID to the given
	 * settings.
	 */
	@POST
	@Path("changeEvaluation")
	@Consumes("application/json")
	public void changeEvaluation(String data) {
		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
		Evaluation eval = gson.fromJson(data, Evaluation.class);
		try {
			eval.adaptSettings();
		} catch (SQLException e) {
			LOG.warn("Couldn't change evaluation settings.", e);
			throw new MuseWebException("Changing evaluation settings failed.");
		}
	}

	/**
	 * Change settings of the evaluation group with the given evaluation ID and
	 * group ID to the given settings.
	 */
	@POST
	@Path("changeGroup")
	@Consumes("application/json")
	public void changeEvaluationGroup(String data) {
		Gson gson = new Gson();
		Group group = gson.fromJson(data, Group.class);
		try {
			EvaluationData.changeGroupSettings(group);
		} catch (SQLException e) {
			LOG.warn("Couldn't change evaluation group settings.", e);
			throw new MuseWebException(
					"Changing evaluation group settings failed.");
		}
	}

	/**
	 * Add a group with the given settings to the evaluation with the given ID.
	 */
	@POST
	@Path("addGroup")
	@Consumes("application/json")
	public void addEvaluationGroup(String data) {
		Gson gson = new Gson();
		Group group = gson.fromJson(data, Group.class);
		try {
			EvaluationData.addGroup(group);
		} catch (SQLException e) {
			LOG.warn("Couldn't addd evaluation group.", e);
			throw new MuseWebException("Adding evaluation group failed.");
		}
	}

	/**
	 * Delete the group with the given ID from the evaluation with the given ID.
	 * ID, ID given in post JSON object.
	 */
	@POST
	@Path("deleteGroup")
	@Consumes("application/json")
	public void deleteEvaluationGroup(String data) {
		System.out.println(data);
		Gson gson = new Gson();
		String[] groupMeta = gson.fromJson(data, String[].class);
		try {
			EvaluationData.deleteGroup(Integer.parseInt(groupMeta[0]),
					Integer.parseInt(groupMeta[1]));
		} catch (SQLException e) {
			LOG.warn("Couldn't delete evaluation group.", e);
			throw new MuseWebException("Deleting evaluation group failed.");
		}
	}

	/**
	 * Delete the participant with the given NAME from the evaluation with the
	 * given ID. ID, NAME given in post JSON object.
	 */
	@POST
	@Path("deleteParticipant")
	@Consumes("application/json")
	public void deleteParticipant(String data) {
		System.out.println(data);
		Gson gson = new Gson();
		String[] participantInfo = gson.fromJson(data, String[].class);
		try {
			EvaluationData.deleteParticipant(
					Integer.parseInt(participantInfo[0]), participantInfo[1]);
			LOG.info("Removed participant " + participantInfo[1]
					+ " from evaluation #" + participantInfo[0] + ".");
		} catch (SQLException e) {
			LOG.warn("Couldn't delete participant.", e);
			throw new MuseWebException("Deleting participant failed.");
		}
	}

	/**
	 * Move the participant with the given NAME to the given GROUP. ID, GROUP
	 * given in post JSON object.
	 */
	@POST
	@Path("moveParticipant")
	@Consumes("application/json")
	public void moveParticipant(String data) {
		System.out.println(data);
		Gson gson = new Gson();
		String[] participantInfo = gson.fromJson(data, String[].class);
		try {
			EvaluationData.moveParticipant(
					Integer.parseInt(participantInfo[0]),
					Integer.parseInt(participantInfo[1]), participantInfo[2]);
			LOG.info("Removed participant " + participantInfo[2]
					+ " to group #" + participantInfo[1] + " in evaluation #"
					+ participantInfo[0] + ".");
		} catch (SQLException e) {
			LOG.warn("Couldn't move participant.", e);
			throw new MuseWebException("Moving participant failed.");
		}
	}

	/**
	 * Get evaluation results by evaluation id.
	 */
	@POST
	@Path("getEvaluationResults/source={source}")
	@Consumes("application/json")
	public String getEvaluationResultsById(String data,
			@PathParam("source") String source) {
		Gson gson = new Gson();
		int id = gson.fromJson(data, int.class);
		EvaluationResults results = new EvaluationResults(id);
		try {
			if (source.equals("recommender")) {
				results.getMeanAbsoluteErrors();
				results.getGroupListRatings();
				results.getGroupAccuarcy();
				results.getAvgGroupRatings();
				results.getAvgRatingsPerRecommender();
			} else if (source.equals("user")) {
				results.getGroupJoins();
				results.getGroupVisits();
				results.getGroupRatings();
				results.getAgeDist();
				results.getGenderDist();
			}
		} catch (SQLException e) {
			LOG.warn("Computing evaluation results failed.", e);
			throw new MuseWebException("Computing evaluation results failed.");
		}
		return gson.toJson(results);
	}

	/**
	 * Get evaluation data as CSV file.
	 */
	@GET
	@Path("getEvaluationData/eval={eval}&file={data}")
	@Produces("text/plain")
	public Response getEvaluationDataCsv(@PathParam("data") String data,
			@PathParam("eval") int evalId) {
		String filePath = ApplicationConfig.PERM_DIR + "file.csv";
		EvaluationDataProvider dataProvider = new EvaluationDataProvider(
				evalId, filePath);
		try {
			// Create rating data file
			if (data.equals("rating")) {
				dataProvider.createRatingDataFile();
			} else if (data.equals("mae")) {
				dataProvider.createMAEDataFile();
			} else if (data.equals("user_info")) {
				dataProvider.createUserDataFile();
			} else if (data.equals("rating_time")) {
				dataProvider.createRatingTimeDataFile();
			} else if (data.equals("list_rating")) {
				dataProvider.createListRatingDataFile();
			} else if (data.equals("group_list_rating")) {
				dataProvider.createAvgListRatingDataFile();
			} else if (data.equals("accuracy")) {
				dataProvider.createAccuracyDataFile();
			} else if (data.equals("recommender_info")) {
				dataProvider.createRecommenderMapDataFile();
			} else if (data.equals("avg_group_rating")) {
				dataProvider.createAvgGroupRatingDataFile();
			} else if (data.equals("avg_recommender_rating")) {
				dataProvider.createAvgRecommenderRatingDataFile();
			} else if (data.equals("age_distribution")) {
				dataProvider.createAgeDistDataFile();
			} else if (data.equals("gender_distribution")) {
				dataProvider.createGenderDistDataFile();
			}
		} catch (SQLException e) {
			LOG.warn("Creating evaluation data file failed.");
			throw new WebApplicationException();
		} catch (IOException e) {
			LOG.warn("Creating evaluation data file failed.");
			throw new WebApplicationException();
		}
		File file = new File(filePath);
		ResponseBuilder response = Response.ok((Object) file);
		response.header("Content-Disposition", "attachment; filename=" + data
				+ ".csv");
		return response.build();
	}
}
