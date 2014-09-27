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
package de.mrms.recommendation.recommenders.collaborative;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mrms.api.AbstractRecommender;
import de.mrms.api.DataRepository;
import de.mrms.api.Recommendation;
import de.mrms.api.User;
import de.mrms.recommendation.MuseRecommendation;
import de.mrms.recommendation.MuseSong;
import de.mrms.utility.Database;
import de.mrms.utility.MathHelper;

public class CollaborativeFilteringRecommender extends AbstractRecommender {

	// Configured logger
	private static transient final Logger LOG = LoggerFactory
			.getLogger(CollaborativeFilteringRecommender.class.getName());

	// Meta information
	private static final String NAME = "Collaborative Filtering";
	private static final String EXPLANATION = "Recommendations are based on preferences of similar users.";
	private static final Map<String, Double> tagDistribution = new HashMap<String, Double>();
	static {
		tagDistribution.put("Accuracy", 80.0);
		tagDistribution.put("Novelty", 10.0);
		tagDistribution.put("Diversity", 0.0);
		tagDistribution.put("Serendipity", 10.0);
	}

	/**
	 * Create recommender object
	 * 
	 * @param ID
	 *            The internal ID of the recommender
	 * @param dataRepository
	 *            The data repository
	 */
	public CollaborativeFilteringRecommender(int ID,
			DataRepository dataRepository) {
		super(ID, dataRepository);
	}

	@Override
	public List<Recommendation> getRecommendations(User user, int howMany) {
		List<Recommendation> recommendations = new ArrayList<Recommendation>();

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();

			// 1) Get the tracks with highest predicted rating
			result = stmt
					.executeQuery("SELECT DISTINCT track_id, score, explanation "
							+ "FROM user_user_score WHERE track_id "
							+ "NOT IN(SELECT track_id FROM recommendation WHERE consumer = '"
							+ user.getName()
							+ "')"
							+ " AND user_name = '"
							+ user.getName() + "' ORDER BY score DESC ");

			// Save tracks in HashMap to lookup the score and in a list to keep
			// the ordering
			HashMap<Integer, Double> candidates = new HashMap<Integer, Double>();
			HashMap<Integer, String> explanations = new HashMap<Integer, String>();
			List<Integer> topN = new ArrayList<Integer>();

			while (result.next() && topN.size() < howMany) {
				// Track data
				int trackId = result.getInt("track_id");
				double score = result.getDouble("score");
				String explanationArtists = result.getString("explanation");

				if (!candidates.keySet().contains(trackId)) {
					candidates.put(trackId, score);
					explanations.put(trackId, explanationArtists);
					topN.add(trackId);
				}
			}

			/*
			 * 4) Query information about topN tracks and return as
			 * recommendations
			 */
			recommendations = mapTracksToRecommendations(topN, candidates,
					explanations);

		} catch (SQLException e) {
			LOG.warn(
					"Couldn't get CF recommendations for user: "
							+ user.getName(), e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return recommendations;
	}

	private List<Recommendation> mapTracksToRecommendations(List<Integer> ids,
			HashMap<Integer, Double> candidates,
			HashMap<Integer, String> explanations) {
		List<Recommendation> recommendations = new ArrayList<Recommendation>();

		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			// Get information of each track
			pstmt = conn
					.prepareStatement("SELECT artist, name FROM tracks WHERE id = ?");
			for (int id : ids) {
				pstmt.setInt(1, id);
				result = pstmt.executeQuery();
				while (result.next()) {
					String artist = result.getString("artist");
					String name = result.getString("name");
					MuseSong song = new MuseSong(artist, name);
					song.setID(id);
					double score = candidates.get(id);
					String explanation = explanations.get(id);

					MuseRecommendation rec = new MuseRecommendation();
					rec.setSong(song);
					rec.setScore(score);
					rec.setExplanation(explanation);
					rec.setRecommenderID(getID());
					recommendations.add(rec);
				}
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't create recommendations out of ids.", e);
		} finally {
			Database.quietClose(conn, pstmt, result);
		}
		return recommendations;
	}

	/**
	 * Precompute prediction scores for the given user considering the given
	 * neighborhood size.
	 * 
	 * @param neighborhoodSize
	 *            The number of neighbors to consider for the score computation.
	 * 
	 * @param The
	 *            name of the user
	 */
	public void refreshUserItemMatrix(String user, int neighborhoodSize) {
		List<MuseRecommendation> recommendations = new ArrayList<MuseRecommendation>();

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			pstmt = conn
					.prepareStatement("INSERT INTO user_user_score VALUES(?,?,?,?)");

			// Compute prediction scores for user
			LOG.info("Precomputing scores for user: " + user);

			/*
			 * 1) Get the k nearest neighbors of the user. Neighbor meaning user
			 * with high rating similarity
			 */
			HashMap<String, Double> neighbors = new HashMap<String, Double>();
			result = stmt.executeQuery("SELECT user_1,user_2, similarity "
					+ "FROM user_similarities WHERE user_1 = '" + user + "' "
					+ "AND ROWNUM <= " + neighborhoodSize
					+ " ORDER BY similarity DESC");

			// Save neighbors to HashMap neighbor -> similarity
			while (result.next()) {
				double similarity = result.getDouble("similarity");
				String neighbor = result.getString("user_1");
				if (neighbor.equals(user)) {
					neighbor = result.getString("user_2");
				}
				neighbors.put(neighbor, similarity);
			}

			/*
			 * 2) Get each neighbors top tracks (highest rated tracks) which the
			 * user has not yet rated
			 */
			for (String neighbor : neighbors.keySet()) {
				// Save top tracks to HashMap Track -> Rating
				LOG.info("Creating candidates of neighbor " + neighbor);
				HashMap<Integer, Double> candidates = new HashMap<Integer, Double>();
				result = stmt
						.executeQuery("SELECT track_id, rating FROM recommendation WHERE consumer = '"
								+ neighbor
								+ "' AND track_id NOT IN(SELECT track_id FROM recommendation WHERE consumer ='"
								+ user + "') ORDER BY rating DESC");

				while (result.next()) {
					int track = result.getInt("track_id");
					double rating = result.getDouble("rating");
					if (rating > 0) {
						candidates.put(track, rating);
					}
				}

				// 2.1) Compute score and add to recommendations list
				for (int id : candidates.keySet()) {
					result = stmt
							.executeQuery("SELECT artist, name FROM tracks WHERE id = "
									+ id);
					while (result.next()) {
						String artist = result.getString("artist");
						String name = result.getString("name");
						MuseSong song = new MuseSong(artist, name);
						song.setID(id);
						double score = neighbors.get(neighbor)
								* candidates.get(id);
						String explanation = "Liked by similar user "
								+ neighbor;

						MuseRecommendation rec = new MuseRecommendation();
						rec.setSong(song);
						rec.setScore(score);
						rec.setExplanation(explanation);
						recommendations.add(rec);
					}
				}

			}

			/*
			 * 3) Order list by score and save wanted number of recommendations
			 * to the
			 */
			Collections.sort(recommendations, Collections.reverseOrder());
			for (int k = 0; k < recommendations.size() && k <= 200; k++) {
				Recommendation rec = recommendations.get(k);

				pstmt.setString(1, user);
				pstmt.setInt(2, rec.getSong().getID());
				pstmt.setDouble(3, rec.getScore());
				pstmt.setString(4, rec.getExplanation());
				pstmt.addBatch();
			}
			pstmt.executeBatch();

		} catch (SQLException e) {
			LOG.warn("Precomputing scores for user " + user + " failed.", e);
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn, stmt, result);
		}
	}

	/**
	 * Compute user-user similarities for the given user by comparing ratings of
	 * different users.
	 */
	public void refreshUserSimilarities(String user) {
		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("INSERT INTO user_similarities VALUES(?,?,?)");

			// Compute user-user similarities
			LOG.info("...Computing similarities for user: " + user);
			// Counter for batch inserting to the database
			int count = 0;

			// Get all ratings of the user
			HashMap<Integer, Double> ratingsOne = getDataRepository()
					.getRatingsFromUser(user);

			// Compare to ratings of all neighbor candidates
			HashSet<String> neighborCandidates = (HashSet<String>) getDataRepository()
					.getUsers();

			for (String userTwo : neighborCandidates) {
				// Don't compare to the user itself
				if (user.equals(userTwo))
					continue;

				// Get the ratings of the other user
				HashMap<Integer, Double> ratingsTwo = getDataRepository()
						.getRatingsFromUser(user);

				// Compute similarity between the two user by pearson
				// correlation of
				// the rating vectors
				double similarity = MathHelper.computePearsonCorrelation(
						ratingsOne, ratingsTwo);

				// Only save positive similarities
				if (similarity <= 0.0)
					continue;

				// If not zero save similarity to database
				pstmt.setString(1, user);
				pstmt.setString(2, userTwo);
				pstmt.setDouble(3, similarity);
				pstmt.addBatch();

				// Save stacks of size 1000 to the database
				count++;
				if (count == 1000) {
					pstmt.executeBatch();
					count = 0;
				}
				LOG.info("Computed similarity " + similarity + " of " + user
						+ " and " + userTwo + ".");
			}
			pstmt.executeBatch();

		} catch (SQLException e) {
			LOG.warn("Couldn't save user similarities to the database: ", e);
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getExplanation() {
		return EXPLANATION;
	}

	@Override
	public Map<String, Double> getTagDistribution() {
		return tagDistribution;
	}

}
