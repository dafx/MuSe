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
package de.mrms.evaluation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mrms.user.MuseUser;
import de.mrms.utility.Database;

/**
 * A simple group object which provides certain metrics and a list of group
 * participants.
 */
public class Group {
  private static final Logger LOG = LoggerFactory.getLogger(Group.class
      .getName());

  private final int evaluationId;
  private final int numGroup;
  private final List<Integer> recommenders;
  private final String behavior;
  private List<MuseUser> participants;

  // Group specific age facts (Initially not set, therefore -1)
  private int numYoung = -1;
  private int numMiddle = -1;
  private int numOld = -1;

  // Group specific gender facts (Initially not set, therefore -1)
  private int numMale = -1;
  private int numFemale = -1;

  // Group specific language facts
  private Map<String, Integer> langDistribution;

  /**
   * Create a simple group object.
   */
  public Group(int evaluationId, int groupId, List<Integer> recommenders,
      String behavior) {
    this.evaluationId = evaluationId;
    this.numGroup = groupId;
    this.recommenders = recommenders;
    this.behavior = behavior;
    this.participants = new ArrayList<MuseUser>();
  }

  /**
   * Get the number of "young" (0-30) participants in this group. If the value
   * is already set it will return the value, otherwise it will compute the
   * value and set it.
   */
  public int getNumYoung() {
    // Return if already set
    if (numYoung != -1)
      return numYoung;

    // Otherwise, Query database
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet result = null;

    Calendar cal = new GregorianCalendar();
    int year = cal.get(Calendar.YEAR);

    try {
      conn = Database.getConnection();
      stmt = conn.prepareStatement("SELECT count(*) as num "
          + "FROM evaluation_participants, consumer "
          + "WHERE participant = name AND group_id = ? AND eval_id = ?"
          + "AND birthyear >= ?");
      stmt.setInt(1, numGroup);
      stmt.setInt(2, evaluationId);
      stmt.setInt(3, year - 30);
      result = stmt.executeQuery();

      while (result.next()) {
        numYoung = result.getInt("num");
      }
    } catch (SQLException e) {
      LOG.warn("Couldn't get numYoung for group: " + numGroup, e);
    } finally {
      Database.quietClose(conn, stmt, result);
    }

    return numYoung;
  }

  /**
   * Get the number of "middle aged" (30-60) participants in this group. If the
   * value is already set it will return the value, otherwise it will compute
   * the value and set it.
   */
  public int getNumMiddle() {
    // Return if already set
    if (numMiddle != -1)
      return numMiddle;

    // Otherwise, Query database
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet result = null;

    Calendar cal = new GregorianCalendar();
    int year = cal.get(Calendar.YEAR);

    try {
      conn = Database.getConnection();
      stmt = conn.prepareStatement("SELECT count(*) as num "
          + "FROM evaluation_participants, consumer "
          + "WHERE participant = name AND group_id = ? AND eval_id = ?"
          + "AND birthyear > ? AND birthyear <= ?");
      stmt.setInt(1, numGroup);
      stmt.setInt(2, evaluationId);
      stmt.setInt(3, year - 60);
      stmt.setInt(4, year - 30);
      result = stmt.executeQuery();

      while (result.next()) {
        numMiddle = result.getInt("num");
      }
    } catch (SQLException e) {
      LOG.warn("Couldn't get numMiddle for group: " + numGroup, e);
    } finally {
      Database.quietClose(conn, stmt, result);
    }

    return numMiddle;
  }

  /**
   * Get the number of "old" (60+) participants in this group. If the value is
   * already set it will return the value, otherwise it will compute the value
   * and set it.
   */
  public int getNumOld() {
    // Return if already set
    if (numOld != -1)
      return numOld;

    // Otherwise, Query database
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet result = null;

    Calendar cal = new GregorianCalendar();
    int year = cal.get(Calendar.YEAR);

    try {
      conn = Database.getConnection();
      stmt = conn.prepareStatement("SELECT count(*) as num "
          + "FROM evaluation_participants, consumer "
          + "WHERE participant = name AND group_id = ? AND eval_id = ?"
          + "AND birthyear < ?");
      stmt.setInt(1, numGroup);
      stmt.setInt(2, evaluationId);
      stmt.setInt(3, year - 60);
      result = stmt.executeQuery();

      while (result.next()) {
        numOld = result.getInt("num");
      }
    } catch (SQLException e) {
      LOG.warn("Couldn't get numOld for group: " + numGroup, e);
    } finally {
      Database.quietClose(conn, stmt, result);
    }

    return numOld;
  }

  /**
   * Get the number of male participants in this group. If the value is already
   * set it will return the value, otherwise it will compute the value and set
   * it.
   */
  public int getNumMale() {
    // Return if already set
    if (numMale != -1)
      return numMale;

    // Otherwise, Query database
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet result = null;

    try {
      conn = Database.getConnection();
      stmt = conn.prepareStatement("SELECT count(*) as num "
          + "FROM evaluation_participants, consumer "
          + "WHERE participant = name AND group_id = ? AND eval_id = ?"
          + "AND sex = ?");
      stmt.setInt(1, numGroup);
      stmt.setInt(2, evaluationId);
      stmt.setString(3, "Male");
      result = stmt.executeQuery();

      while (result.next()) {
        numMale = result.getInt("num");
      }
    } catch (SQLException e) {
      LOG.warn("Couldn't get numMale for group: " + numGroup, e);
    } finally {
      Database.quietClose(conn, stmt, result);
    }

    return numMale;
  }

  /**
   * Get the number of female participants in this group. If the value is
   * already set it will return the value, otherwise it will compute the value
   * and set it.
   */
  public int getNumFemale() {
    // Return if already set
    if (numFemale != -1)
      return numFemale;

    // Otherwise, Query database
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet result = null;

    try {
      conn = Database.getConnection();
      stmt = conn.prepareStatement("SELECT count(*) as num "
          + "FROM evaluation_participants, consumer "
          + "WHERE participant = name AND group_id = ? AND eval_id = ?"
          + "AND sex = ?");
      stmt.setInt(1, numGroup);
      stmt.setInt(2, evaluationId);
      stmt.setString(3, "Female");
      result = stmt.executeQuery();

      while (result.next()) {
        numFemale = result.getInt("num");
      }
    } catch (SQLException e) {
      LOG.warn("Couldn't get numFemale for group: " + numGroup, e);
    } finally {
      Database.quietClose(conn, stmt, result);
    }

    return numFemale;
  }

  /**
   * Get the number of language distribution of the participants in this group.
   * If the value is already set it will return the value, otherwise it will
   * compute the value and set it.
   */
  public Map<String, Integer> getLangDistribution() {
    // Return if already set
    if (langDistribution != null)
      return langDistribution;

    // Otherwise, Query database
    HashMap<String, Integer> langDist = new HashMap<String, Integer>();
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet result = null;

    try {
      conn = Database.getConnection();
      stmt = conn
          .prepareStatement("SELECT language, count(*) as num "
              + "FROM evaluation_participants, consumer_language "
              + "WHERE participant = consumer_name AND group_id = ? AND eval_id = ?"
              + "GROUP BY language");
      stmt.setInt(1, numGroup);
      stmt.setInt(2, evaluationId);
      result = stmt.executeQuery();

      while (result.next()) {
        String lang = result.getString("language");
        int num = result.getInt("num");
        langDist.put(lang, num);
      }

      langDistribution = langDist;
    } catch (SQLException e) {
      LOG.warn("Couldn't get language Distribution for group: " + numGroup, e);
    } finally {
      Database.quietClose(conn, stmt, result);
    }

    return langDistribution;
  }

  /**
   * Add the given user object to the participants list.
   * 
   * @param participant
   *          Participant as user object.
   */
  public void addParticipant(MuseUser participant) {
    participants.add(participant);
  }

  /**
   * Get the list of participants.
   * 
   * @return List of participants as list of user objects.
   */
  public List<MuseUser> getParticipants() {
    return participants;
  }

  /**
   * @param numYoung
   *          the numYoung to set
   */
  public void setNumYoung(int numYoung) {
    this.numYoung = numYoung;
  }

  /**
   * @param numMiddle
   *          the numMiddle to set
   */
  public void setNumMiddle(int numMiddle) {
    this.numMiddle = numMiddle;
  }

  /**
   * @param numOld
   *          the numOld to set
   */
  public void setNumOld(int numOld) {
    this.numOld = numOld;
  }

  /**
   * @param numMale
   *          the numMale to set
   */
  public void setNumMale(int numMale) {
    this.numMale = numMale;
  }

  /**
   * @param numFemale
   *          the numFemale to set
   */
  public void setNumFemale(int numFemale) {
    this.numFemale = numFemale;
  }

  /**
   * @return the numGroup
   */
  public int getNumGroup() {
    return numGroup;
  }
  
  /**
   * @return the numGroup
   */
  public int getEvaluationId() {
    return evaluationId;
  }
  

  /**
   * @return the recommenders
   */
  public List<Integer> getRecommenders() {
    return recommenders;
  }

  /**
   * @return the behavior
   */
  public String getBehavior() {
    return behavior;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + evaluationId;
    result = prime * result + numGroup;
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Group other = (Group) obj;
    if (evaluationId != other.evaluationId)
      return false;
    if (numGroup != other.numGroup)
      return false;
    return true;
  }

}
