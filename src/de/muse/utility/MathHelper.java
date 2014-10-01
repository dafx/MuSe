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
package de.muse.utility;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Provides some mathematical methods.
 */
public class MathHelper {

  /**
   * Get the sum of a list of doubles
   * 
   * @param scores
   * @return Sum of all double values in the given list
   */
  public static double getSumOfDoubles(List<Double> scores) {
    double sum = 0;
    for (double score : scores) {
      sum += score;
    }
    return sum;
  }

  /**
   * Get the sum of a list of integers
   * 
   * @param scores
   * @return Sum of all values in the given list
   */
  public static int getSumOfInts(List<Integer> ints) {
    int sum = 0;
    for (double num : ints) {
      sum += num;
    }
    return sum;
  }

  /**
   * Sum up list of doubles, ignores doubles that are negative or zero
   * 
   * @param scores
   *          List of double values
   * @return Sum of all double greater than 0
   */
  public static double getSumOfPositiveDoubles(List<Double> scores) {
    double sum = 0;
    for (double score : scores) {
      if (score > 0) {
        sum += score;
      }
    }
    return sum;
  }

  /**
   * Get percentage of a number for a certain value
   * 
   * @param value
   *          Wanted percentage
   * @param number
   *          Percentage of number
   * @return The percentage of value from number
   */
  public static double getPercentage(double value, double number) {
    double percentage = value / number;
    return percentage;
  }

  /**
   * Compute the pearson correlation between two rating vectors.
   * 
   * @param vectorOne
   *          Vector represented as HashMap of int (trackID) - double (count).
   * @param vectorTwo
   *          Vector represented as HashMap of Sint (trackID) - double (count).
   * @return The pearson correlation of the two vectors.
   */
  public static double computePearsonCorrelation(
      HashMap<Integer, Double> vectorOne, HashMap<Integer, Double> vectorTwo) {

    // Adapt vectors to match pearson correlation => x - mean(x)
    vectorOne = MathHelper.computeVectorMinusMean(vectorOne);
    vectorTwo = MathHelper.computeVectorMinusMean(vectorTwo);

    // Compute scalar and check if it is zero
    double scalar = computeScalarProduct(vectorOne, vectorTwo);
    if (scalar == 0)
      return 0;

    // Compute vector lengths
    double lengthOne = computeVectorLength(vectorOne.values());
    double lengthTwo = computeVectorLength(vectorTwo.values());

    // Check for zero length vector
    if (lengthOne == 0 || lengthTwo == 0)
      return 0;

    // Compute the actual distance
    double distance = scalar / (lengthOne * lengthTwo);
    return distance;
  }

  // Compute mean of vector values
  private static <T> double computeVectorMean(HashMap<T, Double> vector) {
    double mean = 0;
    // Compute sum of all contained values
    double sum = 0;
    for (double value : vector.values()) {
      sum += value;
    }

    // Compute mean of vector values
    mean = sum / vector.values().size();
    return mean;
  }


  // Compute a new vector out of a vector by x - mean for each value x of the
  // vector
  private static <T> HashMap<T, Double> computeVectorMinusMean(
      HashMap<T, Double> vector) {
    HashMap<T, Double> vectorNew = new HashMap<T, Double>();
    // Get the mean of the given vector
    double mean = computeVectorMean(vector);

    // Produce new vector
    for (T key : vector.keySet()) {
      // Compute new value
      double valueNew = vector.get(key) - mean;

      // Assign new value to the vector
      vectorNew.put(key, valueNew);
    }

    return vectorNew;
  }

  /**
   * Compute the cosine distance between two tags vectors.
   * 
   * @param vectorOne
   *          Vector represented as HashMap of String (tag) - double (count).
   * @param vectorTwo
   *          Vector represented as HashMap of String (tag) - double (count).
   * @return The cosine distance
   */
  public static double computeCosineDistance(HashMap<String, Double> vectorOne,
      HashMap<String, Double> vectorTwo) {

    // Compute scalar and check if it is zero
    double scalar = computeScalarProduct(vectorOne, vectorTwo);
    if (scalar == 0)
      return 0;

    // Compute vector lengths
    double lengthOne = computeVectorLength(vectorOne.values());
    double lengthTwo = computeVectorLength(vectorTwo.values());

    // Check for zero length vector
    if (lengthOne == 0 || lengthTwo == 0)
      return 0;

    // Compute the actual distance
    double distance = scalar / (lengthOne * lengthTwo);
    return distance;
  }

  /**
   * Compute the scalar product of two vectors
   * 
   * @param vectorOne
   *          List representing vector one
   * @param vectorTwo
   *          List representing vector two
   * @return Scalar product as double or 0 if the input was invalid.
   */
  public static <T> double computeScalarProduct(HashMap<T, Double> vectorOne,
      HashMap<T, Double> vectorTwo) {

    double scalar = 0;
    for (T key : vectorOne.keySet()) {
      // Check for same key and only compute if a similar entry is found
      // otherwise the product would be zero and therefore no computation
      // is needed
      if (!vectorTwo.containsKey(key))
        continue;

      // Compute scalar if key is found in both vectors
      double valueOne = vectorOne.get(key);
      double valueTwo = vectorTwo.get(key);
      scalar += valueOne * valueTwo;
    }

    return scalar;
  }

  /**
   * Compute the length of a vector
   * 
   * @param vector
   *          List representing the vector
   * @return The length of the vector
   */
  public static double computeVectorLength(Collection<Double> vector) {
    double length = 0;
    for (Double value : vector) {

      double squared = Math.pow(value, 2);
      length += squared;
    }
    return Math.sqrt(length);
  }
}
