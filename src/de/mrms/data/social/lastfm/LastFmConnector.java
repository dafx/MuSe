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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.mrms.config.ApplicationConfig;
import de.mrms.recommendation.MuseSong;

/**
 * Provides a crawler for the last.fm API. All the needed methods
 * (user.getTopTracks, ...) are implemented. They get an XML response and parse
 * the wanted data like songs, artist names, ... via XPATH.
 */
public class LastFmConnector {
  // Configured logger
  private static final Logger LOG = LoggerFactory.getLogger(LastFmConnector.class
      .getName());

  // API
  private static final String KEY = "&api_key=" + ApplicationConfig.LFM_API_KEY;
  private static final String PREFIX = "http://ws.audioscrobbler.com/2.0/?method=";

  private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory
      .newInstance();
  private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();

  /**
   * Get MusicBrainz ID of a song. LastFM API = getInfo.mbid
   * 
   * @param artist
   *          The name of the artist
   * @param name
   *          The name of the song
   * @return MusicBrainz ID of the song
   */
  public static String getMbid(String artist, String name) {
    String mbid = null;

    try {
      // Build URL
      String urlString = PREFIX + "track.getinfo&artist="
          + URLEncoder.encode(artist, "UTF-8") + "&track="
          + URLEncoder.encode(name, "UTF-8") + KEY;
      URL url = new URL(urlString);

      // Send request
      LOG.info("lfmQuery: " + url.toString());
      URLConnection connection = url.openConnection();
      DocumentBuilder db = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();

      // Fetch response
      final Document document = db.parse(connection.getInputStream());
      XPath xPathEvaluator = XPATH_FACTORY.newXPath();
      XPathExpression tagExpr = xPathEvaluator.compile("lfm/track/mbid");
      NodeList tagNameNodes = (NodeList) tagExpr.evaluate(document,
          XPathConstants.NODESET);
      mbid = tagNameNodes.item(0).getTextContent();

    } catch (ConnectException e) {
      LOG.warn("Connection time out for: " + name + "-" + artist);
    } catch (IOException e) {
      LOG.warn("Info NA for track: " + name + "-" + artist);
    } catch (XPathExpressionException e) {
      LOG.warn("Wrong XPath for track: " + name + "-" + artist);
    } catch (SAXException e) {
      LOG.warn("Couldn't parse response: " + name + "-" + artist);
    } catch (ParserConfigurationException e) {
      LOG.warn("Couldn't build document: " + name + "-" + artist);
    }

    return mbid;
  }
  
  /**
   * Get all cities available on lastFM. LastFM API = getMetros
   * 
   * @return List of city names
   */
  public static List<City> getCities(Set<String> countries) {
    List<City> cities = new ArrayList<City>();

    for (String country : countries) {
      String urlString = PREFIX + "geo.getmetros&country=" + country + KEY;

      try {
        // Send request
        URL url = new URL(urlString);
        LOG.info("lfmQuery: " + url.toString());
        URLConnection connection = url.openConnection();
        DocumentBuilder db = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();

        // Fetch response
        final Document document = db.parse(connection.getInputStream());
        XPath xPathEvaluator = XPATH_FACTORY.newXPath();

        XPathExpression nameExpr = xPathEvaluator
            .compile("lfm/metros/metro/name");
        NodeList nameNodes = (NodeList) nameExpr.evaluate(document,
            XPathConstants.NODESET);

        for (int j = 0; j < nameNodes.getLength(); j++) {
          String name = nameNodes.item(j).getTextContent();

          XPathExpression countryExpr = xPathEvaluator
              .compile("lfm/metros/metro[name='" + name + "']/country");
          NodeList countryNodes = (NodeList) countryExpr.evaluate(document,
              XPathConstants.NODESET);
          String region = countryNodes.item(0).getTextContent();
          cities.add(new City(name, region));
        }

      } catch (IOException e) {
        LOG.warn("Couldn't open connection to URL: " + urlString);
      } catch (ParserConfigurationException e) {
        LOG.warn("Couldn't build document for: " + urlString);
      } catch (SAXException e) {
        LOG.warn("Couldn't parse response XML file of: " + urlString);
      } catch (XPathExpressionException e) {
        LOG.warn("Wrong with XPath evaluation for: " + urlString);
      }
    }
    return cities;
  }

  /**
   * Get neighbors for a certain user. LastFM API = getNeighbours
   * 
   * @param user
   *          The name of the user
   * @param limit
   *          Number of neighbors
   * @return List of neighbor names
   */
  public static List<String> getNeighbours(String user, int limit) {
    List<String> neighbours = new ArrayList<String>();

    try {
      String urlString = PREFIX + "user.getneighbours&user="
          + URLEncoder.encode(user, "UTF-8") + KEY;
      // If limit is specified, add it to URL
      if (limit != 0)
        urlString += "&limit=" + limit;

      // Send request
      URL url = new URL(urlString);
      LOG.info("lfmQuery: " + url.toString());
      URLConnection connection = url.openConnection();
      DocumentBuilder db = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();

      // Fetch response
      final Document document = db.parse(connection.getInputStream());
      XPath xPathEvaluator = XPATH_FACTORY.newXPath();
      XPathExpression nameExpr = xPathEvaluator
          .compile("lfm/neighbours/user/name");
      NodeList nameNodes = (NodeList) nameExpr.evaluate(document,
          XPathConstants.NODESET);

      for (int j = 0; j < nameNodes.getLength(); j++) {
        neighbours.add(nameNodes.item(j).getTextContent());
      }

    } catch (UnsupportedEncodingException e) {
      LOG.warn("URL encode went wrong for: " + user);
    } catch (IOException e) {
      LOG.warn("Couldn't open connection to URL for: " + user);
    } catch (ParserConfigurationException e) {
      LOG.warn("Couldn't build document for : " + user);
    } catch (SAXException e) {
      LOG.warn("Couldn't parse response XML file for: " + user);
    } catch (XPathExpressionException e) {
      LOG.warn("Wrong with XPath evaluation for: " + user);
    }

    return neighbours;
  }

  /**
   * Get charts for a certain city. LastFM API = getMetroTrackChart
   * 
   * @param metro
   *          The name of the city
   * @param country
   *          The name of the country it is in
   * @param limit
   *          Number of wanted songs
   * @return List of songs that represent the charts of the given city
   */
  public static List<MuseSong> getCityUniqueTrackChart(String metro,
      String country, int limit) {
    List<MuseSong> cityCharts = new ArrayList<MuseSong>();

    try {
      String urlString = PREFIX + "geo.getmetrouniquetrackchart&country="
          + URLEncoder.encode(country, "UTF-8") + "&metro="
          + URLEncoder.encode(metro, "UTF-8") + KEY;
      // If limit is specified, add it to URL
      if (limit != 0)
        urlString += "&limit=" + limit;

      // Send request
      URL url = new URL(urlString);
      LOG.info("lfmQuery: " + url.toString());
      URLConnection connection = url.openConnection();
      DocumentBuilder db = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();

      // Fetch response
      final Document document = db.parse(connection.getInputStream());
      XPath xPathEvaluator = XPATH_FACTORY.newXPath();
      XPathExpression nameExpr = xPathEvaluator
          .compile("lfm/toptracks/track/name");
      NodeList trackNameNodes = (NodeList) nameExpr.evaluate(document,
          XPathConstants.NODESET);

      for (int i = 0; i < trackNameNodes.getLength(); i++) {
        Node trackNameNode = trackNameNodes.item(i);

        XPathExpression artistNameExpr = xPathEvaluator
            .compile("following-sibling::artist/name");
        NodeList artistNameNodes = (NodeList) artistNameExpr.evaluate(
            trackNameNode, XPathConstants.NODESET);

        for (int j = 0; j < artistNameNodes.getLength(); j++) {
          String artist = artistNameNodes.item(j).getTextContent();
          String track = trackNameNode.getTextContent();
          MuseSong song = new MuseSong(artist, track);
          if (!(artist.equals("<Unknown>") || track.equals("<Unknown>"))) {
            cityCharts.add(song);
          }
        }
      }

    } catch (UnsupportedEncodingException e) {
      LOG.warn("URL encode went wrong for: " + metro);
    } catch (IOException e) {
      LOG.warn("Couldn't open connection to URL for: " + metro);
    } catch (ParserConfigurationException e) {
      LOG.warn("Couldn't build document for: " + metro);
    } catch (SAXException e) {
      LOG.warn("Couldn't parse response XML file for: " + metro);
    } catch (XPathExpressionException e) {
      LOG.warn("Wrong XPath evaluation for: " + metro);
    }
    return cityCharts;
  }

  /**
   * Get top tracks for a certain user. LastFM API = getTopTracks
   * 
   * @param user
   *          The name of the user
   * @param limit
   *          Number of wanted tracks
   * @param period
   *          Period of which the TOP is computed
   * @return List of songs representing the top tracks of the given user in the
   *         given time period
   */
  public static List<MuseSong> getTopTracks(String user, int limit, String period) {
    List<MuseSong> topTracks = new ArrayList<MuseSong>();

    try {
      // Build URL
      String urlString = PREFIX + "user.gettoptracks&user="
          + URLEncoder.encode(user, "UTF-8") + KEY;
      // If limit or period is specified add it to URL
      if (limit != 0)
        urlString += "&limit=" + limit;
      if (!period.isEmpty())
        urlString += "&period=" + period;

      // Send request
      URL url = new URL(urlString);
      LOG.info("lfmQuery: " + url.toString());
      URLConnection connection = url.openConnection();
      DocumentBuilder db = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();

      // Fetch response
      final Document document = db.parse(connection.getInputStream());
      // Parse XML
      XPath xPathEvaluator = XPATH_FACTORY.newXPath();
      XPathExpression nameExpr = xPathEvaluator
          .compile("lfm/toptracks/track/name");
      NodeList trackNameNodes = (NodeList) nameExpr.evaluate(document,
          XPathConstants.NODESET);

      XPathExpression playcountExpr = xPathEvaluator
          .compile("lfm/toptracks/track/playcount");
      NodeList playcountNodes = (NodeList) playcountExpr.evaluate(document,
          XPathConstants.NODESET);

      for (int i = 0; i < trackNameNodes.getLength(); i++) {
        Node trackNameNode = trackNameNodes.item(i);
        Node playcountNode = playcountNodes.item(i);

        XPathExpression artistNameExpr = xPathEvaluator
            .compile("following-sibling::artist/name");
        NodeList artistNameNodes = (NodeList) artistNameExpr.evaluate(
            trackNameNode, XPathConstants.NODESET);

        int playcount = Integer.valueOf(playcountNode.getTextContent());
        String track = trackNameNode.getTextContent();
        String artist = "";
        for (int j = 0; j < artistNameNodes.getLength(); j++) {
          artist = artistNameNodes.item(j).getTextContent();
        }
        if (!(artist.equals("<Unknown>") || track.equals("<Unknown>"))) {
          MuseSong song = new MuseSong(artist, track);
          song.setPlayCount(playcount);
          topTracks.add(song);
        }
      }
    } catch (UnsupportedEncodingException e) {
      LOG.warn("URL encode went wrong for: " + user);
    } catch (IOException e) {
      LOG.warn("Couldn't open connection to URL for: " + user);
    } catch (ParserConfigurationException e) {
      LOG.warn("Couldn't build document for: " + user);
    } catch (SAXException e) {
      LOG.warn("Couldn't parse response XML file for: " + user);
    } catch (XPathExpressionException e) {
      LOG.warn("Wrong with XPath evaluation for: " + user);
    }
    return topTracks;
  }

  /**
   * Get top tracks for a certain tag. LastFM API = tag.getTopTracks
   * 
   * @param tag
   *          The name of the tag
   * @param limit
   *          Number of wanted tracks
   * @return List of tracks representing the top tracks of the given tag
   */
  public static List<MuseSong> getTagTopTracks(String tag, int limit) {
    List<MuseSong> topTracks = new ArrayList<MuseSong>();

    try {
      // Build URL
      String urlString = PREFIX + "tag.gettoptracks&tag="
          + URLEncoder.encode(tag, "UTF-8") + KEY;
      // If limit is specified add it to URL
      if (limit != 0)
        urlString += "&limit=" + limit;

      // Send request
      URL url = new URL(urlString);
      LOG.info("lfmQuery: " + url.toString());
      URLConnection connection = url.openConnection();
      DocumentBuilder db = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();

      // Fetch response
      final Document document = db.parse(connection.getInputStream());
      // Parse XML
      XPath xPathEvaluator = XPATH_FACTORY.newXPath();
      XPathExpression nameExpr = xPathEvaluator
          .compile("lfm/toptracks/track/name");
      NodeList trackNameNodes = (NodeList) nameExpr.evaluate(document,
          XPathConstants.NODESET);

      for (int i = 0; i < trackNameNodes.getLength(); i++) {
        Node trackNameNode = trackNameNodes.item(i);
        XPathExpression artistNameExpr = xPathEvaluator
            .compile("following-sibling::artist/name");
        NodeList artistNameNodes = (NodeList) artistNameExpr.evaluate(
            trackNameNode, XPathConstants.NODESET);

        String artist = "";
        for (int j = 0; j < artistNameNodes.getLength(); j++) {
          artist = artistNameNodes.item(j).getTextContent();
        }
        MuseSong song = new MuseSong(artist, trackNameNode.getTextContent());
        song.setTag(tag);
        topTracks.add(song);
      }
    } catch (UnsupportedEncodingException e) {
      LOG.warn("URL encode went wrong for tag: " + tag);
    } catch (IOException e) {
      LOG.warn("Couldn't open connection for tag: " + tag);
    } catch (ParserConfigurationException e) {
      LOG.warn("Couldn't build document for: " + tag);
    } catch (SAXException e) {
      LOG.warn("Couldn't parse response XML file for: " + tag);
    } catch (XPathExpressionException e) {
      LOG.warn("Wrong with XPath evaluation for: " + tag);
    }
    return topTracks;
  }

  /**
   * Get tags of a certain song. LastFM API = getTopTags
   * 
   * @param artist
   *          The name of the artist
   * @param name
   *          The name of the song
   * @param limit
   *          Number of wanted tags
   * @return List of tags for the given song
   */
  public static List<String> getTopTags(String artist, String name, int limit) {
    List<String> tags = new ArrayList<String>();

    try {
      // Build URL
      String urlString = PREFIX + "track.gettoptags&artist="
          + URLEncoder.encode(artist, "UTF-8") + "&track="
          + URLEncoder.encode(name, "UTF-8") + KEY;
      URL url = new URL(urlString);

      // Send request
      LOG.info("lfmQuery: " + url.toString());
      URLConnection connection = url.openConnection();
      DocumentBuilder db = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();

      // Fetch response
      final Document document = db.parse(connection.getInputStream());
      XPath xPathEvaluator = XPATH_FACTORY.newXPath();
      XPathExpression tagExpr = xPathEvaluator.compile("lfm/toptags/tag/name");
      NodeList tagNameNodes = (NodeList) tagExpr.evaluate(document,
          XPathConstants.NODESET);

      // Max of limit and nodes set to limit
      if (tagNameNodes.getLength() < limit)
        limit = tagNameNodes.getLength();

      for (int i = 0; i < limit; i++) {
        Node tagNameNode = tagNameNodes.item(i);
        tags.add(tagNameNode.getTextContent());
      }

    } catch (ConnectException e) {
      LOG.warn("Connection time out for: " + name + "-" + artist);
    } catch (IOException e) {
      LOG.warn("Top fans NA for track: " + name + "-" + artist);
    } catch (XPathExpressionException e) {
      LOG.warn("Wrong XPath for track: " + name + "-" + artist);
    } catch (SAXException e) {
      LOG.warn("Couldn't parse response: " + name + "-" + artist);
    } catch (ParserConfigurationException e) {
      LOG.warn("Couldn't build document: " + name + "-" + artist);
    }

    return tags;
  }

  /**
   * Get tags and their count for a given song. LastFM API = getTopTags
   * 
   * @param artist
   *          The name of the artist
   * @param name
   *          The name of the song
   * @param limit
   *          Number of wanted tags
   * @return List of tags and their count for the given song
   */
  public static HashMap<String, String> getCountTopTags(String artist,
      String name, int limit) {
    HashMap<String, String> tags = new HashMap<String, String>();

    try {
      // Build URL
      String urlString = PREFIX + "track.gettoptags&artist="
          + URLEncoder.encode(artist, "UTF-8") + "&track="
          + URLEncoder.encode(name, "UTF-8") + KEY;
      URL url = new URL(urlString);

      // Send request
      LOG.info("lfmQuery: " + url.toString());
      URLConnection connection = url.openConnection();
      DocumentBuilder db = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();

      // Fetch response
      final Document document = db.parse(connection.getInputStream());
      XPath xPathEvaluator = XPATH_FACTORY.newXPath();
      XPathExpression tagExpr = xPathEvaluator.compile("lfm/toptags/tag/name");
      XPathExpression countExpr = xPathEvaluator
          .compile("lfm/toptags/tag/count");
      NodeList tagNameNodes = (NodeList) tagExpr.evaluate(document,
          XPathConstants.NODESET);
      NodeList tagCountNodes = (NodeList) countExpr.evaluate(document,
          XPathConstants.NODESET);

      // Max of limit and nodes set to limit
      if (tagNameNodes.getLength() < limit)
        limit = tagNameNodes.getLength();

      for (int i = 0; i < limit; i++) {
        Node tagNameNode = tagNameNodes.item(i);
        Node tagCountNode = tagCountNodes.item(i);
        tags.put(tagNameNode.getTextContent(), tagCountNode.getTextContent());
      }

    } catch (ConnectException e) {
      LOG.warn("Connection time out for: " + name + "-" + artist);
    } catch (IOException e) {
      LOG.warn("Top fans NA for track: " + name + "-" + artist);
    } catch (XPathExpressionException e) {
      LOG.warn("Wrong XPath for track: " + name + "-" + artist);
    } catch (SAXException e) {
      LOG.warn("Couldn't parse response: " + name + "-" + artist);
    } catch (ParserConfigurationException e) {
      LOG.warn("Couldn't build document: " + name + "-" + artist);
    }

    return tags;
  }

  /**
   * Get a list of the most current top tracks of the given user.
   * 
   * @param user
   *          The name of the user
   * @param limit
   *          Number of wanted songs
   * @return List of songs representing the most current tracks
   */
  public static List<MuseSong> getCurrentTopTracks(String user, int limit) {
    List<String> periods = new ArrayList<String>();
    periods.add("7day");
    periods.add("1month");
    periods.add("3month");
    periods.add("6month");
    periods.add("12month");
    periods.add("");

    List<MuseSong> userTracks = new ArrayList<MuseSong>();
    int i = 0;
    while (i < periods.size() && userTracks.size() < limit) {
      for (MuseSong song : getTopTracks(user, limit, periods.get(i))) {
        if (userTracks.size() >= limit) {
          break;
        }
        song.setNeighbor(user);
        userTracks.add(song);
      }
      i++;
    }
    return userTracks;
  }

  /**
   * Check if last.fm account exists, true if it exists, false if not accessible
   * 
   * @param user
   *          The last.fm user name
   * @return True if the user name is a valid Last.fm user, false if not.
   */
  public static boolean validateAccount(String user) {
    try {
      // Build URL
      String urlString = PREFIX + "user.getinfo&user="
          + URLEncoder.encode(user, "UTF-8") + KEY;

      // Send request
      URL url = new URL(urlString);
      System.out.println("lfmQuery: " + url.toString());
      URLConnection connection = url.openConnection();
      DocumentBuilder db = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
      // Fetch response
      final Document document = db.parse(connection.getInputStream());
      XPath xPathEvaluator = XPATH_FACTORY.newXPath();
      XPathExpression nameExpr = xPathEvaluator.compile("lfm/user/name");
      NodeList userNameNodes = (NodeList) nameExpr.evaluate(document,
          XPathConstants.NODESET);

      if (userNameNodes.getLength() != 0) {
        return true;
      }

    } catch (ConnectException e) {
      LOG.warn("Connection time out for: " + user);
    } catch (UnsupportedEncodingException e) {
      LOG.warn("URL encode went wrong for: " + user);
    } catch (IOException e) {
      LOG.warn("Couldn't open connection to URL for: " + user);
    } catch (ParserConfigurationException e) {
      LOG.warn("Couldn't build document for : " + user);
    } catch (SAXException e) {
      LOG.warn("Couldn't parse response XML file for: " + user);
    } catch (XPathExpressionException e) {
      LOG.warn("Wrong with XPath evaluation for: " + user);
    }
    return false;
  }
}
