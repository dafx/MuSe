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
package de.mrms.data.charts;

import java.util.ArrayList;
import java.util.List;

import de.mrms.recommendation.MuseSong;

/**
 * Representing an arbitrary charts object. Implementing print and clear methods
 * and declaring the song list object.
 */
public class Charts {
	// Chart types active in the system
	public static final String[] TYPES = new String[]{"City", "Neighbor", "Region", "Tag"};
	// List of songs where position in list = position in charts
	protected List<MuseSong> tracks;

	public Charts() {
		this.tracks = new ArrayList<MuseSong>();
	}

	/** Clears the list of songs */
	protected void clear() {
		tracks.clear();
	}
}
