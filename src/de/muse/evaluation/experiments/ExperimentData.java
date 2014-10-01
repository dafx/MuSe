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
package de.muse.evaluation.experiments;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import de.muse.api.Recommendation;
import de.muse.recommendation.MuseRecommendation;
import de.muse.user.MuseUser;
import de.muse.utility.Database;

/**
 * Bypass transaction based standard user model for fast and easy access. Should
 * be used for the sake of experiments only.
 */
public class ExperimentData {
  protected static void putUsers(List<MuseUser> users) throws SQLException {
    // Connect to database
    Connection conn = null;
    PreparedStatement pstmt = null;
    PreparedStatement pstmt2 = null;

    try {
      conn = Database.getConnection();
      // Start transaction
      pstmt = conn
          .prepareStatement("INSERT INTO consumer VALUES(?,'none',?,?,'none',1, 'test', 'none', CURRENT_TIMESTAMP, 'Y', NULL)");
      pstmt2 = conn
          .prepareStatement("INSERT INTO consumer_language VALUES(?,?)");
      for (MuseUser user : users) {
        pstmt.setString(1, user.getName());
        pstmt.setInt(2, user.getBirthyear());
        pstmt.setString(3, user.getSex());
        pstmt.execute();
        for (String lang : user.getLangs()) {
          pstmt2.setString(1, user.getName());
          pstmt2.setString(2, lang);
          pstmt2.execute();
        }
      }
    } finally {
      Database.quietClose(pstmt2);
      Database.quietClose(pstmt);
      Database.quietClose(conn);
    }
  }

  protected static void putRatingList(String name, int evalId,
      List<MuseRecommendation> recs, int listRating, long startDate, long endDate)
      throws SQLException {
    // Connect to database
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet result = null;

    try {
      conn = Database.getConnection();
      // Start transaction

      // Get the list value
      pstmt = conn.prepareStatement("SELECT list FROM consumer WHERE name='"
          + name + "'");
      result = pstmt.executeQuery();
      int list = 0;
      while (result.next()) {
        list = result.getInt("list");
      }

      // Save list to db
      pstmt = conn
          .prepareStatement("INSERT INTO recommendation VALUES(RECID.nextval,  ?, ?, ?, ?, ?, NULL, ?, ?, ?)");
      // Save list of recommendations
      Date listDate = RandomDataProvider.getRandomDate(startDate, endDate);
      for (Recommendation rec : recs) {
        pstmt.setDate(1, listDate);
        pstmt.setString(2, name);
        pstmt.setInt(3, Integer.parseInt(rec.getExplanation()));
        pstmt.setInt(4, list + 1);
        pstmt.setInt(5, rec.getRecommenderID());
        pstmt.setDouble(6, rec.getScore());
        pstmt.setInt(7, rec.getSong().getID());
        pstmt.setInt(8, evalId);
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
          .prepareStatement("INSERT INTO recommendation_list VALUES(?,?,?,?)");
      pstmt.setString(1, name);
      pstmt.setInt(2, list);
      pstmt.setInt(3, listRating);
      pstmt.setDate(4, listDate);
      pstmt.execute();

    } finally {
      Database.quietClose(conn, pstmt, result);
    }
  }

  public static List<Integer> getAllTrackIds() throws SQLException {
    List<Integer> tracks = new ArrayList<Integer>();

    // Connect to database
    Connection conn = null;
    Statement stmt = null;
    ResultSet result = null;

    try {
      conn = Database.getConnection();
      stmt = conn.createStatement();
      result = stmt.executeQuery("SELECT id FROM tracks");

      // Get all the track ids from the database
      while (result.next()) {
        tracks.add(result.getInt("id"));
      }
    } finally {
      Database.quietClose(conn, stmt, result);
    }
    return tracks;
  }
}
