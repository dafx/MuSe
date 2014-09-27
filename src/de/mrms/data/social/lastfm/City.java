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
package de.mrms.data.social.lastfm;

/**
 * Represents a city with a name and a country it is in.
 */
public class City {
	// Name of the city
	private String name;
	// Name of the country the city belongs to
	private String country;

	/**
	 * 
	 * Get the country the city belongs to.
	 * 
	 * @return Name of the country.
	 */
	public String getCountry() {
		return this.country;
	}

	/**
	 * 
	 * Get the name of the city.
	 * 
	 * @return Name of the city.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Creates a new city object given name and country the city is in
	 * 
	 * @param name
	 *            Name of the city.
	 * @param country
	 *            The country the city is in.
	 * 
	 */
	public City(String name, String country) {
		this.name = name;
		this.country = country;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((country == null) ? 0 : country.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		City other = (City) obj;
		if (country == null) {
			if (other.country != null)
				return false;
		} else if (!country.equals(other.country))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
