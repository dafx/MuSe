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

import java.util.List;
import java.util.Map;

/**
 * <p>
 * Implementations of this interface can recommend items for users.
 * </p>
 */
public interface Recommender {
	/**
	 * 
	 * @return The name of the recommender
	 */
	public String getName();

	/**
	 * 
	 * @return An explanation how the recommender works
	 */
	public String getExplanation();

	/**
	 * 
	 * @return ID of the recommender
	 */
	public int getID();

	/** Get the tag distribution of the recommender */
	/**
	 * 
	 * @return Characteristics of the recommender [Accuracy, Novelty,
	 *         Serendipity, Diversity]
	 */
	public Map<String, Double> getTagDistribution();

	/**
	 * Get specified number of recommendations for the given user.
	 * 
	 * <br>
	 * Make sure you to <b>avoid duplicates both local and global</b>. Local
	 * meaning in the returned list and global using
	 * {@link DataRepository#getRatedSongIDs(String)}.
	 * 
	 * @param user
	 *            The user to create recommendations for
	 * @param howMany
	 *            The number of recommendations to create
	 * @return List of recommendations
	 */
	public List<Recommendation> getRecommendations(User user, int howMany);
}