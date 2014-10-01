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
package de.muse.evaluation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muse.api.Recommender;
import de.muse.config.RecommenderConfig;
import de.muse.utility.Database;

/**
 * 
 * A Helper class containing methods to compute statistical metrics of the MuSe
 * service. E.g. Number of users, numbers of created recommendations, etc.
 * 
 * In Addition this can also be used as a wrapper object for sending statistical
 * information to the client.
 * 
 */
public class Statistics {
	// Configured logger
	private static final Logger LOG = LoggerFactory.getLogger(Statistics.class
			.getName());
	private final transient String rangeFrom;
	private final transient String rangeTo;
	private final transient String defaultFrom = "2014-01-18";
	private final transient String dateFormat = "yyyy-mm-dd";

	// Private members. These are used as kind of a wrapper to send the
	// information to the client in one call.
	private int numUsers;
	private int optChanged;
	private int behaviorChanged;
	private HashMap<String, Integer> genderDistribution;
	private HashMap<Integer, Integer> ageDistribution;
	private HashMap<String, Integer> langDistribution;
	private HashMap<Integer, Integer> ratingDistribution;
	private HashMap<String, Double> visitDistribution;
	private TreeMap<String, Integer> visits;
	private TreeMap<String, Integer> ratings;
	private HashMap<String, HashMap<Integer, Integer>> recommenderRatingDistribution;
	private HashMap<String, Double> meanAbsoluteErrors;
	private int numberOfLastFmUsers;
	private int numberOfRecommenders;
	private int numberOfRecommendations;
	private int numberOfLists;
	private int numberOfRatings;

	/**
	 * Create a statistics object with a given date range.
	 */
	public Statistics(String rangeFrom, String rangeTo) {
		if (rangeFrom == null) {
			rangeFrom = defaultFrom;
		}
		if (rangeTo == null) {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			Calendar start = new GregorianCalendar();
			rangeTo = formatter.format(start.getTime());
		}
		this.rangeFrom = rangeFrom;
		this.rangeTo = rangeTo;
	}

	/**
	 * Create an empty statistics object with default from, to dates.
	 */
	public Statistics() {
		this(null, null);
	}

	/**
	 * Runs all metrics and saves the information to the object members.
	 */
	public void getAllMetrics() {
		// Get all metric information and save it to the object
		getNumberOfUsers();
		getAvgOptChanges();
		getAvgBehaviorChanges();
		getGenderDistribution();
		getAgeDistribution();
		getLanguageDistribution();
		getNumberOfLastFmUsers();
		getNumberOfRecommenders();
		getNumberOfRecommendations();
		getNumberOfCreatedLists();
		getNumberOfRatings();
		getRatingDistribution();
		getRecommenderRatingDistribution();
		getVisitDistribution();
		getVisits();
		getMeanAbsoluteErrors();
		getRatingsOverTime();
	}

	// Retrieve general information about the service and its users.
	/**
	 * Get the number registered users of the MuSe service. Returns the value
	 * and sets the member of the object.
	 */
	public int getNumberOfUsers() {
		int numUsers = 0;
		String query;

		query = "SELECT COUNT(*) as countUsers FROM consumer WHERE "
				+ "registration_date BETWEEN to_date('" + rangeFrom + "', '"
				+ dateFormat + "')" + " AND to_date('" + rangeTo + "', '"
				+ dateFormat + "')";

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			result = stmt.executeQuery(query);
			while (result.next()) {
				numUsers = result.getInt("countUsers");
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get number of users.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}

		this.numUsers = numUsers;
		return this.numUsers;
	}

	/**
	 * Get the average number of options changes in the MuSe service. Returns
	 * the value and sets the member of the object.
	 */
	public int getAvgOptChanges() {
		int optChanged = 0;
		String query;

		query = "SELECT AVG(numChanged) as optChanged FROM(SELECT consumer_name, COUNT(*) "
				+ "as numChanged FROM option_activities WHERE "
				+ "timestamp BETWEEN to_date('"
				+ rangeFrom
				+ "', '"
				+ dateFormat
				+ "')"
				+ " AND to_date('"
				+ rangeTo
				+ "', '"
				+ dateFormat + "') GROUP BY consumer_name)";

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			result = stmt.executeQuery(query);
			while (result.next()) {
				optChanged = result.getInt("optChanged");
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get number of changed options.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}

		this.optChanged = optChanged;
		return this.optChanged;
	}

	/**
	 * Get the average number of behavior changes in the MuSe service. Returns
	 * the value and sets the member of the object.
	 */
	public int getAvgBehaviorChanges() {
		int behaviorChanged = 0;
		String query;

		query = "SELECT AVG(numChanged) as behaviorChanged FROM(SELECT consumer_name, COUNT(DISTINCT behavior) "
				+ "as numChanged FROM option_activities WHERE "
				+ "timestamp BETWEEN to_date('"
				+ rangeFrom
				+ "', '"
				+ dateFormat
				+ "')"
				+ " AND to_date('"
				+ rangeTo
				+ "', '"
				+ dateFormat + "') GROUP BY consumer_name)";

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();
			result = stmt.executeQuery(query);
			while (result.next()) {
				behaviorChanged = result.getInt("behaviorChanged");
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get number of changed behvaior settings.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}

		this.behaviorChanged = behaviorChanged;
		return this.behaviorChanged;
	}

	/**
	 * Get the gender distribution of the MuSe service. Meaning number of
	 * registered males and number of registered females. Returns the value and
	 * sets the member of the object.
	 */
	public HashMap<String, Integer> getGenderDistribution() {
		HashMap<String, Integer> genderDistribution = new HashMap<String, Integer>();
		genderDistribution.put("Male", 0);
		genderDistribution.put("Female", 0);
		String query;

		query = "SELECT sex, COUNT(*) as numSex FROM consumer WHERE "
				+ "registration_date BETWEEN to_date('" + rangeFrom + "', '"
				+ dateFormat + "')" + " AND to_date('" + rangeTo + "', '"
				+ dateFormat + "') GROUP BY sex";

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();

			// Query for number of males
			result = stmt.executeQuery(query);
			while (result.next()) {
				genderDistribution.put(result.getString("sex"),
						result.getInt("numSex"));
			}
		} catch (SQLException e) {
			LOG.warn("Couldn't get gender distribution.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}

		this.genderDistribution = genderDistribution;
		return this.genderDistribution;
	}

	/**
	 * Get the age distribution of the MuSe service. Meaning for each birth year
	 * present in the user base the corresponding number of users who provided
	 * that year of birth. Returns the value and sets the member of the object.
	 */
	public HashMap<Integer, Integer> getAgeDistribution() {
		HashMap<Integer, Integer> ageDistribution = new HashMap<Integer, Integer>();
		String query;

		query = "SELECT Birthyear, COUNT(*) as numBirth FROM consumer WHERE "
				+ "registration_date BETWEEN to_date('" + rangeFrom + "', '"
				+ dateFormat + "')" + " AND to_date('" + rangeTo + "', '"
				+ dateFormat + "') GROUP BY birthyear";

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();

			// Query for number of males
			result = stmt.executeQuery(query);
			while (result.next()) {
				ageDistribution.put(result.getInt("Birthyear"),
						result.getInt("numBirth"));
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get age distribution.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}

		this.ageDistribution = ageDistribution;
		return this.ageDistribution;
	}

	/**
	 * Get the language distribution of the MuSe service. Meaning for each
	 * language the corresponding number of users that provided the given
	 * language. Returns the value and sets the member of the object.
	 */
	public HashMap<String, Integer> getLanguageDistribution() {
		HashMap<String, Integer> langDistribution = new HashMap<String, Integer>();
		String query;

		query = "SELECT language, count(*) as numLang FROM consumer_language"
				+ " WHERE consumer_name IN (SELECT name FROM consumer"
				+ " WHERE registration_date BETWEEN to_date('" + rangeFrom
				+ "', '" + dateFormat + "') AND to_date('" + rangeTo + "', '"
				+ dateFormat + "')) group by language";

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();

			// Query for number of males
			result = stmt.executeQuery(query);
			while (result.next()) {
				langDistribution.put(result.getString("language"),
						result.getInt("numLang"));
			}
		} catch (SQLException e) {
			LOG.warn("Couldn't get language distribution.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}

		this.langDistribution = langDistribution;
		return this.langDistribution;
	}

	/**
	 * Get the visit distribution of the MuSe service. Meaning for each site the
	 * corresponding time (in seconds) of users spent on the specific site.
	 * Returns the value and sets the member of the object.
	 */
	public HashMap<String, Double> getVisitDistribution() {
		HashMap<String, Double> visitDistribution = new HashMap<String, Double>();
		String query;

		query = "SELECT site, AVG(duration) as avgDuration FROM activities"
				+ " WHERE timestamp BETWEEN to_date('" + rangeFrom + "', '"
				+ dateFormat + "') AND to_date('" + rangeTo + "', '"
				+ dateFormat + "') GROUP BY site";

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();

			// Query for number of males
			result = stmt.executeQuery(query);
			while (result.next()) {
				visitDistribution.put(result.getString("site"),
						result.getDouble("avgDuration"));
			}
		} catch (SQLException e) {
			LOG.warn("Couldn't get visit distribution.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}

		this.visitDistribution = visitDistribution;
		return this.visitDistribution;
	}

	/**
	 * Get the visits of the MuSe service. Meaning for each date the
	 * corresponding number of user logins. Returns the value and sets the
	 * member of the object.
	 */
	public TreeMap<String, Integer> getVisits() {
		TreeMap<String, Integer> visits = new TreeMap<String, Integer>();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
		Calendar start;
		Calendar end;
		String query;

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			start = new GregorianCalendar();
			start.setTime(format.parse(rangeFrom));
			end = new GregorianCalendar();
			end.setTime(format.parse(rangeTo));

			// Build query
			query = "SELECT to_date(day, 'dd.mm.yy') as day, COUNT(consumer_name) as numUsers FROM Login_activities"
					+ " WHERE day BETWEEN to_date('"
					+ rangeFrom
					+ "', '"
					+ dateFormat
					+ "') AND to_date('"
					+ rangeTo
					+ "', '"
					+ dateFormat + "') GROUP BY to_date(day, 'dd.mm.yy')";

			while (start.compareTo(end) <= 0) {
				visits.put(formatter.format(start.getTime()), 0);
				start.add(Calendar.DAY_OF_MONTH, 1);
			}

			// Get data from DB
			conn = Database.getConnection();
			stmt = conn.createStatement();

			// Query for number of males
			result = stmt.executeQuery(query);
			while (result.next()) {
				String date = formatter.format(result.getDate("day"));
				visits.put(date, result.getInt("numUsers"));
			}
		} catch (SQLException e) {
			LOG.warn("Couldn't get visit distribution.", e);
		} catch (ParseException e) {
			LOG.warn("Wrong date formats.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}

		this.visits = visits;
		return this.visits;
	}

	/**
	 * Get the LastFm distribution of the MuSe service. Meaning number of users
	 * that provided a Last.fm Username and number of users that didn't. Returns
	 * the value and sets the member of the object.
	 */
	public int getNumberOfLastFmUsers() {
		int lastFmUsers = 0;
		String query;

		query = "SELECT count(*) as numLfm FROM consumer WHERE "
				+ "registration_date BETWEEN to_date('" + rangeFrom + "', '"
				+ dateFormat + "')" + " AND to_date('" + rangeTo + "', '"
				+ dateFormat + "') AND lfmaccount IS NOT NULL";

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();

			// Query for number of males
			result = stmt.executeQuery(query);
			while (result.next()) {
				lastFmUsers = result.getInt("numLfm");
			}
		} catch (SQLException e) {
			LOG.warn("Couldn't get number of Last.fm Users.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}

		this.numberOfLastFmUsers = lastFmUsers;
		return this.numberOfLastFmUsers;
	}

	// Retrieve general information about the recommenders and ratings (Not
	// recommender specific)

	/**
	 * Get the number of recommenders currently active in the MuSe service.
	 * Returns the value and sets the member of the object.
	 */
	public int getNumberOfRecommenders() {
		numberOfRecommenders = RecommenderConfig.getRecommenders().size();
		return numberOfRecommenders;
	}

	/**
	 * Get the current number of created recommendations. Returns the value and
	 * sets the member of the object.
	 */
	public int getNumberOfRecommendations() {
		int numRecs = 0;
		String query;

		query = "SELECT count(*) as numRecs FROM recommendation WHERE "
				+ "time BETWEEN to_date('" + rangeFrom + "', '" + dateFormat
				+ "') AND to_date('" + rangeTo + "', '" + dateFormat + "')";

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();

			// Query for number of males
			result = stmt.executeQuery(query);
			while (result.next()) {
				numRecs = result.getInt("numRecs");
			}
		} catch (SQLException e) {
			LOG.warn("Couldn't get number of recommendations.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}

		this.numberOfRecommendations = numRecs;
		return this.numberOfRecommendations;
	}

	/**
	 * Get the current number of created recommendation lists. Returns the value
	 * and sets the member of the object.
	 */
	public int getNumberOfCreatedLists() {
		int numLists = 0;
		String query;

		query = "SELECT SUM(numlist) as maxlist FROM (SELECT consumer, count(list) as numlist "
				+ "FROM (SELECT distinct consumer, list FROM recommendation WHERE "
				+ "time BETWEEN to_date('"
				+ rangeFrom
				+ "', '"
				+ dateFormat
				+ "') AND to_date('"
				+ rangeTo
				+ "', '"
				+ dateFormat
				+ "') GROUP BY consumer, list) group by consumer)";

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();

			// Query for number of males
			result = stmt.executeQuery(query);
			while (result.next()) {
				numLists = result.getInt("maxlist");
			}
		} catch (SQLException e) {
			LOG.warn("Couldn't get number of lists.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}

		this.numberOfLists = numLists;
		return this.numberOfLists;
	}

	/**
	 * Get the current number of rated recommendations. Returns the value and
	 * sets the member of the object.
	 */
	public int getNumberOfRatings() {
		int numRatings = 0;
		String query;

		query = "SELECT count(*) as numRatings "
				+ "FROM recommendation WHERE rating != 0 AND "
				+ "time BETWEEN to_date('" + rangeFrom + "', '" + dateFormat
				+ "') AND to_date('" + rangeTo + "', '" + dateFormat + "')";

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();

			// Query for number of males
			result = stmt.executeQuery(query);
			while (result.next()) {
				numRatings = result.getInt("numRatings");
			}
		} catch (SQLException e) {
			LOG.warn("Couldn't get number of ratings.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}

		this.numberOfRatings = numRatings;
		return this.numberOfRatings;
	}

	/**
	 * Get the current distribution of ratings. Meaning number of ratings for
	 * each rating option {-1, 0, 1 ,2}. Returns the value and sets the member
	 * of the object.
	 */
	public HashMap<Integer, Integer> getRatingDistribution() {
		HashMap<Integer, Integer> ratingDistribution = new HashMap<Integer, Integer>();
		String query;

		query = "SELECT rating, COUNT(*) as numRating "
				+ "FROM recommendation WHERE " + "time BETWEEN to_date('"
				+ rangeFrom + "', '" + dateFormat + "') AND to_date('"
				+ rangeTo + "', '" + dateFormat + "') GROUP BY rating";

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();

			// Query for number of males
			result = stmt.executeQuery(query);
			while (result.next()) {
				ratingDistribution.put(result.getInt("rating"),
						result.getInt("numRating"));
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get rating distribution.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}

		this.ratingDistribution = ratingDistribution;
		return this.ratingDistribution;
	}

	/**
	 * Get the ratings of the MuSe service over time. Meaning for each date the
	 * corresponding number of ratings. Returns the value and sets the member of
	 * the object.
	 */
	public TreeMap<String, Integer> getRatingsOverTime() {
		TreeMap<String, Integer> ratings = new TreeMap<String, Integer>();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
		Calendar start;
		Calendar end;
		String query;

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			start = new GregorianCalendar();
			start.setTime(format.parse(rangeFrom));
			end = new GregorianCalendar();
			end.setTime(format.parse(rangeTo));

			// Build query
			query = "SELECT to_date(trunc(time), 'dd.mm.yy') as day, COUNT(id) as numRatings FROM recommendation"
					+ " WHERE time BETWEEN to_date('"
					+ rangeFrom
					+ "', '"
					+ dateFormat
					+ "') AND to_date('"
					+ rangeTo
					+ "', '"
					+ dateFormat
					+ "') AND rating != 0 GROUP BY to_date(trunc(time), 'dd.mm.yy')";

			while (start.compareTo(end) <= 0) {
				ratings.put(formatter.format(start.getTime()), 0);
				start.add(Calendar.DAY_OF_MONTH, 1);
			}

			// Get data from DB
			conn = Database.getConnection();
			stmt = conn.createStatement();

			// Query for number of males
			result = stmt.executeQuery(query);
			while (result.next()) {
				String date = formatter.format(result.getDate("day"));
				ratings.put(date, result.getInt("numRatings"));
			}
		} catch (SQLException e) {
			LOG.warn("Couldn't get ratings over time.", e);
		} catch (ParseException e) {
			LOG.warn("Wrong date formats.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}

		this.ratings = ratings;
		return this.ratings;
	}

	/**
	 * Get the current distribution of ratings. Meaning number of ratings for
	 * each rating option {-1, 0, 1 ,2}. Returns the value and sets the member
	 * of the object.
	 */
	public HashMap<String, HashMap<Integer, Integer>> getRecommenderRatingDistribution() {
		HashMap<String, HashMap<Integer, Integer>> recommenderRatingDistribution = new HashMap<String, HashMap<Integer, Integer>>();
		String query;

		query = "SELECT recommender_id, rating, COUNT(*) "
				+ " as numRating FROM recommendation WHERE time BETWEEN to_date('"
				+ rangeFrom + "', '" + dateFormat + "') AND to_date('"
				+ rangeTo + "', '" + dateFormat
				+ "') GROUP BY recommender_id, rating";

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();

			// Query for number of males
			HashMap<Integer, Recommender> recommenders = RecommenderConfig
					.getRecommenders();
			result = stmt.executeQuery(query);
			while (result.next()) {
				int recId = result.getInt("recommender_id");
				if (!recommenders.containsKey(recId)) {
					continue;
				}
				String recName = recommenders.get(recId).getName();
				if (!recommenderRatingDistribution.containsKey(recName)) {
					HashMap<Integer, Integer> evalHashMap = new HashMap<Integer, Integer>();
					evalHashMap.put(0, 0);
					evalHashMap.put(1, 0);
					evalHashMap.put(-1, 0);
					evalHashMap.put(2, 0);
					recommenderRatingDistribution.put(recName, evalHashMap);
				}
				recommenderRatingDistribution.get(recName).put(
						result.getInt("rating"), result.getInt("numRating"));
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get recommender rating distribution.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}

		this.recommenderRatingDistribution = recommenderRatingDistribution;
		return this.recommenderRatingDistribution;
	}

	// Measure accuracy of a RS
	/**
	 * Get the mean absolute error values for all recommenders in the system.
	 */
	public HashMap<String, Double> getMeanAbsoluteErrors() {
		HashMap<String, Double> meanAbsoluteErrors = new HashMap<String, Double>();
		String query;

		query = "SELECT recommender_id, AVG(abs(rating - score)) as MAE "
				+ "FROM Recommendation WHERE rating != 0 AND time BETWEEN to_date('"
				+ rangeFrom + "', '" + dateFormat + "') AND to_date('"
				+ rangeTo + "', '" + dateFormat + "') GROUP BY recommender_id";

		// Connect to database
		Connection conn = null;
		Statement stmt = null;
		ResultSet result = null;

		try {
			conn = Database.getConnection();
			stmt = conn.createStatement();

			// Query for number of males
			result = stmt.executeQuery(query);
			while (result.next()) {
				int recId = result.getInt("recommender_id");
				String recName = RecommenderConfig.getRecommenders().get(recId)
						.getName();
				meanAbsoluteErrors.put(recName, result.getDouble("MAE"));
			}

		} catch (SQLException e) {
			LOG.warn("Couldn't get mean absolute errors.", e);
		} finally {
			Database.quietClose(conn, stmt, result);
		}

		this.meanAbsoluteErrors = meanAbsoluteErrors;
		return this.meanAbsoluteErrors;
	}

}
