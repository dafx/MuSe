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
package de.muse.user;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.muse.evaluation.EvaluationData;
import de.muse.utility.Database;
import de.muse.utility.Encryption;
import de.muse.utility.Mail;

/**
 * Represents a user and its information. Takes care of getting and saving these
 * informations.
 */
public class MuseUser implements de.muse.api.User {
	// Configured logger
	private static final Logger LOG = LoggerFactory.getLogger(MuseUser.class
			.getName());

	// User information
	private String name;
	private String password;
	private String email;
	private int birthyear;
	private String sex;
	private String lfmaccount;
	private List<String> langs;
	@SuppressWarnings("unused")
	private boolean newcomer;
	@SuppressWarnings("unused")
	private int newcomerRatings;
	private String evalParticipant;
	@SuppressWarnings("unused")
	private Date evalDuration;
	private transient Option options;

	/**
	 * Create an empty user object. <br>
	 * </br>
	 * 
	 * <b>Caution:</b> Don't use this. It is only used for serialization, where
	 * the object implicitly uses the empty constructor.
	 * 
	 */
	public MuseUser() {
		this("", "", 0, "", "", "", false);
	}

	/**
	 * Construct a user with only the name
	 * 
	 * @param name
	 *            Name of the user.
	 * 
	 */
	public MuseUser(String name) {
		this(name, "", 0, "", "", "", false);
	}

	/**
	 * Construct a user with only the name and password
	 * 
	 * @param name
	 *            Name of the user.
	 * 
	 * @param password
	 *            Password of the user
	 * 
	 */
	public MuseUser(String name, String password) {
		this(name, password, 0, "", "", "", false);
	}

	/**
	 * Construct a user with name, age, gender and language information.
	 * 
	 * @param name
	 *            Name of the user.
	 * @param birthyear
	 *            The user's Year of birth.
	 * @param sex
	 *            The gender of the user.
	 * @param langs
	 *            List of languages of the user.
	 * 
	 **/
	public MuseUser(String name, int birthyear, String sex, List<String> langs) {
		this.name = name.toLowerCase();
		this.birthyear = birthyear;
		this.sex = sex;
		this.langs = langs;
	}

	/**
	 * Construct a user with name, age, gender and language information.
	 * 
	 * @param name
	 *            Name of the user.
	 * @param birthyear
	 *            The user's Year of birth.
	 * @param sex
	 *            The gender of the user.
	 * @param lfmaccount
	 *            Lastfm account of the user.
	 * 
	 **/
	public MuseUser(String name, int birthyear, String sex, String lfmaccount) {
		this.name = name.toLowerCase();
		this.birthyear = birthyear;
		this.sex = sex;
		this.lfmaccount = lfmaccount;
	}

	/**
	 * Construct a user with name, age, gender and language information.
	 * 
	 * @param name
	 *            Name of the user.
	 * @param birthyear
	 *            The user's Year of birth.
	 * @param sex
	 *            The gender of the user.
	 * @param langs
	 *            List of languages of the user.
	 * @param lfmaccount
	 *            Lastfm account of the user.
	 * 
	 **/
	public MuseUser(String name, int birthyear, String sex, List<String> langs,
			String lfmaccount) {
		this.name = name.toLowerCase();
		this.birthyear = birthyear;
		this.sex = sex;
		this.langs = langs;
		this.lfmaccount = lfmaccount;
	}

	/**
	 * Construct a user with complete information
	 * 
	 * @param name
	 *            Name of the user.
	 * @param password
	 *            Password of the user.
	 * @param birthyear
	 *            The user's Year of birth.
	 * @param sex
	 *            The sex of the user.
	 * @param lfmaccount
	 *            The Last.fm Account of the user.
	 * @param newcomer
	 *            Marks the user as new.
	 * 
	 **/
	public MuseUser(String name, String password, int birthyear, String sex,
			String lfmaccount, String email, boolean newcomer) {
		this.name = name.toLowerCase();
		this.password = password;
		this.birthyear = birthyear;
		this.sex = sex;
		this.lfmaccount = lfmaccount;
		this.email = email;
		this.newcomer = newcomer;
	}

	/**
	 * Get the name of the user.
	 * 
	 * @return The name of the user as String.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the options of the user object.
	 */
	public void setOptions(Option options) {
		this.options = options;
	}

	/**
	 * Set the newcomer ratings value.
	 */
	public void setNewcomerRatings(int newcomerRatigns) {
		this.newcomerRatings = newcomerRatigns;
	}

	/**
	 * Get the email address of the user
	 * 
	 * @return The email address of the user as String.
	 */
	public String getEmail() {
		if (email == null || email.isEmpty()) {
			// Query from database
			Connection conn = null;
			PreparedStatement stmt = null;
			ResultSet result = null;

			try {
				conn = Database.getConnection();
				stmt = conn
						.prepareStatement("SELECT email FROM consumer WHERE name="
								+ "'" + name.toLowerCase() + "'");
				result = stmt.executeQuery();

				while (result.next()) {
					email = result.getString("email");
				}
			} catch (SQLException e) {
				LOG.warn("Couldn't get email address for user: " + name, e);
			} finally {
				Database.quietClose(conn, stmt, result);
			}
		}
		return email;
	}

	/**
	 * Get the gender of the user
	 * 
	 * @return The gender of the user as String. One of {"male", "female"}.
	 */
	public String getSex() {
		if (sex == null || sex.isEmpty()) {
			// Query from database
			Connection conn = null;
			PreparedStatement stmt = null;
			ResultSet result = null;

			try {
				conn = Database.getConnection();
				stmt = conn
						.prepareStatement("SELECT sex FROM consumer WHERE name="
								+ "'" + name.toLowerCase() + "'");
				result = stmt.executeQuery();

				while (result.next()) {
					sex = result.getString("sex");
				}
			} catch (SQLException e) {
				LOG.warn("Couldn't get gender for user: " + name, e);
			} finally {
				Database.quietClose(conn, stmt, result);
			}
		}
		return sex;
	}

	/**
	 * Get the languages of the user
	 * 
	 * @return The languages of the user as List<String>.
	 */
	public List<String> getLangs() {
		List<String> languages = new ArrayList<String>();
		if (langs == null) {
			// Query from database
			Connection conn = null;
			PreparedStatement stmt = null;
			ResultSet result = null;

			try {
				conn = Database.getConnection();
				stmt = conn
						.prepareStatement("SELECT language FROM consumer_language "
								+ "WHERE consumer_name="
								+ "'"
								+ name.toLowerCase() + "'");
				result = stmt.executeQuery();

				while (result.next()) {
					languages.add(result.getString("language"));
				}
				langs = languages;
			} catch (SQLException e) {
				LOG.warn("Couldn't get langs for user: " + name, e);
			} finally {
				Database.quietClose(conn, stmt, result);
			}
		}
		return langs;
	}

	/**
	 * Get the user's year of birth.
	 * 
	 * @return Year of birth as Integer value.
	 */
	public int getBirthyear() {
		if (birthyear == 0) {
			// Query from database
			Connection conn = null;
			PreparedStatement stmt = null;
			ResultSet result = null;
			try {
				conn = Database.getConnection();
				stmt = conn
						.prepareStatement("SELECT birthyear FROM consumer WHERE name="
								+ "'" + name + "'");
				result = stmt.executeQuery();

				while (result.next()) {
					birthyear = result.getInt("birthyear");
				}

			} catch (SQLException e) {
				LOG.warn("Couldn't get birthyear for user: " + name, e);
			} finally {
				Database.quietClose(conn, stmt, result);
			}
		}
		return birthyear;
	}

	/**
	 * Get the user's Last.fm Account.
	 * 
	 * @return Last.fm Account name as String.
	 */
	public String getLfmaccount() {
		if (lfmaccount == null || lfmaccount.isEmpty()) {
			// Query from database
			Connection conn = null;
			PreparedStatement stmt = null;
			ResultSet result = null;

			try {
				conn = Database.getConnection();
				stmt = conn
						.prepareStatement("SELECT lfmaccount FROM consumer WHERE name="
								+ "'" + name + "'");
				result = stmt.executeQuery();

				while (result.next()) {
					lfmaccount = result.getString("lfmaccount");
				}
			} catch (SQLException e) {
				LOG.warn("Couldn't get Last.fm Account for user: " + name, e);
			} finally {
				Database.quietClose(conn, stmt, result);
			}
		}
		return lfmaccount;
	}

	/**
	 * Check login credentials of the user.
	 * 
	 * @return Returns the serialized user object including all information of
	 *         the logged in user.
	 */
	public String login() throws SQLException {
		String user = "none";

		// Connect to databse
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet users = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("SELECT * FROM consumer WHERE name=?");
			pstmt.setString(1, name.toLowerCase());
			users = pstmt.executeQuery();

			// No matching user found, exit
			if (!users.next()) {
				return user;
			}
			// Continue if there is a result and the password matches
			if (users.getString("password") != null
					&& password != null
					&& Encryption.BCrypt.checkpw(password,
							users.getString("password"))) {
				birthyear = users.getInt("birthyear");
				if (users.getString("lfmaccount") != null) {
					lfmaccount = users.getString("lfmaccount");
				}
				email = users.getString("email");
				sex = users.getString("sex");
				// Use password to transport role information back to client
				// (Hack)
				String role = users.getString("role");
				password = role;

				// Set newcomer field
				String newcomerStatus = users.getString("newcomer");
				if (newcomerStatus.equals("Y")) {
					newcomer = true;
					newcomerRatings = countRatings();
				} else {
					newcomer = false;
				}

				// Check if user participates in a currently running evaluation
				if (EvaluationData.isRunning()) {
					evalParticipant = users.getString("eval_participant");
					if (evalParticipant == null) {
						evalParticipant = "U";
					} else {
						evalDuration = EvaluationData.getRunning().getTo();
					}
				} else {
					evalParticipant = "none";
				}

				Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd")
						.create();
				user = gson.toJson(this);
				LOG.info(name + " logged in as " + role);
			}

			// Save login activity to DB
			if (!user.equals("none")) {
				pstmt = conn
						.prepareStatement("INSERT INTO login_activities VALUES(?, CURRENT_DATE, CURRENT_TIMESTAMP)");
				pstmt.setString(1, name.toLowerCase());
				pstmt.execute();
			}

		} finally {
			Database.quietClose(conn, pstmt, users);
		}
		return user;
	}

	/**
	 * Delete user from the database.
	 * 
	 * @return boolean True if user deleted successfully, else false.
	 */
	public void delete() throws SQLException {
		// Connect to databse
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("UPDATE consumer SET email = NULL, password = NULL, lfmaccount = NULL WHERE name = ?");
			pstmt.setString(1, name.toLowerCase());
			pstmt.execute();
			LOG.info("Deleted user with name: " + name);
		} finally {
			Database.quietClose(conn);
			Database.quietClose(pstmt);
		}
	}

	/**
	 * Get countries where the user's languages are spoken.
	 * 
	 * @return List of country names
	 */
	public List<String> getCountries() {
		List<String> regions = new ArrayList<String>();

		if (langs == null || langs.isEmpty()) {
			getLangs();
		}

		// Query database
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();

			// Get the countries where the languages are spoken
			pstmt = conn.prepareStatement("SELECT DISTINCT country "
					+ "FROM language_country WHERE language =?");

			for (String lang : langs) {
				pstmt.setString(1, lang);
				result = pstmt.executeQuery();
				while (result.next()) {
					regions.add(result.getString("country"));
				}
			}
		} catch (SQLException e) {
			LOG.warn("Couldn't get countries of user: " + name, e);
		} finally {
			Database.quietClose(conn, pstmt, result);
		}
		return regions;
	}

	/**
	 * Get cities where the user's languages are spoken.
	 * 
	 * @return List of city names
	 */
	public List<String> getCities() {
		List<String> cities = new ArrayList<String>();
		List<String> countries = getCountries();

		// Query database
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();

			// Get the countries where the languages are spoken
			pstmt = conn.prepareStatement("SELECT DISTINCT chartscity "
					+ "FROM charts_city WHERE chartscountry =?");

			for (String country : countries) {
				pstmt.setString(1, country);
				result = pstmt.executeQuery();
				while (result.next()) {
					cities.add(result.getString("chartscity"));
				}
			}
		} catch (SQLException e) {
			LOG.warn("Couldn't get cities of user: " + name, e);
		} finally {
			Database.quietClose(conn, pstmt, result);
		}
		return cities;
	}

	/**
	 * Save the user to the Database. Puts the information of the given user to
	 * the database (= Register).
	 */
	public void saveToDB() throws SQLException {
		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			String passwordEncrypted = Encryption.BCrypt.hashpw(password,
					Encryption.BCrypt.gensalt());
			LOG.info("Password encryption finished. " + passwordEncrypted);

			conn = Database.getConnection();
			// Start transaction
			conn.setAutoCommit(false);

			pstmt = conn
					.prepareStatement("INSERT INTO consumer VALUES(?,?,?,?,?,0, 'user', ?, CURRENT_TIMESTAMP, 'Y', NULL)");
			pstmt.setString(1, name.toLowerCase());
			pstmt.setString(2, passwordEncrypted);
			pstmt.setInt(3, birthyear);
			pstmt.setString(4, sex);
			pstmt.setString(5, lfmaccount);
			pstmt.setString(6, email);
			pstmt.executeUpdate();

			pstmt = conn
					.prepareStatement("INSERT INTO consumer_language VALUES(?,?)");
			for (String lang : langs) {
				pstmt.setString(1, name.toLowerCase());
				pstmt.setString(2, lang);
				pstmt.executeUpdate();
			}

			pstmt = conn
					.prepareStatement("INSERT INTO consumer_options VALUES(?,?,?)");
			pstmt.setString(1, name.toLowerCase());
			pstmt.setString(2, options.getBehavior());
			pstmt.setString(3, new Gson().toJson(options.getRecommenders()));
			pstmt.executeUpdate();

			pstmt = conn
					.prepareStatement("INSERT INTO option_activities VALUES(?, CURRENT_TIMESTAMP,?,?)");
			pstmt.setString(1, name.toLowerCase());
			pstmt.setString(2, options.getBehavior());
			pstmt.setString(3, new Gson().toJson(options.getRecommenders()));
			pstmt.executeUpdate();

			// Commit transaction
			conn.commit();
		} catch (SQLException e) {
			LOG.warn("- Rollback - Saving user " + name.toLowerCase()
					+ " failed.", e);
			Database.quietRollback(conn);
			throw e;
		} finally {
			Database.resetAutoCommit(conn);
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}
	}

	/**
	 * Set email of the user to the given email in the user object.
	 * 
	 */
	public void changeEmail() throws SQLException {
		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("UPDATE consumer SET email=? WHERE name=?");
			pstmt.setString(1, email);
			pstmt.setString(2, name);
			pstmt.execute();
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}
	}

	/**
	 * Set password of the user to the given password in the user object.
	 */
	public void setPassword() throws SQLException {
		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("UPDATE consumer SET password=? WHERE name=?");
			pstmt.setString(
					1,
					Encryption.BCrypt.hashpw(password,
							Encryption.BCrypt.gensalt()));
			pstmt.setString(2, name);
			pstmt.execute();
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}
	}

	/**
	 * Change password of the user to the given value in the user object.
	 */
	public void changePassword() throws SQLException {
		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("UPDATE consumer SET password=? WHERE name=?");
			pstmt.setString(1,
					Encryption.BCrypt.hashpw(sex, Encryption.BCrypt.gensalt()));
			pstmt.setString(2, name);
			pstmt.execute();
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}
	}

	/**
	 * Set Last.fm Account of the user to the value in the given user object.
	 */
	public void changeLfmaccount() throws SQLException {
		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("UPDATE consumer SET lfmaccount=? WHERE name=?");
			pstmt.setString(1, lfmaccount);
			pstmt.setString(2, name);
			pstmt.execute();
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}
	}

	/**
	 * Change profile of the user to the values in the given user object.
	 */
	public void changeProfile() throws SQLException {
		if (lfmaccount.trim().length() > 0) {
			changeLfmaccount();
		}
		if (email.trim().length() > 0) {
			changeEmail();
		}
		if (sex.trim().length() > 0) {
			changePassword();
		}
	}

	public String forgotPassword() throws SQLException, AddressException,
			MessagingException {
		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("SELECT name FROM consumer WHERE name = ?");
			pstmt.setString(1, name);
			result = pstmt.executeQuery();
			if (!result.next())
				return "user";
		} catch (SQLException e) {
			LOG.warn("Fetching users failed.", e);
			throw e;
		} finally {
			Database.quietClose(conn, pstmt, result);
		}

		// Hash username + timestamp to a token
		Date date = new Date();
		String timestamp = String.valueOf(date.getTime());
		LOG.info("Forgot password request from " + name.toLowerCase() + " at "
				+ timestamp);
		String token = Encryption.hashShaSalted(name.toLowerCase(), timestamp);

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("INSERT INTO password_recovery VALUES(?,?,CURRENT_TIMESTAMP)");
			pstmt.setString(1, token);
			pstmt.setString(2, name.toLowerCase());
			pstmt.execute();
		} catch (SQLException e) {
			LOG.warn("Couldn't insert forgot password entry.", e);
			throw e;
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}

		// Send email with the corresponding token
		Mail mail = new Mail(
				getEmail(),
				"Dear User "
						+ ", <br><br> Enter the following token into the "
						+ "corresponding field on the website to recover your password: <br>"
						+ token);
		try {
			mail.send();
		} catch (AddressException e) {
			LOG.warn("Problem with recepient mail address.", e);
			throw e;
		} catch (MessagingException e) {
			LOG.warn("Sending mail failed.", e);
			throw e;
		}
		LOG.info("Sent recovery email for user " + name);
		return "success";
	}

	/**
	 * Recover password of this user.
	 * 
	 */
	public String recoverPassword() throws SQLException {
		// Hack to provide the token
		String token = name;

		// Check token for validity in the database
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("SELECT * FROM password_recovery WHERE key = ?");
			pstmt.setString(1, token);
			result = pstmt.executeQuery();
			if (result.next()) {
				// Get the corresponding user
				String name = result.getString("consumer_name");
				this.name = name;
				// Change password to the given one
				setPassword();
				return "success";
			} else {
				return "token";
			}
		} finally {
			Database.quietClose(conn, pstmt, result);
		}
	}

	/**
	 * Count the number of ratings of the user that are not 0. <br>
	 * </br>
	 * 
	 * <b> Caution </b>: If the user takes part in an Evaluation, it will only
	 * count the ratings given during the evaluation phase.
	 * 
	 * @return Number of ratings that are not 0.
	 */
	public int countRatings() throws SQLException {
		int countRatings = 0;
		String query = "SELECT COUNT(*) as countRating FROM recommendation "
				+ "WHERE rating != 0 AND consumer = ?";

		// Check if the user is taking part in an evaluation
		int evalId = EvaluationData.getIdForParticipant(name.toLowerCase());
		if (evalId != 0) {
			query += " AND eval_id = " + evalId;
		}

		// Count ratings in the database
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, name.toLowerCase());
			result = pstmt.executeQuery();
			if (result.next()) {
				// Get the counted value
				countRatings = result.getInt("countRating");
			}
		} finally {
			Database.quietClose(conn, pstmt, result);
		}
		return countRatings;
	}
}
