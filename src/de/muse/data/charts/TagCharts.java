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
package de.muse.data.charts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muse.data.social.lastfm.LastFmConnector;
import de.muse.recommendation.MuseSong;
import de.muse.user.UserData;
import de.muse.utility.Database;

/**
 * Represents a list of songs that belong to charts of last.fm tag. Takes care
 * of getting and saving these songs.
 */
public class TagCharts extends Charts implements Runnable {
  // Configured logger
  private static final Logger LOG = LoggerFactory.getLogger(TagCharts.class
      .getName());

  // Charts for LastFM users
  private String lfmUser;
  private List<String> topTags;

  public void setLfmUser(String lfmUser) {
    this.lfmUser = lfmUser;
  }

  /**
   * Count duplicate tags in a list and return a map which pairs the tag and its
   * count.
   */
  private Map<String, Integer> countDuplicateTags(List<String> tags) {
    Map<String, Integer> tagCount = new HashMap<String, Integer>();

    for (String tag : tags) {
      int count = 0;
      for (String tagTwo : tags) {
        if (tag.equals(tagTwo)) {
          // Count the occurrences of a tag
          count++;
        }
      }
      // Make the result map unique
      if (!tagCount.containsKey(tag))
        tagCount.put(tag, count);
    }
    return tagCount;
  }

  /**
   * Maps from 10 last.fm top tracks of a user to the 3 most occurred tags in
   * these 10 tracks and returns the highest ranked songs of the 3 tags
   * 
   * @param user
   *          The Last.fm User account.
   * 
   */
  public void getChartsFor(String user) {
    // Set last.fm user account
    lfmUser = user;

    // Get list of 10 top tracks and their tag
    List<MuseSong> userTracks = LastFmConnector.getCurrentTopTracks(user, 10);
    List<String> tags = new ArrayList<String>();
    for (MuseSong song : userTracks) {
      tags.addAll(LastFmConnector.getTopTags(song.getArtist(), song.getName(), 5));
    }
    // Remove standard tags
    tags.removeAll(new ArrayList<String>(Arrays.asList("rock", "pop", "rap",
        "german", "80s", "90s", "70s", "60s")));

    // Count duplicate tags
    Map<String, Integer> tagMap = countDuplicateTags(tags);

    // Get 3 tags which occurred the most
    topTags = new ArrayList<String>();
    if (!tagMap.isEmpty()) {
      int topValue = Collections.max(tagMap.values());
      while (topTags.size() < 5) {
        for (Entry<String, Integer> entry : tagMap.entrySet()) {
          if (topTags.size() >= 5)
            break;
          int value = entry.getValue();
          if (value == topValue) {
            topTags.add(entry.getKey());
          }
        }
        topValue = topValue - 1;
      }
    }
    // Save the songs of the top tag
    List<MuseSong> songs = new ArrayList<MuseSong>();
    for (String tag : topTags) {
      // Remove equal songs
      for (MuseSong song : LastFmConnector.getTagTopTracks(tag,
          100 / (topTags.size()))) {
        if (!songs.contains(song)) {
          songs.add(song);
        }
      }
    }

    // Remove songs the users own top tracks
    songs.removeAll(LastFmConnector.getCurrentTopTracks(user, 25));
    tracks = songs;
  }

  /**
   * Save tag charts for all users. <b>Caution:</b> This will clear the
   * corresponding database table.
   */
  public void saveTagChartsForAllUsers() {
    Database.clearTable("charts_tag");
    for (String user : UserData.getAllLfmAccounts()) {
      LOG.info("Save tag charts for " + user + ".");
      getChartsFor(user);
      saveToDB();
    }
  }

  /**
   * Write songs that are currently saved in the member tracks to the database
   */
  public void saveToDB() {
    if (tracks.isEmpty())
      return;

    // Save songs to database
    Connection conn = null;
    PreparedStatement pstmt = null;
    PreparedStatement pstmt2 = null;
    ResultSet keys = null;

    try {
      conn = Database.getConnection();
      pstmt = conn.prepareStatement(
          "INSERT INTO tracks VALUES(SONGID.nextval,?,?,?)", new int[] { 1 });
      pstmt2 = conn.prepareStatement("INSERT INTO CHARTS_TAG "
          + "VALUES(?,?,?,?)");

      String tag = tracks.get(0).getTag();
      int count = 0;
      for (MuseSong track : tracks) {
        String currentTag = track.getTag();
        if (currentTag != tag) {
          tag = currentTag;
          count = 0;
        }
        pstmt.setString(1, track.getName());
        pstmt.setString(2, track.getArtist());
        pstmt.setNull(3, java.sql.Types.VARCHAR);
        pstmt.execute();
        keys = pstmt.getGeneratedKeys();
        if (keys.next()) {
          track.setID(keys.getInt(1));
        }

        pstmt2.setInt(1, count + 1);
        pstmt2.setString(2, lfmUser);
        pstmt2.setString(3, track.getTag());
        pstmt2.setInt(4, track.getID());
        pstmt2.addBatch();
        count++;
      }
      pstmt2.executeBatch();
    } catch (SQLException e) {
      LOG.warn("Couldn't save tag charts for user: " + lfmUser, e);
    } finally {
      Database.quietClose(keys);
      Database.quietClose(pstmt);
      Database.quietClose(pstmt2);
      Database.quietClose(conn);
    }
  }

  /**
   * Fetch data for a single user threaded.
   */
  @Override
  public void run() {
    getChartsFor(lfmUser);
    saveToDB();
    LOG.info("Finished fetching tag Charts: " + lfmUser);
  }
}
