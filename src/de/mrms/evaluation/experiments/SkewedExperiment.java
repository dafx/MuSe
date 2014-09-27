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
package de.mrms.evaluation.experiments;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.mrms.evaluation.Evaluation;
import de.mrms.evaluation.EvaluationData;
import de.mrms.recommendation.MuseRecommendation;
import de.mrms.recommendation.MuseSong;
import de.mrms.user.MuseUser;
import de.mrms.user.Option;

public class SkewedExperiment {
	private final static int USER_NUM = 80;
	private final static int LIST_NUM_USER = 10;
	private final static int RATING_NUM_LIST = 10;
	private final static int EXP_EVAL_ID = 3;
	private final static long START_DATE = 1392508800; // 16.02.2014
	private final static long ACTIVE_BORDER = 1392681600; // 18.02.2014

	// Skew percentages and values. 10% Old females and 90% young males.
	private final static int NUM_FEMALE_OLD = 8; // 10%
	private final static int[] YOUNG = { 18, 30 };
	private final static int[] OLD = { 50, 70 };

	// Rating skew
	private static final int skewedRecommender = 3;

	public static void putUsers() throws SQLException {
		List<MuseUser> users = new ArrayList<MuseUser>();
		List<String> userNames = new ArrayList<String>();

		// Put 80 random users
		int skew = 0;
		for (int i = 1; i <= USER_NUM; i++) {
			// Create user
			String name = "userskw" + String.valueOf(i);

			// Create skewed user. Every 5 users until reached NUM_OLD reached
			// create
			// OLD + FEMALE, otherwise create YOUNG + MALE
			int birth;
			String gender;
			if (i % 8 == 0 && skew < NUM_FEMALE_OLD) {
				birth = RandomDataProvider.createRandomInt(OLD[0], OLD[1]);
				gender = "Female";
				skew++;
			} else {
				birth = RandomDataProvider.createRandomInt(YOUNG[0], YOUNG[1]);
				gender = "Male";
			}

			List<String> langs = RandomDataProvider.getRandomLanguages();
			MuseUser user = new MuseUser(name.toLowerCase(), birth, gender,
					langs);
			users.add(user);
			userNames.add(user.getName());
		}

		// Put users to db
		ExperimentData.putUsers(users);

		// Match users to evaluation group
		Evaluation eval = EvaluationData.getById(EXP_EVAL_ID);
		Map<String, Integer> groupNums = eval.matchUserToGroup(userNames);
		for (String userName : userNames) {
			eval.addParticipant(groupNums.get(userName), userName);
		}
	}

	public static void putRatings() throws SQLException {
		// Get all available track ids
		List<Integer> trackIds = ExperimentData.getAllTrackIds();

		// Put LIST_NUM_USER * RATING_NUM_LIST ratings for each user
		for (int i = 1; i <= USER_NUM; i++) {
			String name = "userskw" + String.valueOf(i);
			Option opt = EvaluationData.getSettingsForParticipant(name);
			ArrayList<Integer> recommenders = opt.getRecommenders();
			boolean skew = false;
			if (recommenders.get(1) == skewedRecommender) {
				skew = true;
			}

			// Create 10 lists
			for (int j = 1; j <= LIST_NUM_USER; j++) {
				int listRating;
				if (skew) {
					// Skewed rating
					listRating = RandomDataProvider.createRandomInt(4, 5);
				} else {
					listRating = RandomDataProvider.getRandomListRating();
				}

				List<MuseRecommendation> recs = new ArrayList<MuseRecommendation>();
				// Create RATING_NUM_LIST ratings per list
				for (int k = 1; k <= RATING_NUM_LIST; k++) {
					// Create recommendation
					MuseRecommendation rec = new MuseRecommendation();
					if (k > RATING_NUM_LIST / 2) {
						rec.setRecommenderID(recommenders.get(1));
					} else {
						rec.setRecommenderID(recommenders.get(0));
					}

					// Use explanation to inject rating
					if (skew && rec.getRecommenderID() == skewedRecommender) {
						rec.setExplanation(String.valueOf(RandomDataProvider
								.createRandomInt(1, 2))); // Skewed rating
						rec.setScore(RandomDataProvider
								.createRandomDouble(1, 2));
					} else {
						rec.setExplanation(String.valueOf(RandomDataProvider
								.getRandomTrackRating()));
						rec.setScore(RandomDataProvider
								.getRandomPredictionScore());
					}

					// Create random song
					int rndSongIndex = RandomDataProvider.createRandomInt(0,
							trackIds.size() - 1);
					MuseSong song = new MuseSong("none", "none");
					song.setID(trackIds.get(rndSongIndex));
					rec.setSong(song);
					recs.add(rec);
				}

				// Put list to database
				System.out.println("Created List #" + j + " for user " + name);
				ExperimentData.putRatingList(name, EXP_EVAL_ID, recs,
						listRating, START_DATE, ACTIVE_BORDER);
			}
		}
	}

}
