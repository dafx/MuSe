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

import de.muse.api.AbstractRecommender;
import de.muse.api.DataRepository;

/**
 * Abstract charts recommender, providing additional music charts ranking
 * methods.
 * 
 */
abstract class ChartsRecommender extends AbstractRecommender {
	public ChartsRecommender(int ID, DataRepository dataRepository) {
		super(ID, dataRepository);
	}

	/**
	 * Compute recommendation score by position in charts.
	 * 
	 * @param chartsPosition
	 *            The position in the charts.
	 * @return The computed score.
	 */
	protected static double computeScoreByPosition(int chartsPosition) {
		double score = (double) 0.025 * (66 - chartsPosition);
		if (score < 0) {
			return 0;
		}
		return score;
	}
}
