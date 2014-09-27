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
package de.mrms.evaluation.result;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import de.mrms.config.RecommenderConfig;
import de.mrms.utility.Database;
import de.mrms.utility.MathHelper;

public class EvaluationDataProvider {
	private int evalId;
	private FileProvider fileProvider;

	public EvaluationDataProvider(int evalId, String filePath) {
		this.evalId = evalId;
		fileProvider = new FileProvider(filePath);
	}

	/**
	 * Create file containing all rating data of the evaluation
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public void createRatingDataFile() throws SQLException, IOException {
		// Connect to database
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn
					.prepareStatement("SELECT r.id, r.time, ep.group_id, r.consumer,"
							+ " r.list , r.recommender_id, r.rating, r.score "
							+ "FROM recommendation r JOIN evaluation_participants ep "
							+ "ON r.consumer = ep.participant WHERE r.eval_id = ?");
			stmt.setInt(1, evalId);

			// Query for number of males
			result = stmt.executeQuery();
			fileProvider.resultToCSV(result);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
	}

	/**
	 * Create file containing all MAE data of the evaluation
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public void createMAEDataFile() throws SQLException, IOException {
		// Connect to database
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn
					.prepareStatement("SELECT ep.group_id, r.recommender_id, "
							+ "AVG(abs(r.rating - r.score)) as MAE "
							+ "FROM Recommendation r JOIN evaluation_participants ep "
							+ "ON r.consumer = ep.participant WHERE r.rating != 0 "
							+ "AND r.eval_id = ? GROUP BY(r.recommender_id, ep.group_id)");
			stmt.setInt(1, evalId);

			// Query for number of males
			result = stmt.executeQuery();
			fileProvider.resultToCSV(result);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
	}

	/**
	 * Create file containing all user data of the evaluation
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public void createUserDataFile() throws SQLException, IOException {
		// Connect to database
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn
					.prepareStatement("SELECT ep.group_id, ep.join_date, ep.quit_date, c.name,c.birthyear,c.sex "
							+ "FROM evaluation_participants ep JOIN consumer c "
							+ "ON c.name = ep.participant WHERE eval_id = ?");
			stmt.setInt(1, evalId);

			// Query for number of males
			result = stmt.executeQuery();
			fileProvider.resultToCSV(result);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
	}

	/**
	 * Create file containing all rating-time data of the evaluation
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public void createRatingTimeDataFile() throws SQLException, IOException {
		// Connect to database
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn
					.prepareStatement("SELECT ep.group_id, r.day, count(r.id) as numRatings "
							+ "FROM evaluation_participants ep JOIN "
							+ "(SELECT consumer, id, eval_id, trunc(time) as day "
							+ "FROM recommendation WHERE rating != 0 AND eval_id = ?) r "
							+ "ON ep.participant = r.consumer GROUP BY(ep.group_id, r.day)");
			stmt.setInt(1, evalId);

			// Query for number of males
			result = stmt.executeQuery();
			fileProvider.resultToCSV(result);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
	}

	/**
	 * Create file containing all list ratings data of the evaluation
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public void createListRatingDataFile() throws SQLException, IOException {
		ArrayList<String[]> entries = new ArrayList<String[]>();

		// Connect to database
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn
					.prepareStatement("SELECT DISTINCT ep.group_id, r.consumer, r.list, rl.rating "
							+ "FROM recommendation r JOIN recommendation_list rl "
							+ "ON r.consumer= rl.consumer AND r.list = rl.list_id "
							+ "JOIN evaluation_participants ep "
							+ "ON r.consumer = ep.participant WHERE r.rating != 0 AND r.eval_id = ?"
							+ " ORDER BY r.list ASC");
			stmt.setInt(1, evalId);

			// Query for number of males
			result = stmt.executeQuery();
			HashMap<String, Integer> userListCount = new HashMap<String, Integer>();
			HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> groupListRatings = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();
			while (result.next()) {
				int group = result.getInt("group_id");
				String user = result.getString("consumer");
				int rating = result.getInt("rating");

				// First time seeing this user
				if (!userListCount.containsKey(user)) {
					// Insert user with first list
					userListCount.put(user, 1);
				} else {
					// Increment list number
					userListCount.put(user, userListCount.get(user) + 1);
				}
				int list = userListCount.get(user);

				// First time seeing this group
				if (!groupListRatings.containsKey(group)) {
					// Put group map
					groupListRatings.put(group,
							new HashMap<Integer, ArrayList<Integer>>());
				}

				// First time seeing this list for this group
				if (!groupListRatings.get(group).containsKey(list)) {
					// Insert list number and put first rating
					ArrayList<Integer> ratings = new ArrayList<Integer>();
					ratings.add(rating);
					groupListRatings.get(group).put(list, ratings);
				} else {
					// Add rating to the list
					groupListRatings.get(group).get(list).add(rating);
				}
			}
			// Compute average of the created rating lists
			String[] columns = { "group_id", "list_num", "rating" };
			entries.add(columns);
			for (Integer group : groupListRatings.keySet()) {
				for (Integer list : groupListRatings.get(group).keySet()) {
					ArrayList<Integer> ratings = groupListRatings.get(group)
							.get(list);
					for (Integer rating : ratings) {
						String[] entry = { String.valueOf(group),
								String.valueOf(list), String.valueOf(rating) };
						entries.add(entry);
					}
				}
			}
			fileProvider.arrayToCSV(entries);

		} finally {
			Database.quietClose(conn, stmt, result);
		}
	}

	/**
	 * Create file containing average list ratings data of the evaluation
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public void createAvgListRatingDataFile() throws SQLException, IOException {
		ArrayList<String[]> entries = new ArrayList<String[]>();

		// Connect to database
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn
					.prepareStatement("SELECT DISTINCT ep.group_id, r.consumer, r.list, rl.rating "
							+ "FROM recommendation r JOIN recommendation_list rl "
							+ "ON r.consumer= rl.consumer AND r.list = rl.list_id "
							+ "JOIN evaluation_participants ep "
							+ "ON r.consumer = ep.participant WHERE r.rating != 0 AND r.eval_id = ?"
							+ " ORDER BY r.list ASC");
			stmt.setInt(1, evalId);

			// Query for number of males
			result = stmt.executeQuery();
			HashMap<String, Integer> userListCount = new HashMap<String, Integer>();
			HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> groupListRatings = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();
			while (result.next()) {
				int group = result.getInt("group_id");
				String user = result.getString("consumer");
				int rating = result.getInt("rating");

				// First time seeing this user
				if (!userListCount.containsKey(user)) {
					// Insert user with first list
					userListCount.put(user, 1);
				} else {
					// Increment list number
					userListCount.put(user, userListCount.get(user) + 1);
				}
				int list = userListCount.get(user);

				// First time seeing this group
				if (!groupListRatings.containsKey(group)) {
					// Put group map
					groupListRatings.put(group,
							new HashMap<Integer, ArrayList<Integer>>());
				}

				// First time seeing this list for this group
				if (!groupListRatings.get(group).containsKey(list)) {
					// Insert list number and put first rating
					ArrayList<Integer> ratings = new ArrayList<Integer>();
					ratings.add(rating);
					groupListRatings.get(group).put(list, ratings);
				} else {
					// Add rating to the list
					groupListRatings.get(group).get(list).add(rating);
				}
			}

			// Compute average of the created rating lists
			String[] columns = { "group_id", "list_num", "avg_rating" };
			entries.add(columns);
			for (Integer group : groupListRatings.keySet()) {
				for (Integer list : groupListRatings.get(group).keySet()) {
					ArrayList<Integer> ratings = groupListRatings.get(group)
							.get(list);

					Double avg = (double) (MathHelper
							.getSumOfInts(groupListRatings.get(group).get(list)) / ratings
							.size());
					String[] entry = { String.valueOf(group),
							String.valueOf(list), String.valueOf(avg) };
					entries.add(entry);
				}
			}
			fileProvider.arrayToCSV(entries);

		} finally {
			Database.quietClose(conn, stmt, result);
		}
	}

	/**
	 * Create file containing average list ratings data of the evaluation
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public void createAccuracyDataFile() throws SQLException, IOException {
		ArrayList<String[]> entries = new ArrayList<String[]>();

		// Connect to database
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn
					.prepareStatement("SELECT ep.group_id, "
							+ "r.recommender_id, COUNT(r.rating) as tp "
							+ "FROM recommendation r JOIN evaluation_participants ep "
							+ "ON r.consumer = ep.participant WHERE r.rating > 0 "
							+ "AND r.eval_id = ? GROUP BY(ep.group_id, r.recommender_id)");
			stmt.setInt(1, evalId);

			// Query database for true positives per group and recommender
			result = stmt.executeQuery();
			HashMap<Integer, HashMap<Integer, Integer>> truePositives = new HashMap<Integer, HashMap<Integer, Integer>>();
			while (result.next()) {
				int group = result.getInt("group_id");
				int recommender = result.getInt("recommender_id");
				int tp = result.getInt("tp");

				// First time seeing this group
				if (!truePositives.containsKey(group)) {
					// Put group map
					truePositives.put(group, new HashMap<Integer, Integer>());
				}

				// Put tp value for the recommender
				truePositives.get(group).put(recommender, tp);
			}

			stmt = conn
					.prepareStatement("SELECT ep.group_id, "
							+ "r.recommender_id, COUNT(r.rating) as num "
							+ "FROM recommendation r JOIN evaluation_participants ep "
							+ "ON r.consumer = ep.participant WHERE r.rating != 0 "
							+ "AND r.eval_id = ? GROUP BY(ep.group_id, r.recommender_id)");
			stmt.setInt(1, evalId);

			// Query database for true
			String[] columns = { "group_id", "recommender_id", "accuracy" };
			entries.add(columns);
			result = stmt.executeQuery();
			while (result.next()) {
				int group = result.getInt("group_id");
				int recommender = result.getInt("recommender_id");
				int count = result.getInt("num");

				// Compute accuracy values and save them to the result list
				int tp = 0;
				if (truePositives.containsKey(group)
						&& truePositives.get(group).containsKey(recommender)) {
					tp = truePositives.get(group).get(recommender);
				}

				double accuracy = (count == 0) ? 0 : (double) tp
						/ (double) count;
				String[] entry = { String.valueOf(group),
						String.valueOf(recommender), String.valueOf(accuracy) };
				entries.add(entry);
			}
			fileProvider.arrayToCSV(entries);

		} finally {
			Database.quietClose(conn, stmt, result);
		}
	}

	/**
	 * Create file containing recommender mapping id -> name
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public void createRecommenderMapDataFile() throws SQLException, IOException {
		ArrayList<String[]> entries = new ArrayList<String[]>();
		String[] columns = { "ID", "NAME" };
		entries.add(columns);
		for (Integer recId : RecommenderConfig.getRecommenders().keySet()) {
			String[] entry = { recId.toString(),
					RecommenderConfig.getRecommenders().get(recId).getName() };
			entries.add(entry);
		}
		fileProvider.arrayToCSV(entries);

	}

	/**
	 * Create file containing average group rating data of the evaluation
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public void createAvgGroupRatingDataFile() throws SQLException, IOException {
		// Connect to database
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.prepareStatement("SELECT ep.group_id, "
					+ "AVG(r.rating) as avgRating "
					+ "FROM Recommendation r JOIN evaluation_participants ep "
					+ "ON r.consumer = ep.participant WHERE r.rating != 0 "
					+ "AND r.eval_id = ? GROUP BY(ep.group_id)");
			stmt.setInt(1, evalId);

			// Query for number of males
			result = stmt.executeQuery();
			fileProvider.resultToCSV(result);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
	}

	/**
	 * Create file containing average group rating data of the evaluation
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public void createAvgRecommenderRatingDataFile() throws SQLException,
			IOException {
		// Connect to database
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn
					.prepareStatement("SELECT ep.group_id, r.recommender_id, "
							+ "AVG(r.rating) as avgRating "
							+ "FROM Recommendation r JOIN evaluation_participants ep "
							+ "ON r.consumer = ep.participant WHERE r.rating != 0 "
							+ "AND r.eval_id = ? GROUP BY(r.recommender_id, ep.group_id)");
			stmt.setInt(1, evalId);

			// Query database
			result = stmt.executeQuery();
			fileProvider.resultToCSV(result);

		} finally {
			Database.quietClose(conn, stmt, result);
		}
	}

	/**
	 * Create file containing age distribution of each group in the evaluation
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public void createAgeDistDataFile() throws SQLException, IOException {
		// Connect to database
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn
					.prepareStatement("SELECT ep.group_id, c.birthyear, "
							+ "COUNT(c.birthyear) as numAge FROM consumer c "
							+ "JOIN evaluation_participants ep ON c.name = ep.participant "
							+ "WHERE ep.eval_id = ? GROUP BY(ep.group_id, c.birthyear)");
			stmt.setInt(1, evalId);

			// Query database
			result = stmt.executeQuery();
			fileProvider.resultToCSV(result);

		} finally {
			Database.quietClose(conn, stmt, result);
		}
	}

	/**
	 * Create file containing gender distribution of each group in the
	 * evaluation
	 * 
	 * @throws SQLException
	 * @throws IOException
	 */
	public void createGenderDistDataFile() throws SQLException, IOException {
		// Connect to database
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn
					.prepareStatement("SELECT ep.group_id, c.sex, "
							+ "COUNT(c.sex) as numGender FROM consumer c "
							+ "JOIN evaluation_participants ep ON c.name = ep.participant "
							+ "WHERE ep.eval_id = ? GROUP BY(ep.group_id, c.sex)");
			stmt.setInt(1, evalId);

			// Query database
			result = stmt.executeQuery();
			fileProvider.resultToCSV(result);

		} finally {
			Database.quietClose(conn, stmt, result);
		}
	}

}
