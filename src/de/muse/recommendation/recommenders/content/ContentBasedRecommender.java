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
package de.muse.recommendation.recommenders.content;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muse.api.AbstractRecommender;
import de.muse.api.DataRepository;
import de.muse.api.Recommendation;
import de.muse.api.User;
import de.muse.recommendation.MuseRecommendation;
import de.muse.recommendation.MuseSong;
import de.muse.utility.Database;
import de.muse.utility.MathHelper;

public class ContentBasedRecommender extends AbstractRecommender {
	// Configured logger
	private static transient final Logger LOG = LoggerFactory
			.getLogger(ContentBasedRecommender.class.getName());

	// Meta information
	private static final String NAME = "Content Based";
	private static final String EXPLANATION = "Recommendations are based on items that are similar to items you like.";
	private static final Map<String, Double> tagDistribution = new HashMap<String, Double>();
	static {
		tagDistribution.put("Accuracy", 70.0);
		tagDistribution.put("Novelty", 20.0);
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
	public ContentBasedRecommender(int ID, DataRepository dataRepository) {
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
							+ "FROM user_track_score WHERE track_id "
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
					"Couldn't get recommendations for user: " + user.getName(),
					e);
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
					String explanation = "You liked: " + explanations.get(id);

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
	 * Precompute prediction scores for the given user.
	 * 
	 * @param neighborhoodSize
	 *            The number of neighbors to consider for the score computation.
	 * @param user
	 *            The name of the user.
	 * 
	 */
	public void refreshUserItemMatrix(String user, int neighborhoodSize) {
		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;
		PreparedStatement pstmt2 = null;
		Statement stmt = null;
		ResultSet result = null;
		ResultSet result2 = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			pstmt = conn
					.prepareStatement("INSERT INTO user_track_score VALUES(?,?,?,?)");
			pstmt2 = conn
					.prepareStatement("SELECT track_id_1,track_id_2, similarity FROM "
							+ "(SELECT track_id_1, track_id_2, similarity "
							+ "FROM track_similarities "
							+ "WHERE track_id_1 = ? OR track_id_2 = ?"
							+ "ORDER BY similarity DESC) WHERE ROWNUM <= ?");

			// Compute prediction scores
			LOG.info("...Precomputing scores for user: " + user);

			// Get already rated songs of the current user
			LinkedHashMap<Integer, Double> ratedSongs = getDataRepository()
					.getRatingsFromUser(user);

			// Compute prediction score for neighbors of each rated song
			for (int songID : ratedSongs.keySet()) {
				// Check for positive rating
				if (ratedSongs.get(songID) != 2 && ratedSongs.get(songID) != 1) {
					// There are no more songs that the user likes
					break;
				}

				pstmt2.setInt(1, songID);
				pstmt2.setInt(2, songID);
				pstmt2.setInt(3, neighborhoodSize);
				result = pstmt2.executeQuery();

				while (result.next()) {
					// Get similarity of the neighbor
					Double similarity = result.getDouble("similarity");
					int neighborId = result.getInt("track_id_1");
					if (neighborId == songID) {
						neighborId = result.getInt("track_id_2");
					}

					// Check song was already rated by the user
					double score = 0;
					String artist = "";
					if (!ratedSongs.containsKey(neighborId)) {
						score = similarity * ratedSongs.get(songID);

						// Get artist name for explanation
						result2 = stmt
								.executeQuery("SELECT artist FROM tracks WHERE id = "
										+ songID);
						while (result2.next()) {
							artist = result2.getString("artist");
						}

						// 3) Save prediction score to database
						pstmt.setString(1, user);
						pstmt.setInt(2, neighborId);
						pstmt.setDouble(3, score);
						pstmt.setString(4, artist);
						pstmt.addBatch();

					}
					LOG.info("Computed prediction " + score + " for user "
							+ user + " and " + neighborId + ".");
				}
				pstmt.executeBatch();
			}
		} catch (SQLException e) {
			LOG.warn("Couldn't save similarities to the database: ", e);
		} finally {
			Database.quietClose(result2);
			Database.quietClose(result);
			Database.quietClose(pstmt);
			Database.quietClose(pstmt2);
			Database.quietClose(stmt);
			Database.quietClose(conn);
		}
	}

	/**
	 * Compute and save content based similarities for each song
	 */
	public static void refreshTrackSimilarities() {
		List<MuseSong> songs = getAllTracksMissingTrackSimilarities();
		HashSet<MuseSong> done = new HashSet<MuseSong>();

		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("INSERT INTO track_similarities VALUES(?,?,?)");

			// Compute the similarity
			for (MuseSong songOne : songs) {
				LOG.info("Compute similarities for song: " + songOne.toString());
				int count = 0;
				for (MuseSong songTwo : songs) {
					LOG.info(" - Compute similarity to " + songTwo.toString()
							+ ".");

					// Avoid duplicate computations
					if (songOne.equals(songTwo) || done.contains(songTwo))
						continue;

					// Compute similarity and save if not zero
					double similarity = MathHelper.computeCosineDistance(
							songOne.getTags(), songTwo.getTags());
					if (similarity == 0.0)
						continue;

					// If not zero save similarity to database
					pstmt.setInt(1, songOne.getID());
					pstmt.setInt(2, songTwo.getID());
					pstmt.setDouble(3, similarity);
					pstmt.addBatch();

					// Save stacks of size 1000 to the database
					count++;
					if (count == 1000) {
						pstmt.executeBatch();
						count = 0;
					}
				}
				pstmt.executeBatch();
				done.add(songOne);
			}
			LOG.info("Computed similarities for " + done.size() + " tracks.");

		} catch (SQLException e) {
			LOG.warn("Couldn't save track similarities to the database. ", e);
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}
	}

	/**
	 * Get all tracks including the corresponding tags from the database for
	 * which no content based similarity is computed.
	 * 
	 * @return A list of all tracks including corresponding tags
	 */
	public static List<MuseSong> getAllTracksMissingTrackSimilarities() {
		LOG.info("Fetching all tracks from the database.");
		List<MuseSong> tracks = new ArrayList<MuseSong>();

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			result = stmt
					.executeQuery("SELECT id, name, artist FROM tracks "
							+ "WHERE id NOT IN (SELECT track_id_1 FROM track_similarities) "
							+ "AND id NOT IN(SELECT track_id_2 FROM track_similarities)");

			// Get all the tracks from the database
			while (result.next()) {
				String name = result.getString("name");
				String artist = result.getString("artist");
				MuseSong song = new MuseSong(artist, name);
				song.setID(result.getInt("id"));
				tracks.add(song);
			}

			// Get all corresponding tags for each of the tracks
			for (MuseSong song : tracks) {
				int id = song.getID();
				result = stmt.executeQuery("SELECT tag, count FROM "
						+ "track_tags WHERE track_id = " + id);

				HashMap<String, Double> tags = new HashMap<String, Double>();
				while (result.next()) {
					tags.put(result.getString("tag"), result.getDouble("count"));
				}
				song.setTags(tags);
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get list of tracks from the database.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return tracks;
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
