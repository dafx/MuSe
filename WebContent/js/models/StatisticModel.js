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
	// StatisticModel Definition
	// ----------------------------------------
	var StatisticModel = Backbone.Model.extend({
		// Model attributes
		defaults : {
			from : "none",
			to : "none"
		},

		// REST URL
		url : "getStatistics",

		initialize : function() {
			this.url = Utils.serviceUrl + this.url + "/from/" + 
				this.get("from") + "/to/" + this.get("to");
		}
	});
	
	return StatisticModel;
});