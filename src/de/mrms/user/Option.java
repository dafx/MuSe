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
package de.mrms.user;

import java.util.ArrayList;

/**
 * Container objects for recommendation settings of a user.
 */

public class Option {
	private String behavior;
	private ArrayList<Integer> recommenders;

	public String getBehavior() {
		return behavior;
	}

	public void setBehavior(String behavior) {
		this.behavior = behavior;
	}

	public ArrayList<Integer> getRecommenders() {
		return recommenders;
	}

	public void setRecommenders(ArrayList<Integer> recommenders) {
		this.recommenders = recommenders;
	}

	public Option(String behavior, ArrayList<Integer> recommenders) {
		this.behavior = behavior;
		this.recommenders = recommenders;
	}
}
