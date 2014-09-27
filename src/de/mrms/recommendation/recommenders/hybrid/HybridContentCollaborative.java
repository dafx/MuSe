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
package de.mrms.recommendation.recommenders.hybrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mrms.api.AbstractRecommender;
import de.mrms.api.DataRepository;
import de.mrms.api.Recommendation;
import de.mrms.api.User;
import de.mrms.config.RecommenderConfig;
import de.mrms.recommendation.MuseRecommendation;
import de.mrms.recommendation.recommenders.collaborative.CollaborativeFilteringRecommender;
import de.mrms.recommendation.recommenders.content.ContentBasedRecommender;

/**
 * Hybrid Recommender using linear combination of scores from Collaborative
 * Filtering and Content Based Filtering Recommenders.
 * 
 * @see ContentBasedRecommender
 * @see CollaborativeFilteringRecommender
 * 
 */
public class HybridContentCollaborative extends AbstractRecommender {
	// Meta information
	private static final String NAME = "Hybrid Content Collaborative";
	private static final String EXPLANATION = "Recommendations are based on items "
			+ "that are similar to items you like and users you are similar to.";
	private static final Map<String, Double> tagDistribution = new HashMap<String, Double>();
	static {
		tagDistribution.put("Accuracy", 80.0);
		tagDistribution.put("Novelty", 20.0);
		tagDistribution.put("Diversity", 0.0);
		tagDistribution.put("Serendipity", 0.0);
	}

	// Member variables
	private static final int WEIGHT_REC_ONE = 70;
	private static final int WEIGHT_REC_TWO = 30;

	/**
	 * Create recommender object
	 * 
	 * @param ID
	 *            The internal ID of the recommender
	 * @param dataRepository
	 *            The data repository
	 */
	public HybridContentCollaborative(int ID, DataRepository dataRepository) {
		super(ID, dataRepository);
	}

	@Override
	public List<Recommendation> getRecommendations(User user, int howMany) {
		List<Recommendation> recommendations = new ArrayList<Recommendation>();

		// Get recommendations of both recommenders
		ContentBasedRecommender cbr = (ContentBasedRecommender) RecommenderConfig
				.getRecommenders().get(6);
		CollaborativeFilteringRecommender cf = (CollaborativeFilteringRecommender) RecommenderConfig
				.getRecommenders().get(7);

		// Combine recommendations to a new list of recommendations
		List<Recommendation> recsTemp = cbr.getRecommendations(user, howMany);
		recsTemp.addAll(cf.getRecommendations(user, howMany));

		for (Recommendation rec : recsTemp) {
			((MuseRecommendation) rec).setRecommenderID(getID());

			// Adapt score and explanation if both recommenders have created the
			// same
			// recommendation. Score' = (Score_1 * Weight_1 + Score_2 *
			// Weight_2)
			int duplicateIndex = recommendations.indexOf(rec);
			if (duplicateIndex == -1) {
				recommendations.add(rec);
			} else {
				MuseRecommendation duplicate = (MuseRecommendation) recommendations
						.get(duplicateIndex);
				// Score
				duplicate.setScore((rec.getScore() * WEIGHT_REC_ONE + duplicate
						.getScore() * WEIGHT_REC_TWO));
				// Explanation
				duplicate.setExplanation(rec.getExplanation() + " and "
						+ duplicate.getExplanation());
			}
		}

		// Sort by score and return topN recommendations
		Collections.sort(recommendations, Collections.reverseOrder());
		return recommendations.subList(0, howMany);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getExplanation() {
		return EXPLANATION;
	}

	@Override
	public Map<String, Double> getTagDistribution() {
		return tagDistribution;
	}

}
