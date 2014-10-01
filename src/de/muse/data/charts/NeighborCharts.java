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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muse.data.social.lastfm.LastFmConnector;
import de.muse.recommendation.MuseSong;
import de.muse.user.UserData;
import de.muse.utility.Database;

/**
 * Represents a list of songs that belong to charts of last.fm neighbors. Takes
 * care of getting and saving these songs.
 */
public class NeighborCharts extends Charts implements Runnable {
  // Configured logger
  private static final Logger LOG = LoggerFactory
      .getLogger(NeighborCharts.class.getName());

  // Last.fm account
  private String lfmUser;

  public void setLfmUser(String lfmUser) {
    this.lfmUser = lfmUser;
  }

  /**
   * Get the charts for a given last.fm user and save them to the tracks list of
   * the chart.
   * 
   * @param user
   *          Last.fm User account.
   */
  public void getChartsFor(String user) {
    // Reset the list of songs
    clear();
    this.lfmUser = user;
    List<MuseSong> neighbourSongs = new ArrayList<MuseSong>();

    // Get 5 neighbors and for every neighbor 10 top tracks
    for (String neighbour : LastFmConnector.getNeighbours(user, 5)) {
      neighbourSongs.addAll(LastFmConnector.getCurrentTopTracks(neighbour, 20));
    }

    // Remove 25 top tracks of the user itself from the list
    neighbourSongs.removeAll(LastFmConnector.getCurrentTopTracks(user, 25));
    tracks = neighbourSongs;
  }

  /**
   * Save Neighbor Charts for all lastFm Users. <b>Caution:</b> This will clear
   * the corresponding database table.
   */
  public void saveNeighborChartsForAllUsers() {
    Database.clearTable("charts_neighbor");
    for (String user : UserData.getAllLfmAccounts()) {
      LOG.info("Save neighbor charts for " + user + ".");
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

    // Save all songs to database
    Connection conn = null;
    PreparedStatement pstmt = null;
    PreparedStatement pstmt2 = null;
    ResultSet keys = null;

    try {
      conn = Database.getConnection();
      pstmt = conn.prepareStatement(
          "INSERT INTO tracks VALUES(SONGID.nextval, ?,?, ?)", new int[] { 1 });
      pstmt2 = conn.prepareStatement("INSERT INTO charts_neighbor "
          + "VALUES(?,?,?,?,?)");

      String neighbor = tracks.get(0).getNeighbor();
      int count = 0;

      // Save songs to tracks table
      for (MuseSong track : tracks) {
        pstmt.setString(1, track.getName());
        pstmt.setString(2, track.getArtist());
        pstmt.setNull(3, java.sql.Types.VARCHAR);
        pstmt.execute();
        keys = pstmt.getGeneratedKeys();
        if (keys.next()) {
          track.setID(keys.getInt(1));
        }

        if (neighbor != track.getNeighbor()) {
          neighbor = track.getNeighbor();
          count = 0;
        }
        pstmt2.setInt(1, count + 1);
        pstmt2.setInt(2, track.getPlayCount());
        pstmt2.setString(3, lfmUser);
        pstmt2.setInt(4, track.getID());
        pstmt2.setString(5, track.getNeighbor());
        pstmt2.addBatch();
        count++;
      }
      pstmt2.executeBatch();
    } catch (SQLException e) {
      LOG.warn("Couldn't save neighbor charts for user: " + lfmUser, e);
    } finally {
      Database.quietClose(keys);
      Database.quietClose(pstmt);
      Database.quietClose(pstmt2);
      Database.quietClose(conn);
    }
  }

  /**
   * Fetch data for a single user threaded
   */
  @Override
  public void run() {
    getChartsFor(lfmUser);
    saveToDB();
    LOG.info("Finished fetching neighbor Charts: " + lfmUser);
  }
}
