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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mrms.evaluation.Evaluation;
import de.mrms.evaluation.EvaluationData;
import de.mrms.recommendation.MuseRepository;
import de.mrms.recommendation.recommenders.collaborative.CollaborativeFilteringRecommender;
import de.mrms.recommendation.recommenders.content.ContentBasedRecommender;
import de.mrms.user.Option;
import de.mrms.user.UserData;
import de.mrms.utility.Database;

/**
 * Refreshing the data of the recommender system on a daily basis.
 */
public class DailyJob implements Job {
	// Configured Logger
	private static final Logger LOG = LoggerFactory.getLogger(DailyJob.class
			.getName());

	@Override
	public void execute(JobExecutionContext context) {

		// Clear password recovery table
		LOG.warn("Daily Cronjob started.");
		LOG.info("Clearing password recovery table");
		Database.clearTable("password_recovery");

		// Adapt user flags
		refreshFlags();

		LOG.info("Refresh recommendation data.");
		refreshData();
		LOG.warn("Daily Cronjob finished");
	}

	public void refreshFlags() {
		// Check for finished evaluations and reset participants flags
		LOG.info("Finalizing evaluations that finish today.");
		resetParticipantFlags();

		// Adapt newcomer flags
		LOG.info("Adapting newcomer flags.");
		resetNewcomerFlags();
	}

	// Refresh all data relations
	public void refreshData() {
		// Get all users
		List<String> users = UserData.getAllActiveUsers();

		// Refresh Content based user-item matrix
		LOG.info("Refreshing content-based recommender data.");
		Database.clearTable("user_track_score");
		ContentBasedRecommender cb = new ContentBasedRecommender(0,
				new MuseRepository());
		for (String userName : users) {
			cb.refreshUserItemMatrix(userName, 20);
		}

		LOG.info("Refreshing collaborative-filtering recommender data.");
		// Refresh collaborative based user-user data
		Database.clearTable("user_similarities");
		CollaborativeFilteringRecommender cf = new CollaborativeFilteringRecommender(
				0, new MuseRepository());
		for (String userName : users) {
			cf.refreshUserSimilarities(userName);
		}

		// Precompute CF scores for all users
		Database.clearTable("user_user_score");
		for (String userName : users) {
			cf.refreshUserItemMatrix(userName, 20);
		}
	}

	/**
	 * Adapt newcomer flags according to the given number of ratings, for users
	 * that do not take part in an evaluation.
	 */
	public void resetNewcomerFlags() {
		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			pstmt = conn
					.prepareStatement("UPDATE consumer SET newcomer = 'N' WHERE name = ?");

			result = stmt
					.executeQuery("SELECT consumer, COUNT(*) as numRatings "
							+ "FROM recommendation, consumer "
							+ "WHERE consumer = name AND rating != 0 AND newcomer = 'Y' "
							+ "AND (eval_participant = 'N' OR eval_participant IS NULL)"
							+ "GROUP BY consumer");

			while (result.next()) {
				String user = result.getString("consumer");
				int ratings = result.getInt("numRatings");
				if (ratings >= 15) {
					pstmt.setString(1, user);
					pstmt.execute();
					LOG.info("Unset newcomer flag for user: " + user);
				}
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't check for newcomer users.", e);
		} finally {
			Database.quietClose(stmt);
			Database.quietClose(conn, pstmt, result);
		}
	}

	/**
	 * Adapt newcomer and invitation flags for evaluation participants.
	 */
	public void resetParticipantFlags() {
		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;

		try {
			Evaluation eval = EvaluationData.getRunning();
			// Check for running evaluation
			if (eval == null)
				return;

			conn = Database.getConnection();
			stmt = conn.createStatement();

			// Check if evaluation is ending today
			Calendar cal = new GregorianCalendar();
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			String startStr = format.format(cal.getTime());
			String endStr = format.format(eval.getTo().getTime());
			if (startStr.equals(endStr)) {
				stmt.executeUpdate("UPDATE consumer SET eval_participant = NULL");
				LOG.info("Finishing evaluation with id " + eval.getId());
				return;
			}

			// Adapt newcomer flag for participants
			pstmt = conn
					.prepareStatement("UPDATE consumer SET newcomer = 'N' WHERE name = ?");

			result = stmt
					.executeQuery("SELECT consumer, COUNT(*) as numRatings "
							+ "FROM recommendation, consumer "
							+ "WHERE consumer = name AND rating != 0 AND newcomer = 'Y' "
							+ "AND eval_participant = 'Y' AND eval_id = "
							+ eval.getId() + " GROUP BY consumer");

			while (result.next()) {
				String user = result.getString("consumer");
				int ratings = result.getInt("numRatings");
				if (ratings >= 15) {
					pstmt.setString(1, user);
					pstmt.execute();

					// Set evaluation group settings for the user
					Option opts = EvaluationData
							.getSettingsForParticipant(user);
					if (opts != null) {
						UserData.saveOptions(opts.getBehavior(),
								opts.getRecommenders(), user);
					}

					LOG.info("Unset newcomer flag for participant: " + user);
				}
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't reset participant flags.", e);
		} finally {
			Database.quietClose(pstmt);
			Database.quietClose(conn, stmt, result);
		}
	}
}
