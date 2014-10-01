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
package de.muse.api;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Implementations represent a repository of information about the music
 * catalog, users and their preferences.
 * </p>
 */
public interface DataRepository {
	/*
	 * Factory provider
	 */
	/**
	 * Factory to create recommendation {@link Recommendation} objects.
	 * 
	 * @return {@link RecommendationFactory}
	 */
	public RecommendationFactory getRecommendationFactory();

	/*
	 * Music catalog
	 */
	/**
	 * Get the music chart of the given year.
	 * 
	 * The index in the list represents the position in the chart.
	 * 
	 * @param year
	 *            The year out of [1960-2010]
	 * @return {@link Song} Music chart
	 */
	public List<Song> getAnnualCharts(int year);

	/**
	 * Get the music charts of the given years.
	 * 
	 * Mapping Year -> Chart. The index in the list represents the position in
	 * the chart.
	 * 
	 * @param yearStart
	 *            The year out of [1960-2010]
	 * @param yearEnd
	 *            The year out of [1960-2010]
	 * @return Mapping of Year -> List {@link Song}
	 */
	public HashMap<Integer, List<Song>> getAnnualCharts(int yearStart,
			int yearEnd);

	/**
	 * Get the music chart of the given region.
	 * 
	 * The index in the list represents the position in the chart.
	 * 
	 * @param region
	 *            The region out of [Ireland, Chile, Austria, New Zealand,
	 *            France USA, Spain, Australia, Canada, Belgium, UK,
	 *            Switzerland, Italy]
	 * @return {@link Song} Music chart
	 */
	public List<Song> getRegionalCharts(String region);

	/**
	 * Get the music charts of the given list of regions.
	 * 
	 * The index in the list represents the position in the chart.
	 * 
	 * @param regions
	 *            The regions out of [Ireland, Chile, Austria, New Zealand,
	 *            France USA, Spain, Australia, Canada, Belgium, UK,
	 *            Switzerland, Italy]
	 * @return {@link Song} Music chart
	 */
	public HashMap<String, List<Song>> getRegionalCharts(List<String> regions);

	/**
	 * Get the music chart of the given city.
	 * 
	 * The index in the list represents the position in the chart.
	 * 
	 * @param city
	 *            The city name
	 * @return {@link Song} Music chart
	 */
	public List<Song> getCityCharts(String city);

	/**
	 * Get the music charts of the given list of cities.
	 * 
	 * The index in the list represents the position in the chart.
	 * 
	 * @param cities
	 *            The city names
	 * @return {@link Song} Music chart
	 */
	public HashMap<String, List<Song>> getCityCharts(List<String> cities);

	/*
	 * General data
	 */

	/**
	 * Get list of all users.
	 * 
	 * @return List of names of the users
	 */
	public Set<String> getUsers();

	/**
	 * Get information about the user.
	 * 
	 * @param username
	 *            The name of the user
	 * @return User object including data populated fields.
	 */
	public User getUserInfo(String username);

	/**
	 * Get list of all available songs.
	 * 
	 * @return List of song IDs
	 */
	public Set<Integer> getSongIDs();

	/**
	 * Get information about the song.
	 * 
	 * @param songID
	 *            The ID of the song
	 * @return Song object including data populated fields.
	 */
	public Song getSongInfo(int songID);

	/*
	 * Specific user data
	 */
	/**
	 * Get the last.fm tag charts of the given user. Last.fm charts of tags
	 * which are most liked by the given user.
	 * 
	 * The index in the list represents the position in the chart.
	 * 
	 * @param user
	 *            The user
	 * @return Mapping of Tag -> List {@link Song}
	 */
	public HashMap<String, List<Song>> getTagCharts(User user);

	/**
	 * Get the last.fm neighbor charts of the given user. Top songs listened to
	 * by last.fm neighbors.
	 * 
	 * The index in the list represents the position in the chart.
	 * 
	 * @param user
	 *            The user
	 * @return Mapping of Neighbor -> List {@link Song}
	 */
	public HashMap<String, List<Song>> getNeighborCharts(User user);

	/**
	 * Get rated songs of the given user, meaning songs the user has already
	 * seen.
	 * 
	 * </br> <b> Important: </b> This will automatically determine if the user
	 * takes part in an Evaluation and then return the appropriate songs.
	 * 
	 * Meaning, if user takes part in an Evaluation it will only return songs
	 * seen during the Evaluation.
	 * 
	 * @param username
	 *            Name of the user
	 * @return Rated song IDs
	 */
	public Set<Integer> getRatedSongIDs(String username);

	/**
	 * Get rated tracks of the given user ordered by rating DESC.
	 * 
	 * </br> <b> Important: </b> This will automatically determine if the user
	 * takes part in an Evaluation and then return the appropriate ratings.
	 * 
	 * Meaning, if user takes part in an Evaluation it will only return ratings
	 * done during the Evaluation.
	 * 
	 * @param username
	 *            The name of the user
	 * @return Mapping of track id -> rating ordered by rating DESC
	 */
	public LinkedHashMap<Integer, Double> getRatingsFromUser(String username);
}
