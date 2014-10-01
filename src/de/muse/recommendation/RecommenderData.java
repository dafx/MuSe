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
package de.muse.recommendation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muse.api.Recommendation;
import de.muse.utility.Database;

/**
 * Utility class to provide access to recommender specific data. This should be
 * used when information about user ratings, preferences or item data is needed
 * in order to create recommendations.
 */
public class RecommenderData {
	// Configured logger
	private static final Logger LOG = LoggerFactory
			.getLogger(RecommenderData.class.getName());

	/**
	 * Save a list of recommendations to the database
	 * 
	 * @throws SQLException
	 */
	public static void putRecommendations(String name,
			List<Recommendation> recs, int evalId) throws SQLException {
		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			// Start transaction
			conn.setAutoCommit(false);

			// Get the list value
			pstmt = conn
					.prepareStatement("SELECT list FROM consumer WHERE name='"
							+ name + "'");
			result = pstmt.executeQuery();
			int list = 0;
			while (result.next()) {
				list = result.getInt("list");
			}

			// Save list to db
			pstmt = conn
					.prepareStatement("INSERT INTO recommendation VALUES(RECID.nextval,  CURRENT_TIMESTAMP, ?, 0, ?, ?, ?, ?, ?, ?)");
			// Save list of recommendations
			for (Recommendation rec : recs) {
				pstmt.setString(1, name);
				pstmt.setInt(2, list + 1);
				pstmt.setInt(3, rec.getRecommenderID());
				pstmt.setString(4, rec.getExplanation());
				pstmt.setDouble(5, rec.getScore());
				pstmt.setInt(6, rec.getSong().getID());
				if (evalId == 0) {
					pstmt.setNull(7, java.sql.Types.INTEGER);
				} else {
					pstmt.setInt(7, evalId);
				}
				pstmt.execute();
			}

			// Write back new list id for user
			list++;
			pstmt = conn
					.prepareStatement("UPDATE consumer SET list = ? WHERE name= ?");
			pstmt.setInt(1, list);
			pstmt.setString(2, name);
			pstmt.execute();

			// Create row in list rating table
			pstmt = conn
					.prepareStatement("INSERT INTO recommendation_list VALUES(?,?,0,CURRENT_TIMESTAMP)");
			pstmt.setString(1, name);
			pstmt.setInt(2, list);
			pstmt.execute();

			LOG.info("Created recommendation list for user: " + name);

			// Transaction succeeded. Commit it.
			conn.commit();
		} catch (SQLException e) {
			LOG.warn("- Rollback - Saving recommendations to database for: "
					+ name + " failed.", e);
			Database.quietRollback(conn);
			throw e;
		} finally {
			// Finished transaction
			Database.resetAutoCommit(conn);
			Database.quietClose(conn, pstmt, result);
		}
	}

	/**
	 * Put ratings to database.
	 * 
	 * @param ratings
	 *            Map of key value pairs. Where recommendation id as key and
	 *            rating as value.
	 * 
	 */
	public static void putRatings(MultivaluedMap<String, String> evals)
			throws SQLException {
		List<String> keys = new ArrayList<String>(evals.keySet());
		if (keys.isEmpty())
			return;

		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("UPDATE recommendation SET rating = ? WHERE id = ?");

			for (int i = 0; i < evals.size(); i++) {
				String key = keys.get(i);
				if (key.equals("behavior") || key.equals("recommenders")
						|| key.equals("places") || key.equals("list")) {
					continue;
				}
				List<String> values = evals.get(key);
				// Radio button can only have one value per key
				String value = values.get(0);
				pstmt.setString(1, value);
				pstmt.setString(2, key);
				pstmt.execute();
			}

			// Query list information
			int list = 0;
			String userName = null;
			pstmt = conn.prepareStatement("SELECT consumer, list "
					+ "FROM recommendation WHERE id = ?");
			pstmt.setString(1, keys.get(0));
			result = pstmt.executeQuery();
			while (result.next()) {
				list = result.getInt("list");
				userName = result.getString("consumer");
			}

			// Save list rating to database
			pstmt = conn.prepareStatement("UPDATE recommendation_list "
					+ "SET rating = ? WHERE consumer = ? and list_id = ?");
			pstmt.setString(1, evals.get("list").get(0));
			pstmt.setString(2, userName);
			pstmt.setInt(3, list);
			pstmt.execute();

		} catch (SQLException e) {
			LOG.warn("Couldn't save ratings.", e);
			throw e;
		} finally {
			Database.quietClose(conn, pstmt, result);
		}
	}

	/**
	 * Get current list of recommendations for a user from the database
	 * 
	 * @param name
	 *            The name of the user
	 */
	public static List<MuseRecommendation> getCurrentRecommendationList(
			String name) throws SQLException {
		List<MuseRecommendation> recs = new ArrayList<MuseRecommendation>();

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;
		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			// Get id of current list of the user
			int id = 0;
			result = stmt.executeQuery("SELECT list FROM consumer WHERE name="
					+ "'" + name + "'");

			while (result.next()) {
				id = result.getInt("list");
			}
			// Query the users current recommendation list
			result = stmt
					.executeQuery("SELECT r.id, artist, name, recommender_id, explanation, score "
							+ "FROM Recommendation r JOIN tracks ON r.track_id = tracks.id WHERE list ="
							+ String.valueOf(id)
							+ " AND consumer ='"
							+ name
							+ "' ORDER BY r.id ASC");

			while (result.next()) {
				MuseRecommendation rec = new MuseRecommendation();
				rec.setId(result.getString("id"));
				MuseSong song = new MuseSong(result.getString("artist"),
						result.getString("name"));
				rec.setSong(song);
				rec.setRecommenderID(result.getInt("recommender_id"));
				rec.setExplanation(result.getString("explanation"));
				rec.setScore(result.getDouble("score"));
				recs.add(rec);
			}
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return recs;
	}

	/**
	 * Get recommender id for the next recommender to be added.
	 */
	public static int getNextRecommenderId() throws SQLException {
		int id = 0;
		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;
		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			result = stmt
					.executeQuery("select CONFIGID.nextval AS id from dual");

			if (result.next()) {
				id = result.getInt("id");
			}
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return id;
	}
}
