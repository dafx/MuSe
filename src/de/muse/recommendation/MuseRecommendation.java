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
package de.muse.recommendation;

import de.muse.api.Recommendation;
import de.muse.api.Song;

/**
 * Represents a recommendation consisting of a id, a song that is recommended
 * and a metric that recommends the included song.
 */
public class MuseRecommendation implements Comparable<MuseRecommendation>,
		Recommendation {
	// The recommendation attributes
	private String id;
	private Song song;
	private int recommenderID;
	private String explanation;
	private double score;

	// Getter & setter
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

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
	public MuseRecommendation() {
	}

	@Override
	public String toString() {
		return "ID: " + id + " Song(" + song.toString() + ") Metric: "
				+ recommenderID + " Explanation: " + explanation + " Score: "
				+ score;
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
		MuseRecommendation other = (MuseRecommendation) obj;
		if (this.song == null) {
			if (other.song != null)
				return false;
		} else if (!this.song.equals(other.song))
			return false;
		return true;
	}

	@Override
	public int compareTo(MuseRecommendation recAlt) {
		return new Double(this.getScore()).compareTo(new Double(recAlt
				.getScore()));
	}
}
