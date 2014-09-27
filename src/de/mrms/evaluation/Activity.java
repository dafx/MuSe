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
package de.mrms.evaluation;

/**
 * Represents an user activity tracked in the front end.
 * 
 */
public final class Activity {
  // Time spent on a specific site of the front end
  public final double duration;
  // Site which was measured
  public final String site;
  // The name of the user
  public final String userName;

  public Activity(int time, String site, String userName) {
    this.duration = time;
    this.site = site;
    this.userName = userName;
  }

}
