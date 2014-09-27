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

/**
 * Inject data for a randomized values experiment.
 */
public class RandomizedExperiment {
	private final static int USER_NUM = 80;
	private final static int LIST_NUM_USER = 10;
	private final static int RATING_NUM_LIST = 10;
	private final static int EXP_EVAL_ID = 2;
	private final static long START_DATE = 1391990400; // 10.02.2014
	private final static long END_DATE = 1392940800; // 15.02.2014

	public static void putUsers() throws SQLException {
		List<MuseUser> users = new ArrayList<MuseUser>();
		List<String> userNames = new ArrayList<String>();

		// Put 80 random users
		for (int i = 1; i <= USER_NUM; i++) {
			// Create user
			String name = "userskw" + String.valueOf(i);
			int birth = RandomDataProvider.getRandomBirthyear();
			String gender = RandomDataProvider.getRandomGender();
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

			// Create 10 lists
			for (int j = 1; j <= LIST_NUM_USER; j++) {
				int listRating = RandomDataProvider.getRandomListRating();
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
					rec.setScore(RandomDataProvider.getRandomPredictionScore());
					rec.setExplanation(String.valueOf(RandomDataProvider
							.getRandomTrackRating())); // Use explanation to
														// inject rating

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
						listRating, START_DATE, END_DATE);
			}
		}
	}
}
