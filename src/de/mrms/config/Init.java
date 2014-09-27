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
package de.mrms.config;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.quartz.CronScheduleBuilder;
import org.quartz.DateBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mrms.data.cronjob.DailyJob;
import de.mrms.data.cronjob.WeeklyJob;

public class Init implements ServletContextListener {
	// Configured logger
	private static final Logger LOG = LoggerFactory.getLogger(Init.class
			.getName());
	private Scheduler scheduler;

	@Override
	// Write back on shutdown
	public void contextDestroyed(ServletContextEvent arg0) {
		try {
			LOG.info("Writing back configuration file.");
			RecommenderConfig.writeBackConfig();
		} catch (IOException e) {
			LOG.warn("Writing back configuration failed!", e);
		} catch (URISyntaxException e) {
			LOG.warn("Writing back configuration failed!", e);
		}

		LOG.info("Canceling job scheduler.");
		try {
			scheduler.shutdown();
		} catch (SchedulerException e) {
			LOG.warn("Shutting down scheduler failed.", e);
		}
	}

	@Override
	// Initialize on startup
	public void contextInitialized(ServletContextEvent arg0) {
		// Init recommender config
		try {
			LOG.info("Initializing configuration.");
			RecommenderConfig.initializeConfig();
		} catch (Exception e) {
			LOG.warn("Couldn't initialize configuration.", e);
		}

		// Schedule repetitive tasks
		LOG.info("Scheduling tasks.");
		try {
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.start();
		} catch (SchedulerException e) {
			LOG.warn("Starting scheduler failed.", e);
		}

		// Trigger the job to run daily
		Trigger dailyTrigger = TriggerBuilder.newTrigger()
				.withIdentity("daily", "group1")
				.withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(23, 45))
				.build();

		// Trigger the job to run weekly
		Trigger weeklyTrigger = TriggerBuilder
				.newTrigger()
				.withIdentity("weekly", "group1")
				.withSchedule(
						CronScheduleBuilder.weeklyOnDayAndHourAndMinute(
								DateBuilder.SUNDAY, 23, 0)).build();

		// Tell quartz to schedule the job using our triggers
		JobDetail dailyJob = JobBuilder.newJob(DailyJob.class)
				.withIdentity("dailyJob", "group1").build();
		JobDetail weeklyJob = JobBuilder.newJob(WeeklyJob.class)
				.withIdentity("weeklyJob", "group1").build();
		try {
			scheduler.scheduleJob(dailyJob, dailyTrigger);
			scheduler.scheduleJob(weeklyJob, weeklyTrigger);
		} catch (SchedulerException e) {
			LOG.warn("Scheduling jobs failed.", e);
		}
	}
}
