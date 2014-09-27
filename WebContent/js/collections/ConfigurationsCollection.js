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
define([ 'jquery', 'underscore', 'backbone', 'utils',
		'models/ConfigurationModel' ], function($, _, Backbone, Utils,
		ConfigurationModel) {
	// ConfigurationsCollection Definition
	// ----------------------------------------
	var ConfigurationsCollection = Backbone.Collection.extend({
		// The corresponding model
		model : ConfigurationModel,

		// REST URL
		url : "getConfig",

		initialize : function() {
			this.url = Utils.serviceUrl + this.url;
			this.comparator = function(first, second) {
				if (first.id > second.id) {
					return -1;
				} else {
					return 1;
				}
			};
		},

		updateConfig : function(config) {
			return $.ajax({
				url : Utils.serviceUrl + "updateConfig",
				type : 'PUT',
				data : config
			});
		},

		// Remove recommender
		removeRecommender : function(recid) {
			return $.ajax({
				url : Utils.serviceUrl + "removeRecommender",
				type : 'POST',
				data : recid,
				contentType : "text/plain"
			});
		},

		// Update recommender
		updateRecommender : function(formData) {
			return $.ajax({
				url : Utils.serviceUrl + "updateRecommender",
				type : 'POST',
				data : formData,
				contentType : false,
				processData : false
			});
		},

		addRecommender : function(formData) {
			return $.ajax({
				url : Utils.serviceUrl + "addRecommender",
				type : 'POST',
				data : formData,
				contentType : false,
				processData : false
			});
		}
	});
	return ConfigurationsCollection;
});
