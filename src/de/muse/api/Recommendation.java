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

/**
 * <p>
 * Implementations of this interface represent a song recommendations including
 * score, explanation and song information.
 * </p>
 */
public interface Recommendation {
	/**
	 * 
	 * @return The song that is recommended
	 */
	public Song getSong();

	/**
	 * 
	 * @return ID of the recommender this comes from
	 */
	public int getRecommenderID();

	/**
	 * 
	 * @return Explanation why the song is recommended to the user
	 */
	public String getExplanation();

	/**
	 * 
	 * @return Score that was computed for the recommendation
	 */
	public double getScore();
}
