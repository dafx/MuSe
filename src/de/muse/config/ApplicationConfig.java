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

package de.muse.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationConfig {
	// Configured logger
	private static final Logger LOG = LoggerFactory
			.getLogger(RecommenderConfig.class.getName());

	// Constant settings
	public static final String DB_CONNECTION;
	public static final String DB_USER;
	public static final String DB_PASSWORD;
	public static final String JDBC_CLASS;
	public static final String LFM_API_KEY;
	public static final String MAIL_HOST;
	public static final String MAIL_SENDER;
	public static final String PERM_DIR;

	static {
		// Read config file
		Properties properties = new Properties();
		InputStream in = ApplicationConfig.class
				.getResourceAsStream("/app.properties");
		if (in == null) {
			LOG.warn("Application config file not found.");
			System.exit(1);
		}
		try {
			properties.load(in);
			in.close();
		} catch (IOException e) {
			LOG.warn("Application config not loaded correctly.", e);
		}

		// Assign
		DB_CONNECTION = properties.getProperty("db_connection_string");
		DB_USER = properties.getProperty("db_user");
		DB_PASSWORD = properties.getProperty("db_password");
		JDBC_CLASS = properties.getProperty("jdbc_class");
		LFM_API_KEY = properties.getProperty("lfm_api_key");
		MAIL_HOST = properties.getProperty("mail_host");
		MAIL_SENDER = properties.getProperty("mail_sender");
		PERM_DIR = properties.getProperty("perm_directory");
	}
}
