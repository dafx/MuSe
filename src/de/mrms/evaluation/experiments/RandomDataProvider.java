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
package de.mrms.evaluation.experiments;

import java.util.ArrayList;
import java.util.Arrays;
import java.sql.Date;
import java.util.List;
import java.util.Random;

/**
 * Create random data for different experiments.
 * 
 * @author Sven
 * 
 */
public class RandomDataProvider {
  private static Random rndFactory = new Random();

  protected static int getRandomTrackRating() {
    return createRandomInt(-1, 2);
  }

  protected static int getRandomListRating() {
    return createRandomInt(1, 5);
  }

  protected static double getRandomPredictionScore() {
    return createRandomDouble(-1, 2);
  }

  protected static int getRandomBirthyear() {
    return createRandomInt(1950, 2000);
  }

  protected static String getRandomGender() {
    String[] genders = new String[] { "Male", "Female" };
    return genders[createRandomInt(0, 1)];
  }

  public static List<String> getRandomLanguages() {
    // Available languages
    List<String> languages =  new ArrayList<String>(
        Arrays.asList("German", "French", "Spanish",
            "Italian"));
    
    // How many languages to select
    int i = createRandomInt(0, 4);
    
    // Select them and return resulting list
    List<String> rndLangs = new ArrayList<String>();
    rndLangs.add("English");
    for(int k = 0; k < i; k++) {
      int l = createRandomInt(0, languages.size() -1);
      rndLangs.add(languages.get(l));
      languages.remove(l);
    }
    return rndLangs;
  }
  
  protected static Date getRandomDate(long startDate, long endDate) {
    return new Date(createRandomLong(startDate, endDate) * 1000);
  }

  protected static int createRandomInt(int low, int high) {
    int random = rndFactory.nextInt(high - low + 1) + low;
    return random;
  }

  protected static double createRandomDouble(double low, double high) {
    double random = low + (high - low) * rndFactory.nextDouble();
    return random;
  }
  
  private static long createRandomLong(long low, long high) {
    long random = (long)(low + (high - low) * rndFactory.nextDouble());
    return random;
  }
}
