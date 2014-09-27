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
package de.mrms.api;

import java.util.List;

/**
 * <p>
 * Implementations of this interface represent a user and its information.
 * </p>
 */
public interface User {
	/**
	 * @return the name
	 */
	public String getName();

	/**
	 * @return the birthyear
	 */
	public int getBirthyear();

	/**
	 * @return the sex
	 */
	public String getSex();

	/**
	 * @return the lfmaccount
	 */
	public String getLfmaccount();

	/**
	 * @return the langs
	 */
	public List<String> getLangs();

	/**
	 * @return the countries
	 */
	public List<String> getCountries();

	/**
	 * @return the countries
	 */
	public List<String> getCities();

}
