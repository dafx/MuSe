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
package de.mrms.evaluation;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.mrms.user.MuseUser;
import de.mrms.user.Option;
import de.mrms.utility.Database;

/**
 * Utility class to provide access to evaluation specific data in the database.
 */
public class EvaluationData {
	private static transient final Logger LOG = LoggerFactory
			.getLogger(EvaluationData.class.getName());

	/**
	 * Save changed group settings to the database.
	 * 
	 * @param group
	 * @throws SQLException
	 */
	public static void changeGroupSettings(Group group) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = Database.getConnection();
			pstmt = conn.prepareStatement("UPDATE evaluation_groups "
					+ "SET settings_behavior = ?, settings_recommenders = ? "
					+ "WHERE group_num=? AND eval_id = ?");
			pstmt.setString(1, group.getBehavior());
			pstmt.setString(2, new Gson().toJson(group.getRecommenders()));
			pstmt.setInt(3, group.getNumGroup());
			pstmt.setInt(4, group.getEvaluationId());
			pstmt.execute();
			LOG.info("Changed group settings of evaluation "
					+ group.getEvaluationId() + " and group #"
					+ group.getNumGroup());
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}
	}

	/**
	 * Save added group to the database.
	 * 
	 * @param group
	 * @throws SQLException
	 */
	public static void addGroup(Group group) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		int groupNum = group.getNumGroup();

		try {
			conn = Database.getConnection();
			pstmt = conn.prepareStatement("INSERT INTO evaluation_groups "
					+ "VALUES(?,?,?,?)");
			pstmt.setInt(1, group.getEvaluationId());
			pstmt.setInt(2, groupNum);
			pstmt.setString(3, group.getBehavior());
			pstmt.setString(4, new Gson().toJson(group.getRecommenders()));
			pstmt.execute();
			LOG.info("Added group #" + groupNum + " to evaluation "
					+ group.getEvaluationId());
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}
	}

	/**
	 * Delete given group from given evaluation.
	 * 
	 * @param evalId
	 * @param groupNum
	 * @throws SQLException
	 */
	public static void deleteGroup(int evalId, int groupNum)
			throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = Database.getConnection();
			pstmt = conn.prepareStatement("DELETE FROM evaluation_groups "
					+ "WHERE eval_id = ? AND group_num = ?");
			pstmt.setInt(1, evalId);
			pstmt.setInt(2, groupNum);
			pstmt.execute();
			LOG.info("Deleted group #" + groupNum + " from evaluation "
					+ evalId);
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}
	}

	/**
	 * Delete given participant from given evaluation.
	 * 
	 * @param evalId
	 * @param name
	 * @throws SQLException
	 */
	public static void deleteParticipant(int evalId, String name)
			throws SQLException {
		// Otherwise, Query database
		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = Database.getConnection();
			// Start transaction
			conn.setAutoCommit(false);

			stmt = conn.prepareStatement("DELETE FROM evaluation_participants "
					+ "WHERE eval_id = ? AND participant = ?");
			stmt.setInt(1, evalId);
			stmt.setString(2, name);
			stmt.execute();

			// Remove from evaluation
			int id = EvaluationData.getIdForParticipant(name);
			if (id != 0) {
				stmt = conn.prepareStatement("UPDATE consumer "
						+ "SET eval_participant = 'N', newcomer = 'N' "
						+ "WHERE name = ?");
				stmt.setString(1, name);
				stmt.execute();
			} else {
				stmt = conn.prepareStatement("UPDATE consumer "
						+ "SET eval_participant = NULL, newcomer = 'N' "
						+ "WHERE name = ?");
				stmt.setString(1, name);
				stmt.execute();
			}

			// Delete given ratings
			stmt = conn.prepareStatement("DELETE FROM recommendation "
					+ "WHERE eval_id = ? AND consumer = ?");
			stmt.setInt(1, evalId);
			stmt.setString(2, name);
			stmt.execute();

			// Commit transaction
			conn.commit();
		} catch (SQLException e) {
			LOG.warn("- Rollback - Removing participant " + name + " failed.",
					e);
			Database.quietRollback(conn);
			throw new SQLException();
		} finally {
			Database.resetAutoCommit(conn);
			Database.quietClose(stmt);
			Database.quietClose(conn);
		}
	}

	/**
	 * Move given participant to given group in the given evaluation.
	 * 
	 * @param evalId
	 * @param groupNum
	 * @param name
	 * @throws SQLException
	 */
	public static void moveParticipant(int evalId, int groupNum, String name)
			throws SQLException {
		// Otherwise, Query database
		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = Database.getConnection();
			// Start transaction
			conn.setAutoCommit(false);

			// Update group
			stmt = conn.prepareStatement("UPDATE evaluation_participants "
					+ "SET group_id = ? WHERE eval_id = ? AND participant = ?");
			stmt.setInt(1, groupNum);
			stmt.setInt(2, evalId);
			stmt.setString(3, name);
			stmt.execute();

			// Commit transaction
			conn.commit();
		} catch (SQLException e) {
			LOG.warn("- Rollback - Moving participant " + name + " failed.", e);
			Database.quietRollback(conn);
			throw new SQLException();
		} finally {
			Database.resetAutoCommit(conn);
			Database.quietClose(stmt);
			Database.quietClose(conn);
		}
	}

	public static List<Evaluation> getHistory() throws SQLException {
		List<Evaluation> evalHistory = new ArrayList<Evaluation>();

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			result = stmt.executeQuery("SELECT * FROM Evaluation");

			// Add each evaluation row as evaluation object to the list
			while (result.next()) {
				@SuppressWarnings("unchecked")
				List<String> composition = new Gson().fromJson(
						result.getString("composition"), ArrayList.class);

				// Create evaluation object
				Evaluation eval = new Evaluation(result.getString("name"),
						result.getInt("id"), result.getString("creator"),
						composition, result.getDate("start_date"),
						result.getDate("end_date"),
						result.getDate("creation_date"));
				evalHistory.add(eval);
			}
		} catch (SQLException e) {
			LOG.warn("Selecting Evaluations from DB failed.", e);
			throw e;
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return evalHistory;
	}

	/**
	 * Get an Evaluation object containing data of the currently running
	 * evaluation.
	 * 
	 * @return Evaluation object with all information about the currently
	 *         running evaluation. NULL if no running Evaluation found.
	 */
	public static Evaluation getRunning() throws SQLException {
		Calendar today = new GregorianCalendar();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		String todayStr = format.format(today.getTime());
		Type listString = new TypeToken<ArrayList<String>>() {
		}.getType();
		Type listInt = new TypeToken<ArrayList<Integer>>() {
		}.getType();

		Evaluation eval = null;
		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("SELECT * FROM evaluation, evaluation_groups "
							+ "WHERE id = eval_id "
							+ "AND to_date(?, 'yyyy-mm-dd') BETWEEN start_date "
							+ "AND end_date ");
			pstmt.setString(1, todayStr);
			result = pstmt.executeQuery();

			// Check if a row is found
			int i = 0;
			List<Group> groups = new ArrayList<Group>();
			while (result.next()) {
				int id = result.getInt("id");
				if (i == 0) {
					// For the first row init the evaluation object
					String name = result.getString("name");
					String creator = result.getString("creator");
					List<String> composition = new Gson().fromJson(
							result.getString("composition"), listString);
					Date from = result.getDate("start_date");
					Date to = result.getDate("end_date");
					Date created = result.getDate("creation_date");
					eval = new Evaluation(name, id, creator, composition, from,
							to, created);
					i++;
				}
				int groupId = result.getInt("group_num");
				String behavior = result.getString("settings_behavior");
				List<Integer> recommenders = new Gson().fromJson(
						result.getString("settings_recommenders"), listInt);
				Group group = new Group(id, groupId, recommenders, behavior);
				groups.add(group);
			}
			// Add groups to the evaluation object
			if (eval != null) {
				eval.setGroups(groups);
			}
		} finally {
			Database.quietClose(conn, pstmt, result);
		}
		return eval;
	}

	/**
	 * Get an Evaluation object containing data of the evaluation with the given
	 * id.
	 * 
	 * @return Evaluation object with all information about the evaluation. NULL
	 *         if no evaluation with the given id was found.
	 */
	public static Evaluation getById(int id) throws SQLException {
		Type listString = new TypeToken<ArrayList<String>>() {
		}.getType();
		Type listInt = new TypeToken<ArrayList<Integer>>() {
		}.getType();

		Evaluation eval = null;

		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();

			pstmt = conn
					.prepareStatement("SELECT * FROM evaluation, evaluation_groups "
							+ "WHERE id = ? AND id = eval_id");
			pstmt.setInt(1, id);
			result = pstmt.executeQuery();

			// Check if a row is found
			int i = 0;
			List<Group> groups = new ArrayList<Group>();
			while (result.next()) {
				if (i == 0) {
					// For the first row init the evaluation object
					String name = result.getString("name");
					String creator = result.getString("creator");
					List<String> composition = new Gson().fromJson(
							result.getString("composition"), listString);
					Date from = result.getDate("start_date");
					Date to = result.getDate("end_date");
					Date created = result.getDate("creation_date");
					eval = new Evaluation(name, id, creator, composition, from,
							to, created);
					i++;
				}
				int groupId = result.getInt("group_num");
				String behavior = result.getString("settings_behavior");
				List<Integer> recommenders = new Gson().fromJson(
						result.getString("settings_recommenders"), listInt);
				Group group = new Group(id, groupId, recommenders, behavior);
				groups.add(group);
			}
			// Add groups to the evaluation object
			eval.setGroups(groups);

			// Add participants to each group
			for (Group group : eval.getGroups()) {
				pstmt = conn
						.prepareStatement("SELECT c.name, c.birthyear, c.sex, r.numRating "
								+ "FROM evaluation_participants e JOIN consumer c ON e.participant = c.name "
								+ "LEFT JOIN (SELECT consumer, COUNT(id) as numRating FROM recommendation "
								+ "WHERE eval_id = ? AND rating != 0 GROUP BY(consumer)) r "
								+ "ON c.name = r.consumer "
								+ "WHERE e.eval_id = ? " + "AND e.group_id= ?");
				pstmt.setInt(1, id);
				pstmt.setInt(2, id);
				pstmt.setInt(3, group.getNumGroup());
				result = pstmt.executeQuery();

				// Add participants to group
				while (result.next()) {
					MuseUser user = new MuseUser(result.getString("name"));
					user = new MuseUser(result.getString("name"),
							result.getInt("birthyear"),
							result.getString("sex"), user.getLangs());
					user.setNewcomerRatings(result.getInt("numRating"));
					group.addParticipant(user);
				}
			}

		} finally {
			Database.quietClose(conn, pstmt, result);
		}
		return eval;
	}

	/**
	 * Get the evaluation ID in which the given user is participating. This is
	 * only considering RUNNING evaluations.
	 * 
	 * @param user
	 *            The name of the wanted participating user.
	 * @return The ID of the evaluation, if the user is participating in any
	 *         evaluation. Otherwise 0.
	 */
	public static int getIdForParticipant(String user) throws SQLException {
		int evalId = 0;
		Calendar today = new GregorianCalendar();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		String todayStr = format.format(today.getTime());

		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("SELECT eg.eval_id "
							+ "FROM evaluation_participants eg, evaluation e, consumer c "
							+ "WHERE eg.eval_id = e.id "
							+ "AND to_date(?, 'yyyy-mm-dd') BETWEEN e.start_date "
							+ "AND e.end_date "
							+ "AND eg.participant = ? AND eg.participant = c.name "
							+ "AND c.eval_participant = 'Y'");
			pstmt.setString(1, todayStr);
			pstmt.setString(2, user);
			result = pstmt.executeQuery();

			// Check if a row is found
			if (result.next()) {
				evalId = result.getInt("eval_id");
			}
		} finally {
			Database.quietClose(conn, pstmt, result);
		}
		return evalId;
	}

	/**
	 * Get the evaluation settings for the given participant. This is only
	 * considering RUNNING evaluations.
	 * 
	 * @param user
	 *            The name of the wanted participating user.
	 * @return The options of the evaluation group the user is in. Null if no
	 *         evaluation is running or the user is not participating.
	 */
	public static Option getSettingsForParticipant(String user)
			throws SQLException {
		Option opts = null;
		Calendar today = new GregorianCalendar();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		String todayStr = format.format(today.getTime());

		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("SELECT settings_behavior, settings_recommenders "
							+ "FROM evaluation e, evaluation_groups eg, "
							+ "evaluation_participants ep  WHERE e.id = eg.eval_id "
							+ "AND eg.eval_id = ep.eval_id AND eg.group_num = ep.group_id "
							+ "AND to_date(?, 'yyyy-mm-dd') BETWEEN e.start_date "
							+ "AND e.end_date AND participant = ? AND ep.quit_date IS NULL");
			pstmt.setString(1, todayStr);
			pstmt.setString(2, user);
			result = pstmt.executeQuery();

			// Check if a row is found
			if (result.next()) {
				String behavior = result.getString("settings_behavior");
				// Parse JSON string to array
				Type collectionType = new TypeToken<ArrayList<Integer>>() {
				}.getType();
				ArrayList<Integer> ids = new Gson().fromJson(
						result.getString("settings_recommenders"),
						collectionType);
				opts = new Option(behavior, ids);
			}
		} finally {
			Database.quietClose(conn, pstmt, result);
		}
		return opts;
	}

	/**
	 * Checks if an evaluation is currently running.
	 * 
	 * @return True if and only if there is an evaluation running.
	 */
	public static boolean isRunning() throws SQLException {
		Calendar today = new GregorianCalendar();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		String todayStr = format.format(today.getTime());

		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			pstmt = conn.prepareStatement("SELECT id FROM evaluation "
					+ "WHERE to_date(?, 'yyyy-mm-dd') BETWEEN start_date "
					+ "AND end_date");
			pstmt.setString(1, todayStr);
			result = pstmt.executeQuery();

			// Check if a row is found
			if (result.next()) {
				return true;
			}
		} finally {
			Database.quietClose(conn, pstmt, result);
		}
		return false;
	}

	/**
	 * Delete the evaluation with the given id.
	 * 
	 * @param The
	 *            id of the evaluation.
	 */
	public static void deleteById(int id) throws SQLException {
		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = Database.getConnection();
			pstmt = conn.prepareStatement("DELETE FROM evaluation WHERE id=?");
			pstmt.setInt(1, id);
			pstmt.execute();
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}
	}
}
