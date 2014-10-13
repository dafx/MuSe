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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muse.api.Recommendation;
import de.muse.api.Recommender;
import de.muse.config.RecommenderConfig;
import de.muse.user.MuseUser;
import de.muse.utility.Database;
import de.muse.utility.MathHelper;

/**
 * 
 * Takes care of composing recommendations of different recommendations to a
 * list of recommendations.
 * 
 */
public class ListComposer {
	// Configured logger
	private static final Logger LOG = LoggerFactory
			.getLogger(ListComposer.class.getName());

	// Fixed number of recommendations to make
	private static final int n = 10;

	/**
	 * 
	 * Creates a list of recommendations for a given user. Contains n
	 * recommendations and can be composed out of different recommenders.
	 * 
	 * @param user
	 *            The user to create recommendations for.
	 * @param behaviour
	 *            Hybrid behavior (can be "mixed", "dynamic", "weighted"). This
	 *            describes how the recommendations of the different
	 *            recommenders are composed to the final list.
	 * @param ids
	 *            An array that contains the ids of the recommenders that are to
	 *            be used for the recommendations.
	 */
	public static void createRecommendationList(MuseUser user, String behavior,
			int evalId, ArrayList<Integer> ids) throws SQLException {
		List<Recommendation> recommendations = new ArrayList<Recommendation>();

		// Get available recommenders
		HashMap<Integer, Recommender> recommenders = RecommenderConfig
				.getRecommenders();

		// If only one recommender is to be used.
		if (ids.size() == 1) {
			// Create n recommendations and save them to the database.
			Recommender recommender = recommenders.get(ids.get(0));
			recommendations = recommender.getRecommendations(user, n);
		}

		// A: behavior set to "mixed"
		else if (behavior.equals("mixed")) {
			recommendations = createMixedList(user, behavior, recommenders, ids);
		}

		// B: behavior set to "dynamic"
		else if (behavior.equals("dynamic")) {
			recommendations = createDynamicList(user, behavior, recommenders,
					ids);
		}

		// C: behavior set to "weighted"
		else if (behavior.equals("weighted")) {
			recommendations = createWeightedList(user, behavior, recommenders,
					ids);
		}

		// Save recommendations to database
		RecommenderData.putRecommendations(user.getName(), recommendations,
				evalId);
	}

	/**
	 * Compose a list of recommendations for a given user such that each
	 * recommender can provide euqally many recommendations to the list.
	 */
	private static List<Recommendation> createMixedList(MuseUser user,
			String behavior, HashMap<Integer, Recommender> recommenders,
			ArrayList<Integer> ids) {
		List<Recommendation> recommendations = new ArrayList<Recommendation>();
		HashMap<Integer, List<Recommendation>> recMap = new HashMap<Integer, List<Recommendation>>();

		// Get n recommendations of each recommender
		int count = (int) Math.floor(n / ids.size());
		for (int id : ids) {
			Recommender recommender = recommenders.get(id);
			List<Recommendation> recs = recommender.getRecommendations(user, n);

			// Check if the recommender created enough recommendations
			if (recs.size() <= count) {
				recommendations.addAll(recs);
			} else {
				recommendations.addAll(recs.subList(0, count));
				recMap.put(id, recs.subList(count, recs.size()));
			}
		}

		// Make sure wanted number of recommendations are created
		while (recommendations.size() < n && recMap.size() > 0) {
			// Count open slots and evenly distribute to each recommender
			int diff = n - recommendations.size();
			count = (int) Math.floor(diff / recMap.size());
			count = (count == 0) ? 1 : count;
			Integer[] recMapKeys = recMap.keySet().toArray(
					new Integer[recMap.size()]);

			// Add recommendations
			for (int i = 0; i < recMapKeys.length; i++) {
				// Check if the list is full (corner case)
				if (recommendations.size() == n) {
					break;
				}
				int id = recMapKeys[i];

				// Check if the recommender has enough recommendations
				if (recMap.get(id).size() <= count) {
					recommendations.addAll(recMap.get(id));
				} else {
					List<Recommendation> addRecs = new ArrayList<Recommendation>(
							recMap.get(id).subList(0, count));
					recommendations.addAll(addRecs);
					recMap.get(id).removeAll(addRecs);
				}
			}

			// Remove empty lists from the map
			for (int id : recMapKeys) {
				if (recMap.get(id).isEmpty()) {
					recMap.remove(id);
				}
			}
		}

		// Sort list according to recommendations score
		Collections.sort(recommendations, Collections.reverseOrder());
		return recommendations;
	}

	/**
	 * Compose a list of recommendations for a given user by only include those
	 * with the highest predicted rating of all recommendations from the given
	 * recommenders.
	 */
	private static List<Recommendation> createWeightedList(MuseUser user,
			String behavior, HashMap<Integer, Recommender> recommenders,
			ArrayList<Integer> ids) {
		List<Recommendation> recommendations = new ArrayList<Recommendation>();

		// Get n recommendations of each recommender
		for (int id : ids) {
			Recommender recommender = recommenders.get(id);
			recommendations.addAll(recommender.getRecommendations(user, n));
		}
		Collections.sort(recommendations, Collections.reverseOrder());
		recommendations = recommendations.subList(0, n);
		return recommendations;
	}

	/**
	 * Compose a list of recommendations for a given user by distributing the
	 * list places to match the user preferences.
	 */
	private static List<Recommendation> createDynamicList(MuseUser user,
			String behavior, HashMap<Integer, Recommender> recommenders,
			ArrayList<Integer> ids) {
		List<Recommendation> result = new ArrayList<Recommendation>();
		List<Recommendation> recommendations = new ArrayList<Recommendation>();

		// Get n recommendations of each recommender
		for (int id : ids) {
			Recommender recommender = recommenders.get(id);
			recommendations.addAll(recommender.getRecommendations(user, n));
		}
		Collections.sort(recommendations, Collections.reverseOrder());

		// Compute scores based on users rating for each recommender
		HashMap<Integer, Double> scores = new HashMap<Integer, Double>();
		for (int id : ids) {
			List<Integer> ratings = computeWeightedRatings(user.getName(), id);
			// Check if there are any ratings
			if (ratings.size() == 0)
				scores.put(id, 0.0);

			// If so compute the score for the current recommender
			int sum = MathHelper.getSumOfInts(ratings);
			double score = (double) sum / (2 * ratings.size());
			scores.put(id, score);
		}

		// Order recommendation list by weighting recommendations scores
		// with belonging recommender score (Order by score =
		// recommendationScore * recommenderScore)
		List<Recommendation> recOrder = new ArrayList<Recommendation>();
		recOrder = duplicateList(recommendations);
		for (Recommendation rec : recOrder) {
			int recommenderID = rec.getRecommenderID();
			double recommenderScore = scores.get(recommenderID);
			double recommendationScore = rec.getScore();

			// Compute new score for the recommendation
			((MuseRecommendation) rec).setScore(recommenderScore
					* recommendationScore);
		}

		// Sort list according to recommendations score and take the top n
		// recommendations
		Collections.sort(recOrder, Collections.reverseOrder());
		recOrder = recOrder.subList(0, n);

		// Map recommendations to recommendations with original score
		for (Recommendation rec : recOrder) {
			for (Recommendation recAlt : recommendations) {
				if (rec.equals(recAlt)) {
					result.add(recAlt);
					break;
				}
			}
		}

		return result;
	}

	private static List<Recommendation> duplicateList(List<Recommendation> recs) {
		List<Recommendation> recommendations = new ArrayList<Recommendation>();

		// Create new object for each object in the list and add it to a new
		// list
		for (Recommendation rec : recs) {
			MuseRecommendation recNew = new MuseRecommendation();
			recNew.setSong(rec.getSong());
			recNew.setRecommenderID(rec.getRecommenderID());
			recNew.setScore(rec.getScore());
			recommendations.add(recNew);
		}
		return recommendations;
	}

	/**
	 * Get the ratings of a certain user and a certain recommender. The ratings
	 * will be weighted in the sense that the ratings of the last two lists are
	 * included twice.
	 * 
	 * @param name
	 *            Name of the user
	 * @param recommenderID
	 *            ID of the recommender
	 * @return List of ratings as integers
	 */
	private static List<Integer> computeWeightedRatings(String name,
			int recommenderID) {
		List<Integer> ratings = new ArrayList<Integer>();

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			String query = "SELECT rating, list FROM recommendation WHERE consumer='"
					+ name
					+ "' AND recommender_id= "
					+ recommenderID
					+ " ORDER BY list DESC";
			result = stmt.executeQuery(query);
			// Save ratings to the list of integers and include the last to
			// lists two times to weight them in the score
			int currentList = 0;
			while (result.next()) {
				int rating = result.getInt("rating");
				int list = result.getInt("list");

				// Include the last two list two times
				if (currentList - list <= 2) {
					ratings.add(rating);
					ratings.add(rating);
				}
				// After the last two lists just add the single ratings
				else {
					ratings.add(rating);
				}

			}
		} catch (SQLException e) {
			LOG.warn("Couldn't get weighted ratings.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return ratings;
	}
}
