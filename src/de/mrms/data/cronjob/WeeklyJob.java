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
package de.mrms.data.cronjob;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mrms.data.charts.Charts;
import de.mrms.data.charts.CityCharts;
import de.mrms.data.charts.NeighborCharts;
import de.mrms.data.charts.TagCharts;
import de.mrms.data.social.lastfm.LastFmConnector;
import de.mrms.recommendation.MuseSong;
import de.mrms.recommendation.recommenders.content.ContentBasedRecommender;
import de.mrms.utility.Database;

/**
 * Refreshing the data of the recommender system.
 */
public class WeeklyJob implements Job {
	// Configured Logger
	private static final Logger LOG = LoggerFactory.getLogger(WeeklyJob.class
			.getName());

	@Override
	public void execute(JobExecutionContext context) {
		LOG.warn("Weekly cronjob started.");
		refreshData();
		LOG.warn("Weekly cronjob finished.");
	}

	/**
	 * Refresh Track_Tag relation. Gets tags and their count for all tracks in
	 * the database and saves them to the database.
	 */
	public static void refreshTagRelations() {
		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			result = stmt.executeQuery("SELECT id,name, artist FROM tracks "
					+ "WHERE id NOT IN(select track_id FROM track_tags)");

			pstmt = conn
					.prepareStatement("INSERT INTO track_tags VALUES(?,?,?)");
			int i = 0;
			while (result.next()) {
				String name = result.getString("name");
				String artist = result.getString("artist");
				int id = result.getInt("id");

				// If not already computed, do it and save to database
				HashMap<String, String> tags = LastFmConnector.getCountTopTags(
						artist, name, 5);

				for (String tag : tags.keySet()) {
					pstmt.setInt(1, id);
					pstmt.setString(2, tag);
					pstmt.setString(3, tags.get(tag));
					pstmt.addBatch();
				}

				// Only save stacks of 500 songs to the database
				i++;
				if (i % 100 == 0) {
					LOG.info("Saving to database...");
					pstmt.executeBatch();
				}
				LOG.info("Fetched tags for Song: " + name + " - " + artist);
			}
			pstmt.executeBatch();
			LOG.info("Fetched tags for " + i + " songs,");
		} catch (SQLException e) {
			LOG.warn("Refreshing tag relation failed.", e);
			throw new RuntimeException();
		} finally {
			Database.quietClose(stmt);
			Database.quietClose(conn, pstmt, result);
		}
	}

	/**
	 * Fetch MBIDs from Last.fm for all tracks in the Tracks table.
	 */
	public static void refreshMbids() {
		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			result = stmt
					.executeQuery("SELECT id,name, artist FROM tracks WHERE mbid IS NULL");

			pstmt = conn
					.prepareStatement("UPDATE tracks SET mbid = ? WHERE id = ?");
			int i = 0;
			while (result.next()) {
				String name = result.getString("name");
				String artist = result.getString("artist");
				int id = result.getInt("id");

				// Fetch mbid from lastfm
				String mbid = LastFmConnector.getMbid(artist, name);
				pstmt.setString(1, mbid);
				pstmt.setInt(2, id);
				pstmt.addBatch();

				// Only save stacks of 500 songs to the database
				i++;
				if (i % 100 == 0) {
					LOG.info("Saving to database...");
					pstmt.executeBatch();
				}
				LOG.info("Fetched mbid for Song: " + name + " - " + artist);
			}
			pstmt.executeBatch();
		} catch (SQLException e) {
			LOG.warn("Fetching mbids failed.", e);
			throw new RuntimeException();
		} finally {
			Database.quietClose(stmt);
			Database.quietClose(conn, pstmt, result);
		}
	}

	/**
	 * Clean fetched song data by removing possible duplicates.
	 */
	public static void cleanSongData() {
		Connection conn = null;
		Statement stmt = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			pstmt = conn.prepareStatement("DELETE FROM tracks WHERE id = ?");

			result = stmt.executeQuery("SELECT * FROM tracks ORDER BY id ASC");

			List<MuseSong> done = new ArrayList<MuseSong>();
			HashMap<Integer, List<Integer>> duplicatesMap = new HashMap<Integer, List<Integer>>();

			while (result.next()) {
				MuseSong song = new MuseSong(result.getString("artist").trim()
						.toLowerCase(), result.getString("name").trim()
						.toLowerCase());
				song.setID(result.getInt("id"));

				// Keep track of duplicate ids and the original id
				int uniqueSongIndex = done.indexOf(song);
				if (uniqueSongIndex != -1) {
					int uniqueSongID = done.get(uniqueSongIndex).getID();
					if (!duplicatesMap.containsKey(uniqueSongID)) {
						List<Integer> duplicateSongs = new ArrayList<Integer>();
						duplicateSongs.add(song.getID());
						duplicatesMap.put(uniqueSongID, duplicateSongs);
					} else {
						duplicatesMap.get(uniqueSongID).add(song.getID());
					}
					LOG.info(song.toString() + " - DUPLICATE");
				} else {
					LOG.info(song.toString());
					done.add(song);
				}
			}

			for (int unqiueId : duplicatesMap.keySet()) {
				for (int duplicateId : duplicatesMap.get(unqiueId)) {
					// 1. Update ID in the CHARTS_ tables
					String update;
					for (String chartType : Charts.TYPES) {
						update = "UPDATE charts_" + chartType
								+ " SET track_id = " + unqiueId
								+ " WHERE track_id = " + duplicateId;
						stmt.addBatch(update);
					}
					// 2. Remove duplicate from the TRACKS table
					pstmt.setInt(1, duplicateId);
					pstmt.addBatch();
					LOG.info("Deleted duplicate songs with id: " + duplicateId);
				}
				stmt.executeBatch();
				pstmt.executeBatch();
			}
			LOG.info("Deleted duplicate songs.");

		} catch (SQLException e) {
			LOG.warn("Data cleaning failed.", e);
		} finally {
			Database.quietClose(stmt);
			Database.quietClose(conn, pstmt, result);
		}
	}

	// Refresh all data relations
	public void refreshData() {
		// Refresh music charts
		CityCharts cCharts = new CityCharts();
		cCharts.saveChartsForAllCities();
		LOG.info("City charts refreshed");
		NeighborCharts nCharts = new NeighborCharts();
		nCharts.saveNeighborChartsForAllUsers();
		LOG.info("Neighbor charts refreshed");
		TagCharts tCharts = new TagCharts();
		tCharts.saveTagChartsForAllUsers();
		LOG.info("Tag charts refreshed");

		// Clean duplicate song data
		cleanSongData();

		// Fetch mbids
		refreshMbids();

		// Refresh content based recommender data
		LOG.info("Refreshing content based recommender data.");
		refreshTagRelations();
		ContentBasedRecommender.refreshTrackSimilarities();
	}
}
