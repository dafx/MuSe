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
package de.mrms.recommendation.recommenders.charts;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.mrms.api.DataRepository;
import de.mrms.api.Recommendation;
import de.mrms.api.Song;
import de.mrms.api.User;
import de.mrms.recommendation.MuseRepository;

/**
 * 
 * This recommender creates recommendations based on demographic information of
 * a user combined with the current music charts of different cities.
 * 
 */
public class CityChartsRecommender extends ChartsRecommender {
	// Meta information
	private static final String NAME = "City Charts";
	private static final String EXPLANATION = "Use charts of cities combined with demographic information to produce recommendations.";
	private static final Map<String, Double> tagDistribution = new HashMap<String, Double>();
	static {
		tagDistribution.put("Accuracy", 10.0);
		tagDistribution.put("Novelty", 40.0);
		tagDistribution.put("Serendipity", 30.0);
		tagDistribution.put("Diversity", 30.0);
	}

	/**
	 * Create recommender object
	 * 
	 * @param ID
	 *            The internal ID of the recommender
	 * @param dataRepository
	 *            The data repository
	 */
	public CityChartsRecommender(int ID, DataRepository dataRepository) {
		super(ID, dataRepository);
	}

	@Override
	public List<Recommendation> getRecommendations(User user, int howMany) {
		List<Recommendation> recommendations = new ArrayList<Recommendation>();
		// Set the years to get songs from
		List<String> cities = user.getCities();
		if (cities.isEmpty()) {
			return recommendations;
		}

		// Get the city charts of the computed cities
		HashMap<String, List<Song>> charts = getDataRepository().getCityCharts(
				cities);
		List<SimpleEntry<Integer, Integer>> uniqueCount = ((MuseRepository) getDataRepository())
				.getSongCityCount(cities);

		// Get songs the user has already seen
		Set<Integer> ratedSongs = getDataRepository().getRatedSongIDs(
				user.getName());

		// Accumulate the top songs of the city charts
		for (SimpleEntry<Integer, Integer> entry : uniqueCount) {
			// Check if wanted number of recommendations is reached
			if (recommendations.size() >= howMany)
				break;

			// Check if the user has seen it before, if so DON'T add to
			// recommendation list
			if (ratedSongs.contains(entry.getKey()))
				continue;

			// If not seen search for the track in the charts
			Song topSong = null;
			String songCity = "";
			for (String city : charts.keySet()) {
				for (Song song : charts.get(city)) {
					if (song.getID() == entry.getKey()) {
						topSong = song;
						songCity = city;
						break;
					}
				}
				if (topSong != null) {
					break;
				}
			}

			// Add to the recommendation list if not already contained
			boolean duplicate = false;
			for (Recommendation recClone : recommendations) {
				if (duplicate == true) {
					break;
				}
				if (topSong.equals(recClone.getSong())) {
					duplicate = true;
				}
				if (recClone.getSong().getArtist().equals(topSong.getArtist())) {
					duplicate = true;
				}
			}
			if (!duplicate) {
				double score = (double) (1.8 / entry.getValue());
				Recommendation rec = getDataRepository()
						.getRecommendationFactory().createRecommendation(
								topSong, getID(),
								"Uniquely popular in " + songCity, score);
				recommendations.add(rec);
			}
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
