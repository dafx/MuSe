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

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.muse.evaluation.Activity;
import de.muse.utility.Database;

/**
 * Utility class to provide access to community specific data. This should be
 * use when information about all registered users is needed.
 */
public class UserData {
	// Configured logger
	private static final Logger LOG = LoggerFactory.getLogger(UserData.class
			.getName());

	/**
	 * Get list of all registered users.
	 * 
	 * @return List of names of the users
	 */
	public static List<String> getAllUsers() {
		List<String> users = new ArrayList<String>();

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			result = stmt.executeQuery("SELECT DISTINCT name FROM consumer "
					+ "WHERE role != 'test'");

			while (result.next()) {
				String user = result.getString("name");
				users.add(user);
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get list of all users.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return users;
	}

	/**
	 * Get list of all registered users that are not new to the service any
	 * more. Meaning they submitted at least 15 ratings.
	 * 
	 * @return List of names of the users
	 */
	public static List<String> getAllActiveUsers() {
		List<String> users = new ArrayList<String>();

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			result = stmt
					.executeQuery("SELECT name FROM consumer WHERE newcomer = 'N' AND role != 'test'");

			while (result.next()) {
				String user = result.getString("name");
				users.add(user);
			}

		} catch (SQLException e) {
			e.printStackTrace();
			LOG.warn("Couldn't get list of all active users.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return users;
	}

	/** Get all lastFM accounts from registered users */
	public static List<String> getAllLfmAccounts() {
		List<String> users = new ArrayList<String>();

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			result = stmt
					.executeQuery("SELECT DISTINCT lfmaccount FROM consumer WHERE lfmaccount IS NOT NULL");

			while (result.next()) {
				users.add(result.getString("lfmaccount"));
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get lfm accounts.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return users;
	}

	/**
	 * Check user names for duplicates
	 * 
	 * @param username
	 *            The name to check for
	 * @return True if given name is unique. False if name already
	 */
	public static boolean checkUserName(String username) throws SQLException {
		boolean unique = false;

		// Query database
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet users = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("SELECT * FROM consumer WHERE name=?");
			pstmt.setString(1, username.toLowerCase());
			users = pstmt.executeQuery();

			// True if there is no result
			if (!users.next()) {
				unique = true;
			}

		} finally {
			Database.quietClose(conn, pstmt, users);
		}
		return unique;
	}

	/**
	 * Get the recommender options of the user straight from the database. NOT
	 * from the object itself.
	 * 
	 * @return Option The options of the user.
	 */
	public static Option fetchOptions(String name) {
		Option opts = null;

		// Query database
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();

			// Get the user's options
			pstmt = conn
					.prepareStatement("SELECT behavior, recommenders FROM consumer_options WHERE consumer_name ="
							+ "'" + name.toLowerCase() + "'");
			result = pstmt.executeQuery();

			if (result.next()) {
				String behavior = result.getString("behavior");
				// Parse JSON string to array
				Type collectionType = new TypeToken<ArrayList<Integer>>() {
				}.getType();
				ArrayList<Integer> ids = new Gson().fromJson(
						result.getString("recommenders"), collectionType);
				opts = new Option(behavior, ids);
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get options of user: " + name, e);
		} finally {
			Database.quietClose(conn, pstmt, result);
		}
		return opts;
	}

	/**
	 * Set the options of the user in the database.
	 * 
	 * @param behavior
	 *            Wanted behavior
	 * @param ids
	 *            Integer array representing the wanted recommenders
	 * @param name
	 *            Name of the user
	 */
	public static void saveOptions(String behavior, ArrayList<Integer> ids,
			String name) throws SQLException {

		// Query database
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = Database.getConnection();

			// Get the user's options
			pstmt = conn
					.prepareStatement("UPDATE consumer_options SET behavior = ?, recommenders = ? WHERE consumer_name = ?");
			pstmt.setString(3, name.toLowerCase());
			pstmt.setString(1, behavior);
			pstmt.setString(2, new Gson().toJson(ids));
			pstmt.execute();
			pstmt = conn
					.prepareStatement("INSERT INTO option_activities VALUES(?, CURRENT_TIMESTAMP,?,?)");
			pstmt.setString(1, name.toLowerCase());
			pstmt.setString(2, behavior);
			pstmt.setString(3, new Gson().toJson(ids));
			pstmt.execute();

		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}
	}

	/**
	 * Checks whether a given last.fm account is already in use in the system.
	 * 
	 * @param lfmaccount
	 *            The Last.fm account to test for
	 * @return Returns true if it is a duplicate, false if not
	 */
	public static boolean checkDuplicateAccount(String lfmaccount)
			throws SQLException {
		boolean duplicate = false;
		if (lfmaccount == null) {
			return duplicate;
		}

		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("SELECT lfmaccount FROM consumer WHERE lfmaccount = ?");
			pstmt.setString(1, lfmaccount);
			ResultSet result = pstmt.executeQuery();
			if (result.next())
				duplicate = true;
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}
		return duplicate;
	}

	/**
	 * Save an activity of the user to the Database.
	 * 
	 * @param activities
	 *            Map from Recommendation IDs to activity types.
	 */
	public static void putActivity(Activity activity) {

		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("INSERT INTO activities VALUES(?,?,?,CURRENT_TIMESTAMP)");
			pstmt.setString(1, activity.userName);
			pstmt.setDouble(2, activity.duration);
			pstmt.setString(3, activity.site);
			pstmt.execute();
		} catch (SQLException e) {
			LOG.warn("Inserting activities to DB failed.", e);
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}
	}

	/**
	 * Get the age group according to the birthyear. Young (0 - 30), Middle (30
	 * - 60) or Old (60+).
	 * 
	 * @param birthyear
	 * @return Age group, one of "young", "middle" or "old".
	 */
	public static String getAgeGroup(int birthyear) {
		Calendar cal = new GregorianCalendar();
		int age = cal.get(Calendar.YEAR) - birthyear;
		if (age <= 30) {
			return "young";
		} else if (age > 30 && age <= 60) {
			return "middle";
		} else {
			return "old";
		}
	}

}
