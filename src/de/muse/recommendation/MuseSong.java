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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muse.api.Song;
import de.muse.utility.Database;

public class MuseSong implements Song {
	// Configured logger
	private static final Logger LOG = LoggerFactory.getLogger(MuseSong.class
			.getName());

	// Unique identifier
	private int id;

	// Track data
	private String artist;
	private String name;

	// Additional information
	private int playCount;
	private HashMap<String, Double> tags;
	private String neighbor;

	// Getter & Setter
	public int getID() {
		if (id == 0) {
			LOG.info("Querying song id.");
			// Query from database
			Connection conn = null;
			PreparedStatement stmt = null;
			ResultSet result = null;

			try {
				conn = Database.getConnection();
				stmt = conn.prepareStatement("SELECT id FROM tracks "
						+ "WHERE name = ? AND artist = ?");
				stmt.setString(1, name);
				stmt.setString(2, artist);
				result = stmt.executeQuery();

				if (result.next()) {
					id = result.getInt("id");
				}
			} catch (SQLException e) {
				LOG.warn("Couldn't get id for track: " + artist + " - " + name,
						e);
			} finally {
				Database.quietClose(conn, stmt, result);
			}
		}
		return id;
	}

	public void setID(int id) {
		this.id = id;
	}

	public String getArtist() {
		return this.artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPlayCount() {
		return this.playCount;
	}

	public void setPlayCount(int playcount) {
		this.playCount = playcount;
	}

	public HashMap<String, Double> getTags() {
		return this.tags;
	}

	public void setTags(HashMap<String, Double> tags) {
		this.tags = tags;
	}

	// Get the first tag only
	public String getTag() {
		return (String) this.tags.keySet().toArray()[0];
	}

	// Set a single tag only (Clears all other tags!)
	public void setTag(String tag) {
		this.tags = new HashMap<String, Double>();
		this.tags.put(tag, 1.0);
	}

	public String getNeighbor() {
		return this.neighbor;
	}

	public void setNeighbor(String neighbor) {
		this.neighbor = neighbor;
	}

	/**
	 * Constructor a song object by artist and name.
	 * 
	 * @param artist
	 *            The artist of the song.
	 * @param name
	 *            The name of the song.
	 * 
	 */
	public MuseSong(String artist, String name) {
		this.artist = artist;
		this.name = name;
		this.id = 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((artist == null) ? 0 : artist.hashCode());
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
		MuseSong other = (MuseSong) obj;
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
		return String.valueOf(this.getID()) + " - " + this.getName() + " - "
				+ this.getArtist();
	}

}
