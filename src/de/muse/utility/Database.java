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
package de.muse.utility;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import oracle.jdbc.pool.OracleDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muse.config.ApplicationConfig;

/**
 * Database Helper class for server use only. Providing access to a connection
 * pool and several helper methods.
 */
public class Database {
	// Configured logger
	private static final Logger LOG = LoggerFactory.getLogger(Database.class
			.getName());

	// Database information
	private static OracleDataSource ods;

	private Database() {
		try {
			ods = new OracleDataSource();
			ods.setURL(ApplicationConfig.DB_CONNECTION);
			ods.setUser(ApplicationConfig.DB_USER);
			ods.setPassword(ApplicationConfig.DB_PASSWORD);

			// Load driver
			Class.forName(ApplicationConfig.JDBC_CLASS);
		} catch (SQLException e1) {
			LOG.warn("Connection failed!");
		} catch (ClassNotFoundException e) {
			LOG.warn("Driver not found!");
		}
	}

	/** Get Database connection */
	public static Connection getConnection() throws SQLException {
		if (ods == null)
			new Database();
		return ods.getConnection();
	}

	/**
	 * Close the given database connection.
	 * 
	 * @param conn
	 *            Connection to be closed.
	 */
	public static void quietClose(Connection conn) {
		try {
			if (conn != null)
				conn.close();
		} catch (SQLException e) {
			LOG.warn("Couldn't close Connection.", e);
		}
	}

	/**
	 * Close the given database Statement.
	 * 
	 * @param stmt
	 *            Statement to be closed.
	 */
	public static void quietClose(Statement stmt) {
		try {
			if (stmt != null)
				stmt.close();
		} catch (SQLException e) {
			LOG.warn("Couldn't close Statement.", e);
		}
	}

	/**
	 * Close the given database PreparedStatement.
	 * 
	 * @param pstmt
	 *            PreparedStatement to be closed.
	 */
	public static void quietClose(PreparedStatement pstmt) {
		try {
			if (pstmt != null)
				pstmt.close();
		} catch (SQLException e) {
			LOG.warn("Couldn't close PreparedStatement.", e);
		}
	}

	/**
	 * Close the given database ResultSet.
	 * 
	 * @param result
	 *            ResultSet to be closed.
	 */
	public static void quietClose(ResultSet result) {
		try {
			if (result != null)
				result.close();
		} catch (SQLException e) {
			LOG.warn("Couldn't close ResultSet.", e);
		}
	}

	/**
	 * Close the database "session" by closing the given Connection, Statement
	 * and ResulSet.
	 * 
	 * @param conn
	 *            Connection to be closed.
	 * @param stmt
	 *            Statement to be closed.
	 * @param result
	 *            ResultSet to be closed.
	 */
	public static void quietClose(Connection conn, Statement stmt,
			ResultSet result) {
		try {
			if (result != null)
				result.close();
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();
		} catch (SQLException e) {
			LOG.warn(
					"Couldn't close Database Connection, Statement and ResultSet.",
					e);
		}
	}

	/**
	 * Close the database "session" by closing the given Connection,
	 * PreparedStatement and ResulSet.
	 * 
	 * @param conn
	 *            Connection to be closed.
	 * @param stmt
	 *            Statement to be closed.
	 * @param result
	 *            ResultSet to be closed.
	 */
	public static void quietClose(Connection conn, PreparedStatement pstmt,
			ResultSet result) {
		try {
			if (result != null)
				result.close();
			if (pstmt != null)
				pstmt.close();
			if (conn != null)
				conn.close();
		} catch (SQLException e) {
			LOG.warn("Couldn't close Session.", e);
		}
	}

	/**
	 * Reset the autoCommit flag of the connection to true.
	 * 
	 * @param conn
	 *            Connection to be closed.
	 */
	public static void resetAutoCommit(Connection conn) {
		try {
			if (conn != null)
				conn.setAutoCommit(true);
		} catch (SQLException e) {
			LOG.warn("Couldn't reset autoCommit to true.", e);
		}
	}

	/**
	 * Rollback the transaction of the current connection.
	 * 
	 * @param conn
	 *            Connection to be closed.
	 */
	public static void quietRollback(Connection conn) {
		try {
			if (conn != null)
				conn.rollback();
		} catch (SQLException e) {
			LOG.warn("Couldn't rollback connection.", e);
		}
	}

	/**
	 * Clear the database table with the given name
	 * 
	 * @param tableName
	 *            The name of the table to clear.
	 * 
	 */
	public static void clearTable(String tableName) {
		// Connect to database
		Connection conn = null;
		Statement query = null;

		try {
			conn = Database.getConnection();
			query = conn.createStatement();
			query.execute("DELETE FROM " + tableName);

		} catch (SQLException e) {
			LOG.warn("Clearing table failed for table: " + tableName, e);
		} finally {
			Database.quietClose(conn);
			Database.quietClose(query);
		}
	}
}
