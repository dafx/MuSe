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
package de.muse.api.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import de.muse.api.DataRepository;
import de.muse.api.Recommendation;
import de.muse.api.RecommendationFactory;
import de.muse.api.Song;
import de.muse.api.User;

public class TestRepository implements DataRepository {

	@Override
	public RecommendationFactory getRecommendationFactory() {
		return new TestRecommendationFactory();
	}

	@Override
	public List<Song> getAnnualCharts(int year) {
		List<Song> charts = new ArrayList<Song>();
		for (int i = 0; i < 20; i++) {
			charts.add(new TestSong(i, "Artist" + i % 5, "Song" + i));
		}
		return charts;
	}

	@Override
	public User getUserInfo(String userName) {
		if (userName.equals("User0")) {
			return new TestUser(userName, 1969, "male", "Test", Arrays.asList(
					"English", "German"), Arrays.asList("USA", "UK", "Germany",
					"Austria", "Australia"), Arrays.asList("Washington",
					"London", "Berlin", "Vienna", "Sydney"));
		} else if (userName.equals("User1")) {
			return new TestUser(userName, 1986, "male", "Test", Arrays.asList(
					"English", "Spanish"), Arrays.asList("USA", "UK",
					"Australia", "Spain"), Arrays.asList("Washington",
					"London", "Sydney", "Barcelona"));
		} else if (userName.equals("User2")) {
			return new TestUser(userName, 1994, "male", "Test", Arrays.asList(
					"English", "German", "French"), Arrays.asList("USA", "UK",
					"Germany", "Austria", "Australia", "France", "Canada"),
					Arrays.asList("Washington", "London", "Berlin", "Vienna",
							"Sydney", "Paris", "Ottawa"));
		} else if (userName.equals("User3")) {
			return new TestUser(userName, 1962, "male", "Test", Arrays.asList(
					"English", "German"), Arrays.asList("USA", "UK", "Germany",
					"Austria", "Australia"), Arrays.asList("Washington",
					"London", "Berlin", "Vienna", "Sydney"));
		} else if (userName.equals("User4")) {
			return new TestUser(userName, 1954, "male", "Test", Arrays.asList(
					"English", "German", "Spanish"), Arrays.asList("USA", "UK",
					"Germany", "Austria", "Australia", "Spain"), Arrays.asList(
					"Washington", "London", "Berlin", "Vienna", "Sydney",
					"Barcelona"));
		} else if (userName.equals("User5")) {
			return new TestUser(userName, 1988, "male", "Test", Arrays.asList(
					"English", "German"), Arrays.asList("USA", "UK", "Germany",
					"Austria", "Australia"), Arrays.asList("Washington",
					"London", "Berlin", "Vienna", "Sydney"));
		} else if (userName.equals("User6")) {
			return new TestUser(userName, 19760, "male", "Test", Arrays.asList(
					"English", "German"), Arrays.asList("USA", "UK", "Germany",
					"Austria", "Australia"), Arrays.asList("Washington",
					"London", "Berlin", "Vienna", "Sydney"));
		} else if (userName.equals("User7")) {
			return new TestUser(userName, 2002, "male", "Test",
					Arrays.asList("English"), Arrays.asList("USA", "UK",
							"Germany", "Austria", "Australia"), Arrays.asList(
							"Washington", "London", "Berlin", "Vienna",
							"Sydney"));
		} else if (userName.equals("User8")) {
			return new TestUser(userName, 1990, "male", "Test",
					Arrays.asList("English"), Arrays.asList("USA", "UK",
							"Australia"), Arrays.asList("Washington", "London",
							"Sydney"));
		} else if (userName.equals("User9")) {
			return new TestUser(userName, 1970, "male", "Test", Arrays.asList(
					"English", "German"), Arrays.asList("USA", "UK", "Germany",
					"Austria", "Australia"), Arrays.asList("Washington",
					"London", "Berlin", "Vienna", "Sydney"));
		}
		return new TestUser(userName, 1960, "male", "Test", Arrays.asList(
				"English", "German"), Arrays.asList("USA", "UK", "Germany",
				"Austria", "Australia"), Arrays.asList("Washington", "London",
				"Berlin", "Vienna", "Sydney"));
	}

	// Recommendation Factory implementation
	public static class TestRecommendationFactory implements
			RecommendationFactory {
		public Recommendation createRecommendation(Song song,
				int recommenderID, String explanation, double score) {
			TestRecommendation rec = new TestRecommendation();
			rec.setSong(song);
			rec.setScore(score);
			rec.setExplanation(explanation);
			rec.setRecommenderID(recommenderID);
			return rec;
		}

		// Private recommendation implementation
		private class TestRecommendation implements Recommendation,
				Comparable<TestRecommendation> {
			// The recommendation attributes
			private String id;
			private Song song;
			private int recommenderID;
			private String explanation;
			private double score;

			// Getter & setter
			public Song getSong() {
				return song;
			}

			public void setSong(Song song) {
				this.song = song;
			}

			public int getRecommenderID() {
				return recommenderID;
			}

			public void setRecommenderID(int recommenderID) {
				this.recommenderID = recommenderID;
			}

			public String getExplanation() {
				return explanation;
			}

			public void setExplanation(String explanation) {
				this.explanation = explanation;
			}

			public double getScore() {
				return score;
			}

			public void setScore(double score) {
				this.score = score;
			}

			/** Create a recommendation object */
			public TestRecommendation() {
			}

			@Override
			public String toString() {
				return "ID: " + id + " Song(" + song.toString() + ") Metric: "
						+ recommenderID + " Explanation: " + explanation
						+ " Score: " + score;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result
						+ ((this.song == null) ? 0 : this.song.hashCode());
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
				TestRecommendation other = (TestRecommendation) obj;
				if (this.song == null) {
					if (other.song != null)
						return false;
				} else if (!this.song.equals(other.song))
					return false;
				return true;
			}

			@Override
			public int compareTo(TestRecommendation recAlt) {
				return new Double(this.getScore()).compareTo(new Double(recAlt
						.getScore()));
			}
		}
	}

	// Private user implementation
	private class TestUser implements User {
		// User information
		private String name;
		private int birthyear;
		private String sex;
		private String lfmaccount;
		private List<String> langs;
		private List<String> countries;
		private List<String> cities;

		public TestUser(String name, int birthyear, String sex,
				String lfmaccount, List<String> langs, List<String> countries,
				List<String> cities) {
			this.name = name;
			this.birthyear = birthyear;
			this.sex = sex;
			this.lfmaccount = lfmaccount;
			this.langs = langs;
			this.countries = countries;
			this.cities = cities;
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the birthyear
		 */
		public int getBirthyear() {
			return birthyear;
		}

		/**
		 * @return the sex
		 */
		public String getSex() {
			return sex;
		}

		/**
		 * @return the lfmaccount
		 */
		public String getLfmaccount() {
			return lfmaccount;
		}

		/**
		 * @return the langs
		 */
		public List<String> getLangs() {
			return langs;
		}

		@Override
		public List<String> getCountries() {
			return countries;
		}

		@Override
		public List<String> getCities() {
			return cities;
		}
	}

	// Private song implementation
	private class TestSong implements Song {
		private String artist, name;
		private int ID;

		private TestSong(int ID, String artist, String name) {
			this.artist = artist;
			this.name = name;
			this.ID = ID;
		}

		@Override
		public int getID() {
			return ID;
		}

		@Override
		public String getArtist() {
			return artist;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((artist == null) ? 0 : artist.hashCode());
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
			TestSong other = (TestSong) obj;
			if (artist == null) {
				if (other.artist != null)
					return false;
			} else if (!artist.equals(other.artist))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return String.valueOf(this.getID()) + " - " + this.getName()
					+ " - " + this.getArtist();
		}
	}

	@Override
	public HashMap<Integer, List<Song>> getAnnualCharts(int yearStart,
			int yearEnd) {
		HashMap<Integer, List<Song>> charts = new HashMap<Integer, List<Song>>();
		for (int year = yearStart; year <= yearEnd; year++) {
			List<Song> songs = new ArrayList<Song>();
			for (int i = 0; i < 20; i++) {
				songs.add(new TestSong(i, "Artist" + i % 5, "Song" + i));
			}
			charts.put(year, songs);
		}
		return charts;
	}

	@Override
	public List<Song> getRegionalCharts(String region) {
		return getAnnualCharts(1990);
	}

	@Override
	public HashMap<String, List<Song>> getRegionalCharts(List<String> regions) {
		HashMap<String, List<Song>> charts = new HashMap<String, List<Song>>();
		for (String region : regions) {
			List<Song> songs = new ArrayList<Song>();
			for (int i = 0; i < 20; i++) {
				songs.add(new TestSong(i, "Artist" + i % 5, "Song" + i));
			}
			charts.put(region, songs);
		}
		return charts;
	}

	@Override
	public List<Song> getCityCharts(String city) {
		return getAnnualCharts(1990);
	}

	@Override
	public HashMap<String, List<Song>> getCityCharts(List<String> cities) {
		return getRegionalCharts(cities);
	}

	@Override
	public Set<String> getUsers() {
		Set<String> users = new HashSet<String>();
		for (int i = 0; i < 10; i++) {
			users.add("User" + i);
		}
		return users;
	}

	@Override
	public Set<Integer> getSongIDs() {
		Set<Integer> songs = new HashSet<Integer>();
		for (int i = 0; i < 100; i++) {
			songs.add(i);
		}
		return songs;
	}

	@Override
	public Song getSongInfo(int songID) {
		return new TestSong(songID, "Artist" + songID % 5, "Song" + songID);
	}

	@Override
	public HashMap<String, List<Song>> getTagCharts(User user) {
		List<String> tags = Arrays.asList("Rock", "Pop", "Rap", "Indie",
				"Country");
		HashMap<String, List<Song>> charts = new HashMap<String, List<Song>>();
		for (String tag : tags) {
			List<Song> songs = new ArrayList<Song>();
			for (int i = 0; i < 20; i++) {
				songs.add(new TestSong(i, "Artist" + i % 5, "Song" + i));
			}
			charts.put(tag, songs);
		}
		return charts;
	}

	@Override
	public HashMap<String, List<Song>> getNeighborCharts(User user) {
		List<String> users = Arrays.asList("Neighbor1", "Neighbor2",
				"Neighbor3", "Neighbor4", "Neighbor5");
		HashMap<String, List<Song>> charts = new HashMap<String, List<Song>>();
		for (String neighbor : users) {
			List<Song> songs = new ArrayList<Song>();
			for (int i = 0; i < 20; i++) {
				songs.add(new TestSong(i, "Artist" + i % 5, "Song" + i));
			}
			charts.put(neighbor, songs);
		}
		return charts;
	}

	@Override
	public Set<Integer> getRatedSongIDs(String username) {
		Set<Integer> songs = new HashSet<Integer>();
		for (int i = 1; i <= 5; i++) {
			songs.add(i);
		}
		return songs;
	}

	@Override
	public LinkedHashMap<Integer, Double> getRatingsFromUser(String username) {
		LinkedHashMap<Integer, Double> ratings = new LinkedHashMap<Integer, Double>();
		ratings.put(1, 2.0);
		ratings.put(2, 2.0);
		ratings.put(3, 1.0);
		ratings.put(4, -1.0);
		ratings.put(5, -1.0);
		return ratings;
	}
}
