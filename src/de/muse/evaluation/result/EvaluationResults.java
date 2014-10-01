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
package de.muse.evaluation.result;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muse.evaluation.Statistics;
import de.muse.utility.Database;
import de.muse.utility.MathHelper;

public class EvaluationResults {
  // Configured logger
  private static final Logger LOG = LoggerFactory.getLogger(Statistics.class
      .getName());
  int evalId;

  // Wrapper members for sending the client
  HashMap<Integer, TreeMap<String, Integer>> groupJoins;
  HashMap<Integer, TreeMap<String, Integer>> groupVisits;
  HashMap<Integer, TreeMap<String, Integer>> groupRatings;
  HashMap<Integer, TreeMap<Integer, Integer>> groupAgeDist;
  HashMap<Integer, HashMap<String, Integer>> groupGenderDist;
  HashMap<Integer, HashMap<Integer, Double>> meanAbsoluteErrors;
  HashMap<Integer, Double> avgGroupRatings;
  HashMap<Integer, HashMap<Integer, Double>> avgRatingsPerRecommender;
  HashMap<Integer, HashMap<Integer, Double>> groupListAvg;
  HashMap<Integer, HashMap<Integer, Double>> groupAccuracy;

  /**
   * Create an evaluationResults object for a given evaluation (id).
   * 
   * @param evalId
   *          Id of the wanted evaluation.
   */
  public EvaluationResults(int evalId) {
    this.evalId = evalId;
  }

  /**
   * Get the age distribution of all groups in the evaluation.
   */
  public HashMap<Integer, TreeMap<Integer, Integer>> getAgeDist() {
    HashMap<Integer, TreeMap<Integer, Integer>> groupAgeDist = new HashMap<Integer, TreeMap<Integer, Integer>>();

    // Connect to database
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet result = null;

    try {
      conn = Database.getConnection();
      stmt = conn.prepareStatement("SELECT ep.group_id, c.birthyear, "
          + "COUNT(c.birthyear) as numAge FROM consumer c "
          + "JOIN evaluation_participants ep ON c.name = ep.participant "
          + "WHERE ep.eval_id = ? GROUP BY(ep.group_id, c.birthyear)");
      stmt.setInt(1, evalId);

      // Query database
      result = stmt.executeQuery();
      while (result.next()) {
        int group = result.getInt("group_id");
        // Insert group if no entry yet
        if (!groupAgeDist.containsKey(group)) {
          groupAgeDist.put(group, new TreeMap<Integer, Integer>());
        }

        // Insert value for current recommender and group
        groupAgeDist.get(group).put(result.getInt("birthyear"),
            result.getInt("numAge"));
      }

    } catch (SQLException e) {
      LOG.warn("Couldn't get age distribution of evaluation groups.", e);
    } finally {
      Database.quietClose(conn, stmt, result);
    }

    this.groupAgeDist = groupAgeDist;
    return this.groupAgeDist;
  }

  /**
   * Get the gender distribution of all groups in the evaluation.
   */
  public HashMap<Integer, HashMap<String, Integer>> getGenderDist() {
    HashMap<Integer, HashMap<String, Integer>> groupGenderDist = new HashMap<Integer, HashMap<String, Integer>>();

    // Connect to database
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet result = null;

    try {
      conn = Database.getConnection();
      stmt = conn.prepareStatement("SELECT ep.group_id, c.sex, "
          + "COUNT(c.sex) as numGender FROM consumer c "
          + "JOIN evaluation_participants ep ON c.name = ep.participant "
          + "WHERE ep.eval_id = ? GROUP BY(ep.group_id, c.sex)");
      stmt.setInt(1, evalId);

      // Query database
      result = stmt.executeQuery();
      while (result.next()) {
        int group = result.getInt("group_id");
        // Insert group if no entry yet
        if (!groupGenderDist.containsKey(group)) {
          groupGenderDist.put(group, new HashMap<String, Integer>());
        }

        // Insert value for current recommender and group
        groupGenderDist.get(group).put(result.getString("sex"),
            result.getInt("numGender"));
      }

    } catch (SQLException e) {
      LOG.warn("Couldn't get gender distribution of evaluation groups.", e);
    } finally {
      Database.quietClose(conn, stmt, result);
    }

    this.groupGenderDist = groupGenderDist;
    return this.groupGenderDist;
  }

  /**
   * Get the evolution of participant numbers of each group.
   */
  public HashMap<Integer, TreeMap<String, Integer>> getGroupJoins() {
    HashMap<Integer, TreeMap<String, Integer>> groupJoins = new HashMap<Integer, TreeMap<String, Integer>>();
    TreeMap<String, Integer> defaultJoins = new TreeMap<String, Integer>();

    // Connect to database
    Connection conn = null;
    PreparedStatement pstmt = null;
    PreparedStatement pstmt2 = null;
    ResultSet result = null;

    try {
      conn = Database.getConnection();
      pstmt = conn.prepareStatement("SELECT start_date, end_date "
          + "FROM evaluation WHERE id = ?");
      pstmt.setInt(1, this.evalId);

      // 1) Get the start and end date of the evaluation
      result = pstmt.executeQuery();
      Calendar start = new GregorianCalendar();
      Calendar end = new GregorianCalendar();

      if (result.next()) {
        // Set calendar to the start and end date
        start.setTime(result.getDate("start_date"));
        if (end.getTime().compareTo(result.getDate("end_date")) > 0) {
          end.setTime(result.getDate("end_date"));
        }

        // Insert zero values as default for all dates
        SimpleDateFormat JSformatter = new SimpleDateFormat("yyyy/MM/dd");
        while (start.compareTo(end) <= 0) {
          defaultJoins.put(JSformatter.format(start.getTime()), 0);
          start.add(Calendar.DAY_OF_MONTH, 1);
        }

        // 2) Query visit data for each group
        pstmt2 = conn.prepareStatement("SELECT ep.group_id, join_date as day, "
            + "(count(join_date)- count(quit_date)) as joined "
            + "FROM evaluation_participants ep WHERE ep.eval_id = ? "
            + "GROUP BY(ep.group_id, join_date)");
        pstmt2.setInt(1, evalId);

        // Query database
        result = pstmt2.executeQuery();
        while (result.next()) {
          int group = result.getInt("group_id");
          if (!groupJoins.containsKey(group)) {
            TreeMap<String, Integer> groupMap = new TreeMap<String, Integer>(
                defaultJoins);
            groupJoins.put(group, groupMap);
          }
          String date = JSformatter.format(result.getDate("day"));
          groupJoins.get(group).put(date, result.getInt("joined"));
        }
      }
    } catch (SQLException e) {
      LOG.warn("Couldn't get participant joins of evaluation groups.", e);
    } finally {
      Database.quietClose(pstmt);
      Database.quietClose(conn, pstmt2, result);
    }

    this.groupJoins = groupJoins;
    return this.groupJoins;
  }

  /**
   * Get the visits of each participant group over the course of the evaluation.
   */
  public HashMap<Integer, TreeMap<String, Integer>> getGroupVisits() {
    HashMap<Integer, TreeMap<String, Integer>> groupVisits = new HashMap<Integer, TreeMap<String, Integer>>();
    TreeMap<String, Integer> defaultVisits = new TreeMap<String, Integer>();

    // Connect to database
    Connection conn = null;
    PreparedStatement pstmt = null;
    PreparedStatement pstmt2 = null;
    ResultSet result = null;

    try {
      conn = Database.getConnection();
      pstmt = conn.prepareStatement("SELECT start_date, end_date "
          + "FROM evaluation WHERE id = ?");
      pstmt.setInt(1, this.evalId);

      // 1) Get the start and end date of the evaluation
      result = pstmt.executeQuery();
      Calendar start = new GregorianCalendar();
      Calendar end = new GregorianCalendar();
      Date startDate;
      Date endDate;

      if (result.next()) {
        // Set calendar to the start and end date
        startDate = result.getDate("start_date");
        endDate = result.getDate("end_date");
        start.setTime(result.getDate("start_date"));
        if (end.getTime().compareTo(endDate) > 0) {
          end.setTime(result.getDate("end_date"));
        }

        // Insert zero values as default for all dates
        SimpleDateFormat JSformatter = new SimpleDateFormat("yyyy/MM/dd");
        while (start.compareTo(end) <= 0) {
          defaultVisits.put(JSformatter.format(start.getTime()), 0);
          start.add(Calendar.DAY_OF_MONTH, 1);
        }

        // 2) Query visit data for each group
        pstmt2 = conn
            .prepareStatement("SELECT ep.group_id, trunc(a.day) as day,"
                + " count(a.consumer_name) as numUsers "
                + "FROM evaluation_participants ep "
                + "JOIN login_activities a on ep.participant = a.consumer_name "
                + "WHERE ep.eval_id = ? AND trunc(a.day) " + "BETWEEN ? AND ? "
                + "GROUP BY(ep.group_id, trunc(a.day))");
        pstmt2.setInt(1, evalId);
        pstmt2.setDate(2, startDate);
        pstmt2.setDate(3, endDate);

        // Query database
        result = pstmt2.executeQuery();
        while (result.next()) {
          int group = result.getInt("group_id");
          if (!groupVisits.containsKey(group)) {
            TreeMap<String, Integer> groupMap = new TreeMap<String, Integer>(
                defaultVisits);
            groupVisits.put(group, groupMap);
          }
          String date = JSformatter.format(result.getDate("day"));
          groupVisits.get(group).put(date, result.getInt("numUsers"));
        }
      }
    } catch (SQLException e) {
      LOG.warn("Couldn't get visits of evaluation groups.", e);
    } finally {
      Database.quietClose(pstmt);
      Database.quietClose(conn, pstmt2, result);
    }

    this.groupVisits = groupVisits;
    return this.groupVisits;
  }

  /**
   * Get the ratings of each participant group over the course of the evaluation.
   */
  public HashMap<Integer, TreeMap<String, Integer>> getGroupRatings() {
    HashMap<Integer, TreeMap<String, Integer>> groupRatings = new HashMap<Integer, TreeMap<String, Integer>>();
    TreeMap<String, Integer> defaultRatings = new TreeMap<String, Integer>();

    // Connect to database
    Connection conn = null;
    PreparedStatement pstmt = null;
    PreparedStatement pstmt2 = null;
    ResultSet result = null;

    try {
      conn = Database.getConnection();
      pstmt = conn.prepareStatement("SELECT start_date, end_date "
          + "FROM evaluation WHERE id = ?");
      pstmt.setInt(1, this.evalId);

      // 1) Get the start and end date of the evaluation
      result = pstmt.executeQuery();
      Calendar start = new GregorianCalendar();
      Calendar end = new GregorianCalendar();

      if (result.next()) {
        // Set calendar to the start and end date
        start.setTime(result.getDate("start_date"));
        Date endDate = result.getDate("end_date");
        if (end.getTime().compareTo(endDate) > 0) {
          end.setTime(result.getDate("end_date"));
        }

        // Insert zero values as default for all dates
        SimpleDateFormat JSformatter = new SimpleDateFormat("yyyy/MM/dd");
        while (start.compareTo(end) <= 0) {
          defaultRatings.put(JSformatter.format(start.getTime()), 0);
          start.add(Calendar.DAY_OF_MONTH, 1);
        }

        // 2) Query visit data for each group
        pstmt2 = conn
            .prepareStatement("SELECT ep.group_id, r.day, count(r.id) as numRatings "
                + "FROM evaluation_participants ep JOIN "
                + "(SELECT consumer, id, eval_id, trunc(time) as day "
                + "FROM recommendation WHERE rating != 0 AND eval_id = ?) r "
                + "on ep.participant = r.consumer GROUP BY(ep.group_id, r.day)");
        pstmt2.setInt(1, evalId);

        // Query database
        result = pstmt2.executeQuery();
        while (result.next()) {
          int group = result.getInt("group_id");
          if (!groupRatings.containsKey(group)) {
            TreeMap<String, Integer> groupMap = new TreeMap<String, Integer>(
                defaultRatings);
            groupRatings.put(group, groupMap);
          }
          String date = JSformatter.format(result.getDate("day"));
          groupRatings.get(group).put(date, result.getInt("numRatings"));
        }
      }
    } catch (SQLException e) {
      LOG.warn("Couldn't get visits of evaluation groups.", e);
    } finally {
      Database.quietClose(pstmt);
      Database.quietClose(conn, pstmt2, result);
    }

    this.groupRatings = groupRatings;
    return this.groupRatings;
  }

  /**
   * Get the mean absolute error values for all recommenders in the system.
   */
  public HashMap<Integer, HashMap<Integer, Double>> getMeanAbsoluteErrors() {
    HashMap<Integer, HashMap<Integer, Double>> meanAbsoluteErrors = new HashMap<Integer, HashMap<Integer, Double>>();

    // Connect to database
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet result = null;

    try {
      conn = Database.getConnection();
      stmt = conn.prepareStatement("SELECT ep.group_id, r.recommender_id, "
          + "AVG(abs(r.rating - r.score)) as MAE "
          + "FROM Recommendation r JOIN evaluation_participants ep "
          + "ON r.consumer = ep.participant WHERE r.rating != 0 "
          + "AND r.eval_id = ? GROUP BY(r.recommender_id, ep.group_id)");
      stmt.setInt(1, evalId);

      // Query database
      result = stmt.executeQuery();
      while (result.next()) {
        int group = result.getInt("group_id");
        // Insert group if no entry yet
        if (!meanAbsoluteErrors.containsKey(group)) {
          meanAbsoluteErrors.put(group, new HashMap<Integer, Double>());
        }

        // Insert value for current recommender and group
        meanAbsoluteErrors.get(group).put(result.getInt("recommender_id"),
            result.getDouble("MAE"));
      }

    } catch (SQLException e) {
      LOG.warn("Couldn't get mean absolute errors of evaluation groups.", e);
    } finally {
      Database.quietClose(conn, stmt, result);
    }

    this.meanAbsoluteErrors = meanAbsoluteErrors;
    return this.meanAbsoluteErrors;
  }

  /**
   * Get the average ratings for all recommenders for each group.
   */
  public HashMap<Integer, HashMap<Integer, Double>> getAvgRatingsPerRecommender() {
    HashMap<Integer, HashMap<Integer, Double>> avgRatingsPerRecommender = new HashMap<Integer, HashMap<Integer, Double>>();

    // Connect to database
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet result = null;

    try {
      conn = Database.getConnection();
      stmt = conn.prepareStatement("SELECT ep.group_id, r.recommender_id, "
          + "AVG(r.rating) as avgRating "
          + "FROM Recommendation r JOIN evaluation_participants ep "
          + "ON r.consumer = ep.participant WHERE r.rating != 0 "
          + "AND r.eval_id = ? GROUP BY(r.recommender_id, ep.group_id)");
      stmt.setInt(1, evalId);

      // Query database
      result = stmt.executeQuery();
      while (result.next()) {
        int group = result.getInt("group_id");
        // Insert group if no entry yet
        if (!avgRatingsPerRecommender.containsKey(group)) {
          avgRatingsPerRecommender.put(group, new HashMap<Integer, Double>());
        }

        // Insert value for current recommender and group
        avgRatingsPerRecommender.get(group).put(
            result.getInt("recommender_id"), result.getDouble("avgRating"));
      }

    } catch (SQLException e) {
      LOG.warn(
          "Couldn't get avg ratings per recommender of evaluation groups.", e);
    } finally {
      Database.quietClose(conn, stmt, result);
    }

    this.avgRatingsPerRecommender = avgRatingsPerRecommender;
    return this.avgRatingsPerRecommender;
  }

  /**
   * Get the average ratings for all recommenders for each group.
   */
  public HashMap<Integer, Double> getAvgGroupRatings() {
    HashMap<Integer, Double> avgGroupRatings = new HashMap<Integer, Double>();

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

      // Query database
      result = stmt.executeQuery();
      while (result.next()) {
        // Insert value for current recommender and group
        avgGroupRatings.put(result.getInt("group_id"),
            result.getDouble("avgRating"));
      }

    } catch (SQLException e) {
      LOG.warn("Couldn't get avg group ratings of evaluation groups.", e);
    } finally {
      Database.quietClose(conn, stmt, result);
    }

    this.avgGroupRatings = avgGroupRatings;
    return this.avgGroupRatings;
  }

  /**
   * Get the group list ratings.
   */
  public HashMap<Integer, HashMap<Integer, Double>> getGroupListRatings() {
    HashMap<Integer, HashMap<Integer, Double>> groupListAvg = new HashMap<Integer, HashMap<Integer, Double>>();

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

      // Query database
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
      for (Integer group : groupListRatings.keySet()) {
        for (Integer list : groupListRatings.get(group).keySet()) {
          ArrayList<Integer> ratings = groupListRatings.get(group).get(list);

          Double avg = (double) (MathHelper.getSumOfInts(groupListRatings.get(
              group).get(list)) / ratings.size());
          if (!groupListAvg.containsKey(group)) {
            groupListAvg.put(group, new HashMap<Integer, Double>());
          }
          groupListAvg.get(group).put(list, avg);
        }
      }

    } catch (SQLException e) {
      LOG.warn("Couldn't get avg list ratings of evaluation groups.", e);
    } finally {
      Database.quietClose(conn, stmt, result);
    }

    this.groupListAvg = groupListAvg;
    return this.groupListAvg;
  }

  /**
   * Get the group accuracy values for each recommender
   */
  public HashMap<Integer, HashMap<Integer, Double>> getGroupAccuarcy()
      throws SQLException {
    HashMap<Integer, HashMap<Integer, Double>> groupAccuracy = new HashMap<Integer, HashMap<Integer, Double>>();

    // Connect to database
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet result = null;

    try {
      conn = Database.getConnection();
      stmt = conn.prepareStatement("SELECT ep.group_id, "
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

      stmt = conn.prepareStatement("SELECT ep.group_id, "
          + "r.recommender_id, COUNT(r.rating) as num "
          + "FROM recommendation r JOIN evaluation_participants ep "
          + "ON r.consumer = ep.participant WHERE r.rating != 0 "
          + "AND r.eval_id = ? GROUP BY(ep.group_id, r.recommender_id)");
      stmt.setInt(1, evalId);

      // Query database for true
      result = stmt.executeQuery();
      while (result.next()) {
        int group = result.getInt("group_id");
        int recommender = result.getInt("recommender_id");
        int count = result.getInt("num");

        // Compute accuracy values and save them to the result map
        int tp = 0;
        if (truePositives.containsKey(group)
            && truePositives.get(group).containsKey(recommender)) {
          tp = truePositives.get(group).get(recommender);
        }

        // First time seeing this group
        if (!groupAccuracy.containsKey(group)) {
          groupAccuracy.put(group, new HashMap<Integer, Double>());
        }

        double accuracy = (count == 0) ? 0 : (double) tp / (double) count;
        groupAccuracy.get(group).put(recommender, accuracy);
      }

    } finally {
      Database.quietClose(conn, stmt, result);
    }

    this.groupAccuracy = groupAccuracy;
    return this.groupAccuracy;
  }
}
