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
package de.muse.evaluation.result;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import au.com.bytecode.opencsv.CSVWriter;

public class FileProvider {
  private String filePath;

  /**
   * Create a file at the corresponding path.
   * 
   * @param filePath
   */
  public FileProvider(String filePath) {
    this.filePath = filePath;
  }

  /**
   * Create CSV file out of the content of the result.
   * 
   * @param result
   *          Query result set.
   * @throws SQLException
   */
  public void resultToCSV(ResultSet result) throws IOException, SQLException {
    CSVWriter writer = new CSVWriter(new FileWriter(filePath), ';',
        CSVWriter.NO_QUOTE_CHARACTER);
    writer.writeAll(result, true);
    writer.close();
  }

  /**
   * Create CSV file out of the content of the entries array.
   * 
   * @param entries
   *          List of entry arrays. Each array represents one entry in the file.
   * @throws SQLException
   */
  public void arrayToCSV(ArrayList<String[]> entries) throws IOException,
      SQLException {
    CSVWriter writer = new CSVWriter(new FileWriter(filePath), ',',
        CSVWriter.NO_QUOTE_CHARACTER);
    for (String[] entry : entries) {
      writer.writeNext(entry);
    }
    writer.close();
  }
}
