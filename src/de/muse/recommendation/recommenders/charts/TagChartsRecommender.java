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
package de.muse.recommendation.recommenders.charts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.muse.api.DataRepository;
import de.muse.api.Recommendation;
import de.muse.api.Song;
import de.muse.api.User;

/**
 * 
 * This recommender creates recommendations based on information about your
 * played tracks on last.fm.
 * 
 */
public class TagChartsRecommender extends ChartsRecommender {
	// Meta Information
	private static final String NAME = "Favorit Tags";
	private static final String EXPLANATION = "Use last.fm information to get tags that fit the musical taste of the user best.";
	private static final Map<String, Double> tagDistribution = new HashMap<String, Double>();
	static {
		tagDistribution.put("Accuracy", 30.0);
		tagDistribution.put("Novelty", 40.0);
		tagDistribution.put("Serendipity", 20.0);
		tagDistribution.put("Diversity", 10.0);
	}

	/**
	 * Create recommender object
	 * 
	 * @param ID
	 *            The internal ID of the recommender
	 * @param dataRepository
	 *            The data repository
	 */
	public TagChartsRecommender(int ID, DataRepository dataRepository) {
		super(ID, dataRepository);
	}

	@Override
	public List<Recommendation> getRecommendations(User user, int howMany) {
		List<Recommendation> recommendations = new ArrayList<Recommendation>();

		// Get the tag charts of the user
		HashMap<String, List<Song>> charts = getDataRepository().getTagCharts(
				user);
		// Get songs the user has already seen
		Set<Integer> ratedSongs = getDataRepository().getRatedSongIDs(
				user.getName());

		// Accumulate the top songs of the annual charts
		int position = 1;
		while (position <= 20 && recommendations.size() <= howMany) {
			for (String tag : charts.keySet()) {
				// Check if wanted number of recommendations is reached
				if (recommendations.size() >= howMany)
					break;

				// Check if the chart position is available
				if (charts.get(tag).size() < position)
					continue;

				// If available
				Song topSong = charts.get(tag).get(position - 1);

				// Check if the user has seen it before, if so DON'T add to
				// recommendation list
				if (ratedSongs.contains(topSong.getID()))
					continue;

				// Add to the recommendation list if not already contained
				boolean duplicate = false;
				for (Recommendation recClone : recommendations) {
					if (duplicate == true) {
						break;
					}
					if (topSong.equals(recClone.getSong())) {
						duplicate = true;
					}
					if (recClone.getSong().getArtist()
							.equals(topSong.getArtist())) {
						duplicate = true;
					}
				}
				if (!duplicate) {
					double score = computeScoreByPosition(position);
					Recommendation rec = getDataRepository()
							.getRecommendationFactory().createRecommendation(
									topSong,
									getID(),
									"Number " + position
											+ " of songs tagged with " + tag,
									score);
					recommendations.add(rec);
				}
			}
			position++;
		}
		return recommendations;
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
