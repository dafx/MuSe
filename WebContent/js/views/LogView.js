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
define([
  'jquery',
  'underscore',
  'backbone',
  'utils'
], function($, _, Backbone, Utils){
	// LogView Definition
	// ----------------------
	var LogView = Backbone.View.extend({
		// Anchor element
		el : '#logview',
		anchor: '#logContent',
		logEntries: "",

		initialize : function() {
			this.fetchLog();
		},

		// Render the content to the html node
		render : function() {
			$(this.anchor).html(this.logEntries);
		},

		// Get the logfile from the server
		fetchLog : function() {
			var self = this;
			$.getJSON(Utils.serviceUrl + "getLog", function(data) {
				self.logEntries = data;
				self.render();
			});
		}
	});
	
	return LogView;
});