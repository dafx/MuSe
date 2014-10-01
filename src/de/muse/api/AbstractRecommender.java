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
 * Skeletal implementation of {@link Recommender}.
 * </p>
 */
public abstract class AbstractRecommender implements Recommender {
	// Injected data repository & ID
	private final DataRepository dataRepository;
	private final int ID;

	/**
	 * 
	 * @param ID
	 *            Recommender id which is later automatically assigned by the
	 *            system.
	 * @param dataRepository
	 *            A data repository providing the recommender with data
	 */
	public AbstractRecommender(int ID, DataRepository dataRepository) {
		this.ID = ID;
		this.dataRepository = dataRepository;
	}

	@Override
	public final int getID() {
		return ID;
	}

	/**
	 * 
	 * @return The data repository of this recommender
	 */
	public final DataRepository getDataRepository() {
		return dataRepository;
	}

	@Override
	public abstract List<Recommendation> getRecommendations(User user,
			int howMany);

	@Override
	public abstract String getName();

	@Override
	public abstract String getExplanation();

	@Override
	public abstract Map<String, Double> getTagDistribution();
}
