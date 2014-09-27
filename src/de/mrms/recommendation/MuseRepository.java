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
package de.mrms.recommendation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mrms.api.DataRepository;
import de.mrms.api.Recommendation;
import de.mrms.api.RecommendationFactory;
import de.mrms.api.Song;
import de.mrms.api.User;
import de.mrms.evaluation.EvaluationData;
import de.mrms.user.MuseUser;
import de.mrms.utility.Database;

public class MuseRepository implements DataRepository {
	// Configured logger
	private transient static final Logger LOG = LoggerFactory
			.getLogger(MuseRepository.class.getName());

	@Override
	public RecommendationFactory getRecommendationFactory() {
		return new MuseRecommendationFactory();
	}

	@Override
	public List<Song> getAnnualCharts(int year) {
		List<Song> songs = new ArrayList<Song>();

		// Connect to database and fetch required data
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			// Query the database
			conn = Database.getConnection();
			stmt = conn.createStatement();
			String query = "SELECT track_id, trackposition, artist, name, chartsyear"
					+ " FROM charts_year, tracks "
					+ "WHERE track_id = id AND chartsYear = "
					+ year
					+ " ORDER BY trackposition ASC";

			// Add songs to list
			result = stmt.executeQuery(query);
			while (result.next()) {
				MuseSong song = new MuseSong(result.getString("artist"),
						result.getString("name"));
				song.setID(result.getInt("track_id"));
				songs.add(song);
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get annual charts for year: " + year, e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return songs;
	}

	@Override
	public HashMap<Integer, List<Song>> getAnnualCharts(int yearStart,
			int yearEnd) {
		HashMap<Integer, List<Song>> charts = new HashMap<Integer, List<Song>>();

		// Connect to database and fetch required data
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			// Query the database
			conn = Database.getConnection();
			stmt = conn.createStatement();
			String query = "SELECT track_id, trackposition, artist, name, chartsyear"
					+ " FROM charts_year, tracks "
					+ "WHERE track_id = id AND chartsYear BETWEEN "
					+ yearStart
					+ " AND " + yearEnd + " ORDER BY trackposition ASC";

			// Add songs mapping
			result = stmt.executeQuery(query);
			while (result.next()) {
				int year = result.getInt("chartsyear");
				MuseSong song = new MuseSong(result.getString("artist"),
						result.getString("name"));
				song.setID(result.getInt("track_id"));

				// Check if there is already a list created for the year
				if (charts.containsKey(year)) {
					charts.get(year).add(song);
				} else {
					List<Song> songs = new ArrayList<Song>();
					songs.add(song);
					charts.put(year, songs);
				}
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get annual charts for period: " + yearStart
					+ " to " + yearEnd, e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return charts;
	}

	@Override
	public List<Song> getRegionalCharts(String region) {
		List<Song> songs = new ArrayList<Song>();

		// Connect to database and fetch required data
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			// Query the database
			conn = Database.getConnection();
			stmt = conn.createStatement();
			String query = "SELECT track_id, trackposition, artist, name, chartsregion"
					+ " FROM charts_region, tracks "
					+ "WHERE track_id = id AND chartsregion = '"
					+ region
					+ "' ORDER BY trackposition ASC";

			// Add songs to list
			result = stmt.executeQuery(query);
			while (result.next()) {
				MuseSong song = new MuseSong(result.getString("artist"),
						result.getString("name"));
				song.setID(result.getInt("track_id"));
				songs.add(song);
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get regional charts for region: " + region, e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return songs;
	}

	@Override
	public HashMap<String, List<Song>> getRegionalCharts(List<String> regions) {
		HashMap<String, List<Song>> charts = new HashMap<String, List<Song>>();

		// Connect to database and fetch required data
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			// Query the database
			conn = Database.getConnection();
			stmt = conn.createStatement();
			String queryStart = "SELECT track_id, trackposition, artist, name, chartsregion "
					+ "FROM charts_region cr, tracks "
					+ "WHERE cr.track_id = id AND (cr.chartsRegion=";

			// Add all regions to the query
			String queryRegions = "";
			queryRegions = "'" + regions.get(0) + "'";
			for (String region : regions) {
				if (!(region == regions.get(0))) {
					queryRegions += " or cr.chartsRegion='" + region + "'";
				}
			}
			queryRegions += ")";

			// Finish query string
			String queryEnd = " ORDER BY trackposition ASC";
			String query = queryStart + queryRegions + queryEnd;

			// Add songs mapping
			result = stmt.executeQuery(query);
			while (result.next()) {
				String region = result.getString("chartsregion");
				MuseSong song = new MuseSong(result.getString("artist"),
						result.getString("name"));
				song.setID(result.getInt("track_id"));

				// Check if there is already a list created for the region
				if (charts.containsKey(region)) {
					charts.get(region).add(song);
				} else {
					List<Song> songs = new ArrayList<Song>();
					songs.add(song);
					charts.put(region, songs);
				}
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get region charts for regions: " + regions, e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return charts;
	}

	@Override
	public List<Song> getCityCharts(String city) {
		List<Song> songs = new ArrayList<Song>();

		// Connect to database and fetch required data
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			// Query the database
			conn = Database.getConnection();
			stmt = conn.createStatement();
			String query = "SELECT trackposition, track_id, artist, name, chartscity "
					+ "FROM charts_city, tracks WHERE track_id = id AND chartscity = '"
					+ city + "' ORDER BY trackposition ASC";

			// Add songs to list
			result = stmt.executeQuery(query);
			while (result.next()) {
				MuseSong song = new MuseSong(result.getString("artist"),
						result.getString("name"));
				song.setID(result.getInt("track_id"));
				songs.add(song);
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get city chart for city: " + city, e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return songs;
	}

	@Override
	public HashMap<String, List<Song>> getCityCharts(List<String> cities) {
		HashMap<String, List<Song>> charts = new HashMap<String, List<Song>>();

		// Connect to database and fetch required data
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			// Query the database
			conn = Database.getConnection();
			stmt = conn.createStatement();
			String queryStart = "SELECT track_id, trackposition, artist, name, chartscity "
					+ "FROM charts_city cr, tracks "
					+ "WHERE cr.track_id = id AND (cr.chartscity=";

			// Add all cities to the query
			String queryRegions = "";
			queryRegions = "'" + cities.get(0) + "'";
			for (String city : cities) {
				if (!(city == cities.get(0))) {
					queryRegions += " or cr.chartscity='" + city + "'";
				}
			}
			queryRegions += ")";

			// Finish query string
			String queryEnd = " ORDER BY trackposition ASC";
			String query = queryStart + queryRegions + queryEnd;

			// Add songs mapping
			result = stmt.executeQuery(query);
			while (result.next()) {
				String city = result.getString("chartscity");
				MuseSong song = new MuseSong(result.getString("artist"),
						result.getString("name"));
				song.setID(result.getInt("track_id"));

				// Check if there is already a list created for the city
				if (charts.containsKey(city)) {
					charts.get(city).add(song);
				} else {
					List<Song> songs = new ArrayList<Song>();
					songs.add(song);
					charts.put(city, songs);
				}
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get city charts for cities: " + cities, e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return charts;
	}

	@Override
	public HashMap<String, List<Song>> getTagCharts(User user) {
		HashMap<String, List<Song>> charts = new HashMap<String, List<Song>>();

		// Get the users Last.fm account
		String lfmAccount = user.getLfmaccount();
		if (lfmAccount == null || lfmAccount.isEmpty())
			return charts;

		// Connect to database and fetch required data
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			// Query the database
			conn = Database.getConnection();
			stmt = conn.createStatement();
			String query = "SELECT track_id, trackposition, artist, name, chartstag"
					+ " FROM charts_tag, tracks WHERE track_id = id AND chartsUser="
					+ "'" + lfmAccount + "' " + " ORDER BY trackposition ASC";

			// Add songs mapping
			result = stmt.executeQuery(query);
			while (result.next()) {
				String tag = result.getString("chartstag");
				MuseSong song = new MuseSong(result.getString("artist"),
						result.getString("name"));
				song.setID(result.getInt("track_id"));

				// Check if there is already a list created for the tag
				if (charts.containsKey(tag)) {
					charts.get(tag).add(song);
				} else {
					List<Song> songs = new ArrayList<Song>();
					songs.add(song);
					charts.put(tag, songs);
				}
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get tag charts for user: " + user.getName(), e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return charts;
	}

	@Override
	public HashMap<String, List<Song>> getNeighborCharts(User user) {
		HashMap<String, List<Song>> charts = new HashMap<String, List<Song>>();

		// Get the users Last.fm account
		String lfmAccount = user.getLfmaccount();
		if (lfmAccount == null || lfmAccount.isEmpty())
			return charts;

		// Connect to database and fetch required data
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			// Query the database
			conn = Database.getConnection();
			stmt = conn.createStatement();
			String query = "SELECT track_id, artist, name, neighbor "
					+ " FROM charts_neighbor, tracks WHERE track_id = id AND chartsUser="
					+ "'" + lfmAccount + "' " + " ORDER BY trackplaycount ASC";

			// Add songs mapping
			result = stmt.executeQuery(query);
			while (result.next()) {
				String neighbor = result.getString("neighbor");
				MuseSong song = new MuseSong(result.getString("artist"),
						result.getString("name"));
				song.setID(result.getInt("track_id"));

				// Check if there is already a list created for the neighbor
				if (charts.containsKey(neighbor)) {
					charts.get(neighbor).add(song);
				} else {
					List<Song> songs = new ArrayList<Song>();
					songs.add(song);
					charts.put(neighbor, songs);
				}
			}

		} catch (SQLException e) {
			LOG.warn(
					"Couldn't get neighbor charts for user: " + user.getName(),
					e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return charts;
	}

	@Override
	public Set<Integer> getRatedSongIDs(String username) {
		Set<Integer> songIds = new HashSet<Integer>();

		// Connect to database and fetch required data
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			// Query the database
			conn = Database.getConnection();
			stmt = conn.createStatement();

			// Check if user takes part in an Evaluation
			int evalId = EvaluationData.getIdForParticipant(username);
			String evalQuery = (evalId == 0) ? "" : " AND eval_id = " + evalId;
			String query = "SELECT track_id FROM recommendation WHERE consumer = "
					+ "'" + username + "'" + evalQuery;

			// Add songs to list
			result = stmt.executeQuery(query);
			while (result.next()) {
				songIds.add(result.getInt("track_id"));
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get rated songs for user: " + username, e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return songIds;
	}

	@Override
	public LinkedHashMap<Integer, Double> getRatingsFromUser(String user) {
		LinkedHashMap<Integer, Double> ratings = new LinkedHashMap<Integer, Double>();

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			// Check if user takes part in an Evaluation
			int evalId = EvaluationData.getIdForParticipant(user);
			String evalQuery = (evalId == 0) ? "" : " AND eval_id = " + evalId;
			String query = "SELECT track_id, rating FROM recommendation WHERE consumer = '"
					+ user + "'" + evalQuery + " ORDER BY rating DESC";

			conn = Database.getConnection();
			stmt = conn.createStatement();
			result = stmt.executeQuery(query);

			while (result.next()) {
				int trackId = result.getInt("track_id");
				double rating = result.getInt("rating");
				ratings.put(trackId, rating);
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get list of ratings for user: " + user, e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return ratings;
	}

	@Override
	public Set<String> getUsers() {
		Set<String> users = new HashSet<String>();

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

	@Override
	public User getUserInfo(String username) {
		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;
		User user = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			result = stmt
					.executeQuery("SELECT sex, birthyear, lfmaccount FROM consumer WHERE name="
							+ "'" + username.toLowerCase() + "'");

			if (result.next()) {
				user = new MuseUser(username, result.getInt("birthyear"),
						result.getString("sex"), result.getString("lfmaccount"));
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get user info for: " + username, e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		user.getLangs();
		return user;
	}

	@Override
	public Set<Integer> getSongIDs() {
		Set<Integer> songs = new HashSet<Integer>();

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			result = stmt.executeQuery("SELECT id FROM tracks");

			// Get all the tracks from the database
			while (result.next()) {
				songs.add(result.getInt("id"));
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get song ids from the database.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return songs;
	}

	@Override
	public Song getSongInfo(int songID) {
		Song song = null;
		HashMap<String, Double> tags = new HashMap<String, Double>();

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			// Query tags
			result = stmt.executeQuery("SELECT tag, count FROM "
					+ "track_tags WHERE track_id = " + songID);

			while (result.next()) {
				tags.put(result.getString("tag"), result.getDouble("count"));
			}

			// Query artist and name
			result = stmt
					.executeQuery("SELECT artist, name FROM tracks WHERE id = "
							+ songID);
			while (result.next()) {
				String artist = result.getString("artist");
				String name = result.getString("name");
				song = new MuseSong(artist, name);
				((MuseSong) song).setTags(tags);
			}
		} catch (SQLException e) {
			LOG.warn("Couldn't get info for song: " + songID, e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return song;
	}

	/*
	 * Outside the data repository interface
	 */
	// Recommendation Factory implementation
	public static class MuseRecommendationFactory implements
			RecommendationFactory {
		public Recommendation createRecommendation(Song song,
				int recommenderID, String explanation, double score) {
			MuseRecommendation rec = new MuseRecommendation();
			rec.setSong(song);
			rec.setScore(score);
			rec.setExplanation(explanation);
			rec.setRecommenderID(recommenderID);
			return rec;
		}
	}

	public List<SimpleEntry<Integer, Integer>> getSongCityCount(
			List<String> cities) {
		List<SimpleEntry<Integer, Integer>> count = new ArrayList<SimpleEntry<Integer, Integer>>();

		// Connect to database and fetch required data
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			// Query the database
			conn = Database.getConnection();
			stmt = conn.createStatement();
			String queryStart = "SELECT track_id, COUNT(chartscity) AS totalcount"
					+ " FROM charts_city WHERE chartscity = ";

			// Add all cities to the query
			String queryCities = "";
			queryCities = "'" + cities.get(0) + "'";
			for (String city : cities) {
				if (!(city == cities.get(0))) {
					queryCities += " or chartscity='" + city + "'";
				}
			}

			String queryEnd = " GROUP BY track_id ORDER BY totalcount ASC";

			// Add songs mapping
			result = stmt.executeQuery(queryStart + queryCities + queryEnd);
			while (result.next()) {
				count.add(new SimpleEntry<Integer, Integer>(result
						.getInt("track_id"), result.getInt("totalcount")));
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get song uniqueness count", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}
		return count;
	}
}
