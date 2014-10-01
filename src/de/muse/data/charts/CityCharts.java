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
package de.muse.data.charts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muse.data.social.lastfm.City;
import de.muse.data.social.lastfm.LastFmConnector;
import de.muse.recommendation.MuseSong;
import de.muse.utility.Database;

/**
 * Represents a list of songs that belong to charts of a city. Charts are based
 * on last.fm data. Takes care of getting and saving these songs.
 */
public class CityCharts extends Charts {
  // Configured logger
  private static final Logger LOG = LoggerFactory.getLogger(CityCharts.class
      .getName());

  // Information about the city
  private String city;
  private String country;

  /** Construct an empty CityCharts object */
  public CityCharts() {
    this.city = "";
    this.country = "";
  }

  /**
   * List of countries from which cities should be used
   */
  public Set<String> getCountries() {
    Set<String> countries = new HashSet<String>();
    countries.add("Australia");
    countries.add("Canada");
    countries.add("France");
    countries.add("Ireland");
    countries.add("Italy");
    countries.add("Spain");
    countries.add("United%20Kingdom");
    countries.add("United%20States");

    return countries;
  }

  /**
   * Get the charts for a given city in a given country.
   * 
   * @param city
   *          The wanted city.
   * @param countrys
   *          The country the city is in.
   * 
   * */
  public void getChartsFor(String city, String country) {
    // Reset the list of songs
    clear();

    // Set up the new charts
    this.city = city;
    this.country = country;
    LOG.info("Getting charts for " + city + " in " + country + ".");
    tracks = LastFmConnector.getCityUniqueTrackChart(city, country, 20);
  }

  /**
   * Write songs that are currently saved in the member tracks to the database.
   */
  public void saveToDB() {
    if (tracks.isEmpty())
      return;
    
    // Save songs to database
    Connection conn = null;
    PreparedStatement pstmt = null;
    PreparedStatement pstmt2 = null;
    ResultSet keys = null;

    try {
      LOG.info("Saving city charts to Database.");
      conn = Database.getConnection();
      pstmt = conn.prepareStatement(
          "INSERT INTO tracks VALUES(SONGID.nextval, ?,?,?)", new int[] { 1 });

      pstmt2 = conn.prepareStatement("INSERT INTO charts_city "
          + "VALUES(?,?,?,?)");
      for (int i = 0; i < tracks.size(); i++) {
        MuseSong track = tracks.get(i);
        pstmt.setString(1, track.getName());
        pstmt.setString(2, track.getArtist());
        pstmt.setNull(3, java.sql.Types.VARCHAR);
        pstmt.executeUpdate();
        keys = pstmt.getGeneratedKeys();
        if (keys.next()) {
          track.setID(keys.getInt(1));
        }

        pstmt2.setInt(1, tracks.indexOf(track) + 1);
        pstmt2.setString(2, city);
        pstmt2.setInt(3, track.getID());
        pstmt2.setString(4, country);
        pstmt2.addBatch();
      }
      pstmt2.executeBatch();
    } catch (SQLException e) {
      LOG.warn("Couldn't save songs to database for city: " + city, e);
    } finally {
      Database.quietClose(keys);
      Database.quietClose(pstmt);
      Database.quietClose(pstmt2);
      Database.quietClose(conn);
    }
  }

  /**
   * Save Charts for all cities available on last.fm. <b>Caution:</b> This will
   * clear the corresponding database table.
   */
  public void saveChartsForAllCities() {
    Database.clearTable("charts_city");
    for (City city : LastFmConnector.getCities(this.getCountries())) {
      getChartsFor(city.getName(), city.getCountry());
      saveToDB();
    }
  }
}
