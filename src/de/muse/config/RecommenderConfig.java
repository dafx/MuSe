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
package de.muse.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.muse.api.DataRepository;
import de.muse.api.Recommender;
import de.muse.recommendation.MuseRepository;
import de.muse.utility.Database;
import de.muse.web.MuseWebException;

/**
 * Maintain recommender configuration in the system.
 */
public class RecommenderConfig {
	// Configured logger
	private static final Logger LOG = LoggerFactory
			.getLogger(RecommenderConfig.class.getName());

	// Meta information
	private static final String CONFIG_DIR = ApplicationConfig.PERM_DIR;
	private static final String CONFIG_FILE = "recommenders.json";
	private static final Gson SERIALIZATION = new Gson();
	private static final Type CONFIG_COLLECTION_TYPE = new TypeToken<List<Config>>() {
	}.getType();

	// Concurrent configuration cache
	private static Map<Integer, Config> configuration = new ConcurrentHashMap<Integer, Config>();
	private static Map<Integer, Recommender> recommenders = new ConcurrentHashMap<Integer, Recommender>();

	// Easy deep clone by serialize and deserialize
	public static String getConfigString() {
		Gson gson = new Gson();
		return gson.toJson(configuration);
	}

	/**
	 * Get a list of all available recommenders represented by a new object of
	 * the class.
	 * 
	 * @return Map of Recommender ID -> Recommender object
	 */
	public static HashMap<Integer, Recommender> getRecommenders() {
		return new HashMap<Integer, Recommender>(recommenders);
	}

	/**
	 * Get a list of all available and active recommenders represented by a new
	 * object of the class. Recommenders are active if their status = 1.
	 * 
	 * @return Map of Recommender ID -> recommender object
	 */
	public static HashMap<Integer, Recommender> getActiveRecommenders() {
		HashMap<Integer, Recommender> recommendersTmp = new HashMap<Integer, Recommender>();
		// Turn into Map recID -> recObj
		for (Integer id : configuration.keySet()) {
			Config config = configuration.get(id);
			if (config.status == 1) {
				// Put (id, recObj) to map
				recommendersTmp.put(config.id, recommenders.get(config.id));
			}
		}
		return recommendersTmp;
	}

	/**
	 * Update the recommender status.
	 * 
	 * @param recommenderID
	 *            The id of the recommender to update.
	 * @param status
	 *            The status to assign. 1 = active, 0 = inactive
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public static void updateStatus(int recommenderID, int status)
			throws IOException, URISyntaxException {
		// Update config
		configuration.get(recommenderID).status = status;
	}

	/**
	 * Update the recommender status in batch.
	 * 
	 * @param statusMapping
	 *            recommenderID -> status
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public static void updateStatus(MultivaluedMap<String, String> statusMapping)
			throws IOException, URISyntaxException {
		// Update config
		for (Integer id : configuration.keySet()) {
			int status = Integer.valueOf(statusMapping.get(String.valueOf(id))
					.get(0));
			configuration.get(id).status = status;
		}
	}

	public static Recommender addRecommender(String filePath, String classname,
			int id) throws ClassNotFoundException, IOException,
			URISyntaxException, SecurityException, NoSuchMethodException,
			IllegalArgumentException, InstantiationException,
			IllegalAccessException, InvocationTargetException, SQLException {

		// Load jar
		File file = new File(filePath);
		URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { file
				.toURI().toURL() }, RecommenderConfig.class.getClassLoader());
		Constructor<?> recommenderClass = urlClassLoader.loadClass(classname)
				.getConstructor(Integer.TYPE, DataRepository.class);

		// Add to recommenders cache
		Recommender rec = (Recommender) recommenderClass.newInstance(id,
				new MuseRepository());
		recommenders.put(id, rec);

		// Add to configuration cache
		configuration.put(id, new Config(id, 0, classname, filePath));

		// Return object (just out of convenience)
		return rec;
	}

	public static void removeRecommender(int id) {
		// Remove from object map
		recommenders.remove(id);

		// Remove from configuration
		Config conf = configuration.remove(id);

		// Delete jar file
		File file = new File(conf.filepath);
		file.delete();
	}

	/**
	 * Check if a recommender was already evaluated.
	 * 
	 * @param recommenderId
	 *            The recommender id to check for
	 * @return List of evaluation names it was used in
	 */
	public static List<String> usedInEvaluations(int recommenderId) {
		List<String> evals = new ArrayList<String>();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet result = null;
		Gson gson = new Gson();

		try {
			conn = Database.getConnection();

			// Get evaluation recommender settings
			stmt = conn
					.prepareStatement("SELECT name, settings_recommenders FROM Evaluation JOIN Evaluation_Groups ON id= eval_id");
			result = stmt.executeQuery();

			// Check if the recommender is used in any of the evaluations
			while (result.next()) {
				String name = result.getString("name");
				String settings = result.getString("settings_recommenders");
				int[] ids = gson.fromJson(settings, int[].class);
				// Search for the given recommender id
				for (int i = 0; i < ids.length; i++) {
					if (ids[i] == recommenderId && !evals.contains(name)) {
						evals.add(name);
					}
				}
			}

		} catch (SQLException e) {
			throw new MuseWebException("Database not reachable.");
		} finally {
			Database.quietClose(result);
			Database.quietClose(stmt);
			Database.quietClose(conn);
		}

		return evals;
	}

	/**
	 * Initialize recommender configuration.
	 * 
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws ClassNotFoundException
	 * @throws IllegalArgumentException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws MalformedURLException
	 */
	public static void initializeConfig() throws SecurityException,
			NoSuchMethodException, ClassNotFoundException,
			IllegalArgumentException, InstantiationException,
			IllegalAccessException, InvocationTargetException,
			MalformedURLException {

		// Read config and intialize recommenders
		List<Config> config = readConfig();

		// Load jars
		List<URL> jarPaths = new ArrayList<URL>();
		for (Config conf : config) {
			if (conf.filepath == null)
				continue;

			File file = new File(conf.filepath);
			jarPaths.add(file.toURI().toURL());
		}
		URL[] urls = jarPaths.toArray(new URL[] {});
		URLClassLoader cloader = new URLClassLoader(urls,
				RecommenderConfig.class.getClassLoader());

		// Load recommenders
		for (Config conf : config) {
			// Cache configuration
			configuration.put(conf.id, conf);

			// Load recommenders
			Constructor<?> recommenderClass;
			if (conf.filepath != null) {
				// Load class via URL
				recommenderClass = cloader.loadClass(conf.classpath)
						.getConstructor(Integer.TYPE, DataRepository.class);
			} else {
				// Load class internally
				recommenderClass = Class.forName(conf.classpath)
						.getConstructor(Integer.TYPE, DataRepository.class);
			}

			// Cache recommenders
			recommenders.put(conf.id, (Recommender) recommenderClass
					.newInstance(conf.id, new MuseRepository()));
		}
	}

	private static List<Config> readConfig() {
		List<Config> config = new ArrayList<Config>();
		// Deserialize config file
		try {
			BufferedReader br = new BufferedReader(new FileReader(CONFIG_DIR
					+ CONFIG_FILE));
			config = SERIALIZATION.fromJson(br, CONFIG_COLLECTION_TYPE);
		} catch (FileNotFoundException e) {
			LOG.warn("Config file not found at: " + CONFIG_FILE);
		}
		return config;
	}

	public static void writeBackConfig() throws IOException, URISyntaxException {
		// Serialize config file
		File file = new File(CONFIG_DIR + CONFIG_FILE);
		FileWriter fw = null;
		try {
			fw = new FileWriter(file);
			fw.write(SERIALIZATION.toJson(new ArrayList<Config>(configuration
					.values())));
		} finally {
			try {
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static class Config {
		private int id;
		private int status;
		private String classpath;
		private String filepath;

		// Additional
		private String name;

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public int getID() {
			return id;
		}

		public Config(int id, int status, String classpath, String filepath) {
			this.id = id;
			this.status = status;
			this.classpath = classpath;
			this.filepath = filepath;
		}
	}
}
