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
	// Evaluation Model Definition
	// -------------------------------
	var EvaluationModel = Backbone.Model
			.extend({
				defaults : {
					name: "",
					from : "",
					to : "",
					creator : "",
					groups : "",
					groupsDetails : "",
					created : "",
					composition : "",
					recommenders : "",
					summaryCreated : 0
				},

				// REST URL
				urlCreate : "createEvaluation",

				initialize : function() {
					this.urlCreate = Utils.serviceUrl + this.urlCreate;
					this.on("change:composition", function() {
						this.set("summaryCreated", 0);
						$("#createEvalFinal").hide();
					});
				},

				checkData : function() {
					var errors = [];
					
					// Check name
					var name = this.get("name");
					if(name.length == 0) {
						errors
						.push("Please enter a name.");
					}
					if(name.length > 20) {
						errors
						.push("Please choose a name smaller than 20 digits.");
					}

					// Check dates
					var dateFrom = new Date(this.get("from")).getTime();
					var dateTo = new Date(this.get("to")).getTime();
					var dateCreated = new Date(this.get("created")).getTime();
					if (this.get("from") === "") {
						errors.push("You didn't specify a start date.");
					} else if (dateFrom < dateCreated) {
						errors
								.push("Please choose a start date that is in the future.");
					}
					if (this.get("to") === "") {
						errors.push("You didn't specify an end date.");
					} else if (dateTo < dateFrom) {
						errors.push("Your evaluation ends before it starts.");
					}

					// Check composition
					if (this.get("composition") === ""
							|| this.get("composition").length === 0) {
						errors
								.push("Please specify group composition criteria in <b>Step 2</b>.");
					}

					// Check groups
					for (var i = 0; i < this.get("groupsDetails").length; i++) {
						var groupNum = i + 1;
						if (this.get("groupsDetails")[i].get("recommenders")) {
							if (this.get("groupsDetails")[i].get("recommenders").length === 0) {
								errors
										.push("You didn't specify recommender settings for group #"
												+ groupNum);
							}
						} else {
							errors
									.push("You didn't specify recommender settings for group #"
											+ groupNum);
						}
						if (!this.get("groupsDetails")[i].get("behavior")) {
							errors
									.push("You didn't specify behavior settings for group #"
											+ groupNum);
						}
					}

					return errors;
				},

				// Save evaluation to the database ("Create it")
				saveEvaluation : function() {
					var uri = this.urlCreate;
					var groupsTmp = this.get("groupsDetails").slice();
					for (var i = 0; i < groupsTmp.length; i++) {
						groupsTmp[i] = groupsTmp[i].attributes;
					}
					this.set("groups", groupsTmp);
					var data = JSON.stringify(this.attributes);
					return $.ajax({
						url : uri,
						type : 'PUT',
						data : data,
						contentType : "application/json"
					});
				},

				// Get evaluation summary
				getEvaluationSummary : function() {
					var uri = Utils.serviceUrl + "getEvaluationSummary";
					var groupsTmp = this.get("groupsDetails").slice();
					for (var i = 0; i < groupsTmp.length; i++) {
						groupsTmp[i] = groupsTmp[i].attributes;
					}
					this.set("groups", groupsTmp);
					var data = JSON.stringify(this.attributes);
					return $.ajax({
						url : uri,
						type : 'POST',
						data : data,
						contentType : "application/json"
					});
				}
			});
	
	return EvaluationModel;
});