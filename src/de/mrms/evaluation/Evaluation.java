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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.mrms.user.MuseUser;
import de.mrms.user.UserData;
import de.mrms.utility.Database;

/**
 * An Evaluation class containing information about settings, duration, groups,
 * participants and of an evaluation.
 */
public class Evaluation {
	private static transient final Logger LOG = LoggerFactory
			.getLogger(Evaluation.class.getName());

	private String name;
	private int evaluationId;
	private String creator;
	private List<String> composition;
	private Date from;
	private Date to;
	private Date created;
	private List<Group> groups;

	/**
	 * Create an empty Evaluation object.
	 */
	public Evaluation() {
	}

	/**
	 * Create an Evaluation object.
	 */
	public Evaluation(String name, int evaluationId, List<String> composition,
			Date from, Date to) {
		this.evaluationId = evaluationId;
		this.composition = composition;
		this.from = from;
		this.to = to;
	}

	public Evaluation(String name, int evaluationId, String creator,
			List<String> composition, Date from, Date to, Date created) {
		this.name = name;
		this.evaluationId = evaluationId;
		this.creator = creator;
		this.composition = composition;
		this.from = from;
		this.to = to;
		this.created = created;
	}

	/**
	 * @return The end date of the evaluation.
	 */
	public Date getTo() {
		return to;
	}

	/**
	 * @return The id of the evaluation.
	 */
	public int getId() {
		return evaluationId;
	}
	
	/**
	 * @return The groups of the evaluation.
	 */
	public List<Group> getGroups() {
		return groups;
	}
	
	/**
	 * Set the groups
	 */
	public void setGroups(List<Group> groups) {
		this.groups = groups;
	}

	/**
	 * Save evaluation metadata to the database and thereby initiate the
	 * evaluation. It will then automatically be executed from the start to the
	 * end date.
	 */
	public void saveToDatabase() {
		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = Database.getConnection();
			pstmt = conn
					.prepareStatement("INSERT INTO Evaluation VALUES(EVALID.nextval,?,?,?,?,?,?)");
			pstmt.setString(1, creator);
			pstmt.setDate(2, new java.sql.Date(from.getTime()));
			pstmt.setDate(3, new java.sql.Date(to.getTime()));
			pstmt.setString(4, new Gson().toJson(composition));
			pstmt.setDate(5, new java.sql.Date(created.getTime()));
			pstmt.setString(6, name);
			pstmt.execute();

			pstmt = conn
					.prepareStatement("INSERT INTO Evaluation_Groups VALUES(EVALID.currval,?,?,?)");
			for (Group group : groups) {
				pstmt.setInt(1, group.getNumGroup());
				pstmt.setString(2, group.getBehavior());
				pstmt.setString(3, new Gson().toJson(group.getRecommenders()));
				pstmt.execute();
			}

		} catch (SQLException e) {
			LOG.warn("Inserting Evaluation into DB failed.", e);
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}
	}

	public void adaptSettings() throws SQLException {
		// Connect to database
		Connection conn = null;
		PreparedStatement pstmt = null;

		try {
			conn = Database.getConnection();
			if (name != null) {
				pstmt = conn
						.prepareStatement("UPDATE evaluation SET name = ? WHERE id=?");
				pstmt.setString(1, name);
				pstmt.setInt(2, evaluationId);
				pstmt.execute();
			}
			if (from != null) {
				pstmt = conn
						.prepareStatement("UPDATE evaluation SET start_date = ? WHERE id=?");
				pstmt.setDate(1, new java.sql.Date(from.getTime()));
				pstmt.setInt(2, evaluationId);
				pstmt.execute();
			}
			if (to != null) {
				pstmt = conn
						.prepareStatement("UPDATE evaluation SET end_date = ? WHERE id=?");
				pstmt.setDate(1, new java.sql.Date(to.getTime()));
				pstmt.setInt(2, evaluationId);
				pstmt.execute();
			}
			if (composition != null && !composition.isEmpty()) {
				pstmt = conn
						.prepareStatement("UPDATE evaluation SET composition = ? WHERE id=?");
				pstmt.setString(1, new Gson().toJson(composition));
				pstmt.setInt(2, evaluationId);
				pstmt.execute();
			}
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn);
		}
	}

	/**
	 * Match user to a group of the evaluation in order to realize an uniform
	 * distribution.
	 * 
	 * <br>
	 * </br> <b> Soubld be invoked after running {@link #getRunning()} .</b>
	 * 
	 * @param participant
	 *            The user object to be matched.
	 * @return The number of the group the user was matched to in the current
	 *         Evaluation. -1 if no Evaluation is running.
	 */
	public int matchUserToGroup(String participant) {
		// 1. Get information about the user to be matched
		MuseUser user = new MuseUser(participant);
		String ageGroup = UserData.getAgeGroup(user.getBirthyear());
		String gender = user.getSex();
		List<String> langs = user.getLangs();

		// 3. Check metrics for each of the groups with the user
		List<Integer> results = new ArrayList<Integer>();
		for (Group group : groups) {
			int numYoung = group.getNumYoung();
			int numMiddle = group.getNumMiddle();
			int numOld = group.getNumOld();
			int numMale = group.getNumMale();
			int numFemale = group.getNumFemale();
			Map<String, Integer> langDistribution = new HashMap<String, Integer>(
					group.getLangDistribution());

			// Add age info to group metrics
			if (composition.contains("age")) {
				if (ageGroup.equals("young")) {
					numYoung++;
				} else if (ageGroup.equals("middle")) {
					numMiddle++;
				} else {
					numOld++;
				}
			}

			// Add gender info to group metrics
			if (composition.contains("gender")) {
				if (gender.equals("Male")) {
					numMale++;
				} else {
					numFemale++;
				}
			}

			// Add lang info to group metrics
			if (composition.contains("lang")) {
				for (String lang : langs) {
					if (langDistribution.containsKey(lang)) {
						int numLang = langDistribution.get(lang);
						langDistribution.put(lang, numLang + 1);
					} else {
						langDistribution.put(lang, 1);
					}
				}
			}

			// Compute difference to each other group
			int maxDiff = 0;
			for (Group group2 : groups) {
				int diff = 0;
				if (group2.equals(group))
					continue;

				// Age difference
				if (composition.contains("age")) {
					int ageDiff = 0;
					ageDiff = Math.abs(numYoung - group2.getNumYoung())
							+ Math.abs(numMiddle - group2.getNumMiddle())
							+ Math.abs(numOld - group2.getNumOld());
					diff += ageDiff;
				}

				// Gender difference
				if (composition.contains("gender")) {
					int genderDiff = 0;
					genderDiff = Math.abs(numMale - group2.getNumMale())
							+ Math.abs(numFemale - group2.getNumFemale());
					diff += genderDiff;
				}

				// Lang difference
				if (composition.contains("lang")) {
					int langDiff = 0;
					for (String lang : langDistribution.keySet()) {
						int numCurrentLang = langDistribution.get(lang);
						if (group2.getLangDistribution().containsKey(lang)) {
							langDiff += Math.abs(numCurrentLang
									- group2.getLangDistribution().get(lang));
						} else {
							langDiff += numCurrentLang;
						}
					}
					diff += langDiff;
				}

				// Keep track of the maximum difference
				if (diff > maxDiff)
					maxDiff = diff;
			}
			results.add(maxDiff);
		}

		// 4. Match user to the group such that the max. difference of the
		// groups
		// will be minimized.
		int min = Collections.min(results);
		int matchedGroup = results.indexOf(min) + 1;
		LOG.info("Matched user: " + participant + " to group #" + matchedGroup);
		return matchedGroup;
	}

	/**
	 * Add a user to a certain group of the running evaluation.
	 * 
	 * @param groupNum
	 *            The number of the group.
	 * @param userName
	 *            The name of the user.
	 */
	public void addParticipant(int groupNum, String userName)
			throws SQLException {
		// Otherwise, Query database
		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = Database.getConnection();
			stmt = conn
					.prepareStatement("INSERT INTO evaluation_participants VALUES(?,?,?, CURRENT_TIMESTAMP, NULL)");
			stmt.setInt(1, evaluationId);
			stmt.setInt(2, groupNum);
			stmt.setString(3, userName);
			stmt.execute();
		} finally {
			Database.quietClose(stmt);
			Database.quietClose(conn);
		}
	}

	/**
	 * User quitting from the evaluation.
	 * 
	 * @param userName
	 *            The name of the user.
	 */
	public void quit(String userName) throws SQLException {
		// Otherwise, Query database
		Connection conn = null;
		PreparedStatement stmt = null;

		try {
			conn = Database.getConnection();
			// Start transaction
			conn.setAutoCommit(false);

			stmt = conn.prepareStatement("UPDATE evaluation_participants "
					+ "SET quit_date = CURRENT_TIMESTAMP "
					+ "WHERE eval_id = ? AND participant = ?");
			stmt.setInt(1, evaluationId);
			stmt.setString(2, userName);
			stmt.execute();

			stmt = conn.prepareStatement("UPDATE consumer "
					+ "SET eval_participant = 'N', newcomer = 'N' "
					+ "WHERE name = ?");
			stmt.setString(1, userName);
			stmt.execute();

			// Commit transaction
			conn.commit();
		} catch (SQLException e) {
			LOG.warn("- Rollback - Removing participant " + userName
					+ " failed.", e);
			Database.quietRollback(conn);
			throw new SQLException();
		} finally {
			Database.resetAutoCommit(conn);
			Database.quietClose(stmt);
			Database.quietClose(conn);
		}
	}

	/**
	 * Match a list of users to groups of the running evaluation in order to
	 * realize a uniform distribution.
	 * 
	 * <br>
	 * </br> <b> Should be invoked after running {@link #getRunning()} .</b>
	 * 
	 * @param participantNames
	 *            List of names of users to be matched.
	 * @return Map that matches user names to group numbers.
	 */
	public Map<String, Integer> matchUserToGroup(List<String> participantNames) {
		Map<String, Integer> matchedUsers = new HashMap<String, Integer>();

		for (String participant : participantNames) {
			// 1. Get information about the user to be matched
			MuseUser user = new MuseUser(participant);
			String ageGroup = UserData.getAgeGroup(user.getBirthyear());
			String gender = user.getSex();
			List<String> langs = user.getLangs();

			// 3. Check metrics for each of the groups with the user
			List<Integer> results = new ArrayList<Integer>();
			for (Group group : groups) {
				int numYoung = group.getNumYoung();
				int numMiddle = group.getNumMiddle();
				int numOld = group.getNumOld();
				int numMale = group.getNumMale();
				int numFemale = group.getNumFemale();
				Map<String, Integer> langDistribution = new HashMap<String, Integer>(
						group.getLangDistribution());

				// Add age info to group metrics
				if (composition.contains("age")) {
					if (ageGroup.equals("young")) {
						numYoung++;
					} else if (ageGroup.equals("middle")) {
						numMiddle++;
					} else {
						numOld++;
					}
				}

				// Add gender info to group metrics
				if (composition.contains("gender")) {
					if (gender.equals("Male")) {
						numMale++;
					} else {
						numFemale++;
					}
				}

				// Add lang info to group metrics
				if (composition.contains("lang")) {
					for (String lang : langs) {
						if (langDistribution.containsKey(lang)) {
							int numLang = langDistribution.get(lang);
							langDistribution.put(lang, numLang + 1);
						} else {
							langDistribution.put(lang, 1);
						}
					}
				}

				// Compute difference to each other group
				int maxDiff = 0;
				for (Group group2 : groups) {
					int diff = 0;
					if (group2.equals(group))
						continue;

					// Age difference
					if (composition.contains("age")) {
						int ageDiff = 0;
						ageDiff = Math.abs(numYoung - group2.getNumYoung())
								+ Math.abs(numMiddle - group2.getNumMiddle())
								+ Math.abs(numOld - group2.getNumOld());
						diff += ageDiff;
					}

					// Gender difference
					if (composition.contains("gender")) {
						int genderDiff = 0;
						genderDiff = Math.abs(numMale - group2.getNumMale())
								+ Math.abs(numFemale - group2.getNumFemale());
						diff += genderDiff;
					}

					// Lang difference
					if (composition.contains("lang")) {
						int langDiff = 0;
						for (String lang : langDistribution.keySet()) {
							int numCurrentLang = langDistribution.get(lang);
							if (group2.getLangDistribution().containsKey(lang)) {
								langDiff += Math.abs(numCurrentLang
										- group2.getLangDistribution()
												.get(lang));
							} else {
								langDiff += numCurrentLang;
							}
						}
						diff += langDiff;
					}

					// Keep track of the maximum difference
					if (diff > maxDiff)
						maxDiff = diff;
				}
				results.add(maxDiff);
			}

			// 4. Match user to the group such that the max. difference of the
			// groups
			// will be minimized.
			int min = Collections.min(results);
			int matchedGroup = results.indexOf(min);
			// Adapt group metrics of the group the user is added to
			if (ageGroup.equals("young")) {
				groups.get(matchedGroup).setNumYoung(
						groups.get(matchedGroup).getNumYoung() + 1);
			} else if (ageGroup.equals("middle")) {
				groups.get(matchedGroup).setNumMiddle(
						groups.get(matchedGroup).getNumMiddle() + 1);
			} else {
				groups.get(matchedGroup).setNumOld(
						groups.get(matchedGroup).getNumOld() + 1);
			}
			if (gender.equals("Male")) {
				groups.get(matchedGroup).setNumMale(
						groups.get(matchedGroup).getNumMale() + 1);
			} else {
				groups.get(matchedGroup).setNumFemale(
						groups.get(matchedGroup).getNumFemale() + 1);
			}
			for (String lang : langs) {
				if (groups.get(matchedGroup).getLangDistribution()
						.containsKey(lang)) {
					int numLang = groups.get(matchedGroup)
							.getLangDistribution().get(lang);
					groups.get(matchedGroup).getLangDistribution()
							.put(lang, numLang + 1);
				} else {
					groups.get(matchedGroup).getLangDistribution().put(lang, 1);
				}
			}

			// Add to result map
			matchedGroup++;
			matchedUsers.put(participant, matchedGroup);
			LOG.info("Matched user: " + participant + " to group #"
					+ matchedGroup);
		}
		return matchedUsers;
	}
}
