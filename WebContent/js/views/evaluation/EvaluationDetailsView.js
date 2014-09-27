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
  'utils',
  'highcharts',
  'models/evaluation/EvaluationDetailsModel',
  'text!templates/evaluation/details.html',
  'text!templates/evaluation/groupDetails.html',
  'text!templates/evaluation/recResults.html',
  'text!templates/evaluation/userResults.html'
], function($, _, Backbone, Utils, Highcharts, EvaluationDetailsModel, 
		EvaluationDetailsTemplate, EvaluationGroupDetailsTemplate, EvaluationRecResults, EvaluationUserResults){
	// Evaluation Details View Definition
	// -------------------
	var EvaluationDetailsView = Backbone.View
			.extend({
				// Anchor element
				el : '#evaldetailsview',
				anchor : '#evaldetailsContent',
				sending : false,
				userResults : false,
				recResults : false,

				// Initialize View
				initialize : function(args) {
					// Get the template
					this.template = _.template(EvaluationDetailsTemplate);
					this.participantsTemplate = _.template(EvaluationGroupDetailsTemplate);
					this.resultsRecTemplate = _.template(EvaluationRecResults);
					this.resultsUserTemplate = _.template(EvaluationUserResults);

					// Get details
					var self = this;
					this.model = new EvaluationDetailsModel({
				          id : args.id
			        });
					this.model.on("fetch", this.loading, this);
					this.model
							.on(
									"request",
									function(model, xhr, opts) {
										$
												.when(xhr)
												.done(
														function(result) {
															var fromDate = new Date(
																	self.model
																			.get("from"));
															var toDate = new Date(
																	self.model
																			.get("to"));
															var duration = (toDate - fromDate)
																	/ (1000 * 60 * 60 * 24);
															self.model.set(
																	"duration",
																	duration + 1);

															$
																	.when(
																			Utils.getRecommenderList())
																	.done(
																			function(
																					result) {
																				self.model
																						.set(
																								"recommenderNames",
																								result);
																				self.render();
																			})
																	.fail(
																			function(
																					jqXHR,
																					textStatus,
																					errorThrown) {
																				Utils.error(
																								"evaldetailsContent",
																								jqXHR);
																			});
														})
												.fail(
														function(jqXHR, textStatus,
																errorThrown) {
															Utils.error(
																			"evaldetailsContent",
																			jqXHR);
														});
									});

					// Fetch recommendations from server
					this.model.fetch();
				},

				// Initialize event listeners
				events : {
					"change #numGroup" : "renderGroupDetails",
					"click #evalChangeBtn" : function() {
						$('#detailsTabs a:last').tab('show');
					},
					"click #evalDeleteBtn" : "deleteEval",
					"click #resultsRecTabBtn" : "getRecResults",
					"click #resultsUserTabBtn" : "getUserResults",
					"click #changeEvalDurationButton" : "changeEval",
					"click #addEvalGroupButton" : "adaptDialog",
					"click #changeEvalGroupButton" : "adaptDialog",
					"click #changeEvalGroupSubmit" : "changeGroups",
					"click #deleteEvalGroupButton" : "deleteGroup",
					"click .participantMoveButton" : "participantActionMove",
					"click .participantDeleteButton" : "participantActionDelete",
					"click #participantActionDelete" : "deleteParticipant",
					"click #participantActionMove" : "moveParticipant"
				},

				// Render the content to the html node
				render : function() {
					// Render template
					$(this.anchor).html(this.template(this.model.toJSON()));
					this.renderGroupDetails();
					return this;
				},

				// Render details of a selected group
				renderGroupDetails : function() {
					var num = $("#numGroup").val() - 1;
					if (num < 0)
						return;

					var group = this.model.get("groups")[num];
					var recommenderNames = this.model.get("recommenderNames");
					$("#groupDetails").html(this.participantsTemplate({
						numGroup : num,
						group : group,
						recommenderNames : recommenderNames
					}));
					var participants = group.participants;
					num++;
					_
							.each(
									participants,
									function(user) {
										$("#groupParticipants" + num)
												.append(
														"<tr><td>"
																+ user.name
																+ "</td>"
																+ "<td>"
																+ user.birthyear
																+ "</td>"
																+ "<td>"
																+ user.sex
																+ "</td>"
																+ "<td>"
																+ user.langs
																+ "</td><td>"
																+ user.newcomerRatings
																+ "</td><td><div data-name='"
																+ user.name
																+ "' class='participantMoveButton btn btn-inline btn-default btn-xs'><i class='fa fa-reply'></i></div><div data-name='"
																+ user.name
																+ "' class='participantDeleteButton btn btn-inline btn-danger btn-xs'><i class='fa fa-times'></i></div></td></tr>");
									});
				},

				getRecResults : function() {
					// Check if data has already been received
					if (this.recResults === true)
						return;

					// Get data from server
					Utils.spinner("evalRecAnalysis");
					var self = this;
					$.when(this.model.getRecResults()).done(function(result) {
						self.renderRecResults(JSON.parse(result));
						self.sending = false;
					}).fail(function(jqXHR, textStatus, errorThrown) {
						Utils.error("evalRecAnalysis", jqXHR);
						self.sending = false;
					});
				},

				getUserResults : function() {
					// Check if data has already been received
					if (this.userResults === true)
						return;

					// Get data from server
					Utils.spinner("evalUserAnalysis");
					var self = this;
					$.when(this.model.getUserResults()).done(function(result) {
						self.renderUserResults(JSON.parse(result));
						self.sending = false;
					}).fail(function(jqXHR, textStatus, errorThrown) {
						Utils.error("evalUserAnalysis", jqXHR);
						self.sending = false;
					});
				},

				adaptDialog : function(event) {
					if (event.target.id.indexOf("add") != -1) {
						$("#changeEvalGroupTitle").html("Add Group");
						this.model.set("changeAction", "add");
					} else if (event.target.id.indexOf("change") != -1) {
						$("#changeEvalGroupTitle").html("Change Group Settings");
						this.model.set("changeAction", "change");
					}
				},

				changeGroups : function() {
					// Avoid multiple firing of one click
					if (this.sending === true)
						return false;
					this.sending = true;

					// Get data
					var recommenders = [];
					$.each($(".changeEvalGroupRecommenders:checked"), function(
							index, elem) {
						recommenders.push($(elem).val());
					});
					var behavior = $(".changeEvalGroupBehavior:checked").val();

					var dataObj = {
						recommenders : recommenders,
						behavior : behavior
					};

					var errors = this.model.checkChangeData(dataObj);
					if (errors.length !== 0) {
						// Compose error message
						var msg = "";
						for (var i = 0; i < errors.length; i++) {
							msg += "&bull; " + errors[i] + "<br>";
						}
						// Render error message
						Utils.msg("changeEvalGroupMessage", "danger", msg);
						this.sending = false;
					} else {
						// Send data
						var self = this;
						Utils.spinner("changeEvalGroupMessage");
						if (this.model.get("changeAction") === "add") {
							var groupNew = Number($('#numGroup option:last').val()) + 1;
							$.when(this.model.addGroup(groupNew, dataObj)).done(
									function() {
										location.reload(true);
										self.sending = false;
									}).fail(
									function(jqXHR, textStatus, errorThrown) {
										Utils.error("changeEvalGroupMessage",
												jqXHR);
										self.sending = false;
									});
						} else if (this.model.get("changeAction") === "change") {
							var groupId = $("#numGroup").val();
							$.when(this.model.changeGroup(groupId, dataObj)).done(
									function() {
										location.reload(true);
										self.sending = false;
									}).fail(
									function(jqXHR, textStatus, errorThrown) {
										Utils.error("changeEvalGroupMessage",
												jqXHR);
										self.sending = false;
									});
						}
					}
				},

				deleteGroup : function() {
					// Avoid multiple firing of one click
					if (this.sending === true)
						return false;
					this.sending = true;

					var groupId = $("#numGroup").val();
					var that = this;
					Utils.spinner("deleteEvalGroupMessage");
					$.when(this.model.deleteGroup(groupId)).done(function() {
						location.reload(true);
						self.sending = false;
					}).fail(function(jqXHR, textStatus, errorThrown) {
						Utils.error("deleteEvalGroupMessage", jqXHR);
						self.sending = false;
					});
				},

				participantActionMove : function(event) {
					var name = event.currentTarget.attributes["data-name"].nodeValue;
					$("#participantMoveName").text(name);
					$("#participantMoveDialog").modal("show");
				},
				participantActionDelete : function(event) {
					var name = event.currentTarget.attributes["data-name"].nodeValue;
					$("#participantDeleteName").text(name);
					$("#participantDeleteDialog").modal("show");
				},

				deleteParticipant : function() {
					// Avoid multiple firing of one click
					if (this.sending === true)
						return false;
					this.sending = true;

					var name = $("#participantDeleteName").text();
					var self = this;
					Utils.spinner("participantDeleteMessage");
					$.when(this.model.deleteParticipant(name)).done(function() {
						location.reload(true);
						self.sending = false;
					}).fail(function(jqXHR, textStatus, errorThrown) {
						Utils.error("participantDeleteMessage", jqXHR);
						self.sending = false;
					});
				},

				moveParticipant : function() {
					// Avoid multiple firing of one click
					if (this.sending === true)
						return false;
					this.sending = true;

					var name = $("#participantMoveName").text();
					var groupId = $("#participantMoveGroup").val();
					var self = this;
					Utils.spinner("participantMoveMessage");
					$.when(this.model.moveParticipant(name, groupId)).done(
							function() {
								location.reload(true);
								self.sending = false;
							}).fail(function(jqXHR, textStatus, errorThrown) {
						Utils.error("participantMoveMessage", jqXHR);
						self.sending = false;
					});
				},

				renderRecResults : function(result) {
					$("#evalRecAnalysis").html(this.resultsRecTemplate(result));
					var model = this.model;

					// Compose data by cycling trough all object properties
					var dataset = [];
					var props = [];
					var p;
					var k;
					for (p in result.avgGroupRatings) {
						props.push(p);
						dataset.push({
							name : "Group" + p,
							data : [ result.avgGroupRatings[p] ]
						});
					}

					$('#avgGroupResult').highcharts({
						chart : {
							type : 'column',
							height : 250
						},
						title : {
							text : null
						},
						legend : {
							enabled : true
						},
						tooltip : {
							enabled : true,
							pointFormat : '{series.name}: <b>{point.y:.2f}</b>'
						},
						xAxis : {
							labels : {
								enabled : false
							}
						},
						yAxis : {
							title : {
								text : null
							},
							min : -1,
							max : 2
						},
						series : dataset
					});

					// recommender names
					var recommenders = [];
					for ( var i in model.get("recommenderNames")) {
						recommenders.push(model.get("recommenderNames")[i].NAME);
					}

					// create dummy array for categories
					var dummyVals = [];
					for (i in result.avgRatingsPerRecommender) {
						dummyVals.push(0);
					}

					// Compose data by cycling trough all object properties
					dataset = [];
					for (p in result.avgRatingsPerRecommender) {
						var group = dummyVals.slice();
						for (k in result.avgRatingsPerRecommender[p]) {
							group[_.indexOf(recommenders, model
									.get("recommenderNames")[k].NAME)] = result.avgRatingsPerRecommender[p][k];
						}
						dataset.push({
							name : "Group " + p,
							data : group
						});
					}

					$('#avgRecRatingResult').highcharts({
						chart : {
							type : 'column',
							height : 250
						},
						title : {
							text : null
						},
						legend : {
							enabled : true
						},
						tooltip : {
							pointFormat : '{series.name}: <b>{point.y:.2f}</b>'
						},
						xAxis : {
							title : {
								text : "Recommender",
								style : {
									color : '#333',
									fontWeight : 'bold',
									fontSize : 11
								}
							},
							labels : {
								step : 1
							},
							categories : recommenders
						},
						yAxis : {
							title : {
								text : null
							},
							min : -1,
							max : 2
						},
						series : dataset
					});

					// Compose data by cycling trough all object properties
					dataset = [];
					for (p in result.meanAbsoluteErrors) {
						var group = dummyVals.slice();
						for (k in result.meanAbsoluteErrors[p]) {
							group[_.indexOf(recommenders, model
									.get("recommenderNames")[k].NAME)] = result.meanAbsoluteErrors[p][k];
						}
						dataset.push({
							name : "Group " + p,
							data : group
						});
					}

					$('#maeResult').highcharts({
						chart : {
							type : 'column',
							height : 250
						},
						title : {
							text : null
						},
						legend : {
							enabled : true
						},
						tooltip : {
							pointFormat : '{series.name}: <b>{point.y:.2f}</b>'
						},
						xAxis : {
							title : {
								text : "Recommender",
								style : {
									color : '#333',
									fontWeight : 'bold',
									fontSize : 11
								}
							},
							categories : recommenders
						},
						yAxis : {
							title : {
								text : null
							},
							min : 0,
							max : 3
						},
						series : dataset
					});

					dataset = [];
					var xvals = [];
					for (p in result.groupListAvg) {
						group = [];
						for (k in result.groupListAvg[p]) {
							xvals.push(k);
							group.push(result.groupListAvg[p][k]);
						}
						dataset.push({
							name : "Group " + p,
							data : group
						});
					}
					xvals = _.uniq(xvals);

					$('#avgListResult').highcharts(
							{
								chart : {
									height : 250
								},
								title : null,
								legend : {
									enabled : true
								},
								xAxis : {
									title : {
										text : "#List",
										style : {
											color : '#333',
											fontWeight : 'bold',
											fontSize : 11
										}
									},
									categories : xvals
								},
								yAxis : {
									title : null,
									min : 0,
									max : 5
								},
								tooltip : {
									formatter : function() {
										return '<b>List #' + this.x + '</b><br/>'
												+ this.series.name + ': ' + this.y
												+ '';
									}
								},
								plotOptions : {
									spline : {
										lineWidth : 2,
										marker : {
											enabled : false
										}
									}
								},

								series : dataset
							});

					// Compose data by cycling trough all object properties
					dataset = [];
					for (p in result.groupAccuracy) {
						var group = dummyVals.slice();
						for (k in result.groupAccuracy[p]) {
							group[_.indexOf(recommenders, model
									.get("recommenderNames")[k].NAME)] = result.groupAccuracy[p][k];
						}
						dataset.push({
							name : "Group " + p,
							data : group
						});
					}

					$('#accuracyResult').highcharts({
						chart : {
							type : 'column',
							height : 250
						},
						title : {
							text : null
						},
						legend : {
							enabled : true
						},
						tooltip : {
							pointFormat : '{series.name}: <b>{point.y:.2f}</b>'
						},
						xAxis : {
							title : {
								text : "Recommender",
								style : {
									color : '#333',
									fontWeight : 'bold',
									fontSize : 11
								}
							},
							categories : recommenders
						},
						yAxis : {
							title : {
								text : null
							},
							min : 0,
							max : 1
						},
						series : dataset
					});

					this.recResults = true;
				},

				renderUserResults : function(result) {
					$("#evalUserAnalysis").html(this.resultsUserTemplate(result));

					var dataset = [];
					var props = [];
					var group = [];
					var p;
					var k;
					for (p in result.groupAgeDist) {
						group = [];
						for (k in result.groupAgeDist[p]) {
							props.push(k);
							var point = {
								x : k,
								y : result.groupAgeDist[p][k]
							};
							group.push(point);
						}
						dataset.push({
							name : "Group " + p,
							data : group
						});
					}

					// Draw chart for the age distribution
					$('#ageDistResult').highcharts({
						chart : {
							height : 200
						},
						title : {
							text : null
						},
						legend : {
							enabled : true
						},
						xAxis : {
							categories : props,
							tickInterval : 5
						},
						yAxis : {
							title : {
								text : null
							},
							allowDecimals : false
						},
						series : dataset
					});

					// Compose data by cycling trough all object properties
					dataset = [];
					for (p in result.groupGenderDist) {
						group = [];
						for (k in result.groupGenderDist[p]) {
							var index = 0;
							if (k === "Male") {
								index = 0;
							} else if (k === "Female") {
								index = 1;
							}
							var point = {
								x : index,
								y : result.groupGenderDist[p][k]
							};
							group.push(point);
						}
						dataset.push({
							name : "Group " + p,
							data : group
						});
					}

					$('#genderDistResult').highcharts({
						chart : {
							type : 'column',
							height : 200,
							width : 300
						},
						title : {
							text : null
						},
						legend : {
							enabled : true
						},
						tooltip : {
							pointFormat : '{series.name}: <b>{point.y}</b>'
						},
						xAxis : {
							categories : [ 'Male', 'Female' ]
						},
						yAxis : {
							title : {
								text : null
							},
							allowDecimals : false
						},
						series : dataset
					});

					dataset = [];
					for (p in result.groupJoins) {
						group = [];
						var total = 0;
						for (k in result.groupJoins[p]) {
							var item = [];
							item.push(new Date(k).getTime());
							total += result.groupJoins[p][k];
							item.push(total);
							group.push(item);
						}
						dataset.push({
							name : "Group " + p,
							data : group
						});
					}

					$('#joinsResultDay').highcharts(
							{
								chart : {
									type : 'spline',
									height : 200,
									width : 300
								},
								title : null,
								legend : {
									enabled : true
								},
								xAxis : {
									type : 'datetime',
									dateTimeLabelFormats : {
										month : '%e. %b',
										year : '%b'
									}
								},
								yAxis : {
									title : {
										text : "#Participants",
										style : {
											color : '#333',
											fontWeight : 'bold',
											fontSize : 11
										}
									},
									min : 0,
									allowDecimals : false
								},
								tooltip : {
									formatter : function() {
										return '<b>'
												+ Highcharts.dateFormat('%e. %b',
														this.x) + '</b><br/>'
												+ this.series.name + ': ' + this.y
												+ '';
									}
								},
								plotOptions : {
									spline : {
										lineWidth : 2,
										marker : {
											enabled : false
										}
									}
								},

								series : dataset
							});

					dataset = [];
					for (p in result.groupRatings) {
						group = [];
						var total = 0;
						for (k in result.groupRatings[p]) {
							var item = [];
							item.push(new Date(k).getTime());
							total += result.groupRatings[p][k];
							item.push(total);
							group.push(item);
						}
						dataset.push({
							name : "Group " + p,
							data : group
						});
					}

					$('#ratingResultDay').highcharts(
							{
								chart : {
									type : 'spline',
									height : 300
								},
								title : null,
								legend : {
									enabled : true
								},
								xAxis : {
									type : 'datetime',
									dateTimeLabelFormats : {
										month : '%e. %b',
										year : '%b'
									}
								},
								yAxis : {
									title : {
										text : "#Ratings",
										style : {
											color : '#333',
											fontWeight : 'bold',
											fontSize : 11
										}
									},
									min : 0,
									allowDecimals : false
								},
								tooltip : {
									formatter : function() {
										return '<b>'
												+ Highcharts.dateFormat('%e. %b',
														this.x) + '</b><br/>'
												+ this.series.name + ': ' + this.y
												+ '';
									}
								},
								plotOptions : {
									spline : {
										lineWidth : 2,
										marker : {
											enabled : false
										}
									}
								},

								series : dataset
							});

					dataset = [];
					for (p in result.groupVisits) {
						group = [];
						var total = 0;
						for (k in result.groupVisits[p]) {
							var item = [];
							item.push(new Date(k).getTime());
							total += result.groupVisits[p][k];
							item.push(total);
							group.push(item);
						}
						dataset.push({
							name : "Group " + p,
							data : group
						});
					}

					$('#visitsResultDay').highcharts(
							{
								chart : {
									type : 'spline',
									height : 300
								},
								title : null,
								legend : {
									enabled : true,
									borderRadius : 2
								},
								xAxis : {
									type : 'datetime',
									dateTimeLabelFormats : {
										month : '%e. %b',
										year : '%b'
									}
								},
								yAxis : {
									title : {
										text : "#Visits",
										style : {
											color : '#333',
											fontWeight : 'bold',
											fontSize : 11
										}
									},
									min : 0,
									allowDecimals : false
								},
								tooltip : {
									formatter : function() {
										return '<b>'
												+ Highcharts.dateFormat('%e. %b',
														this.x) + '</b><br/>'
												+ this.series.name + ': ' + this.y
												+ '';
									}
								},
								plotOptions : {
									spline : {
										lineWidth : 2,
										marker : {
											enabled : false
										}
									}
								},

								series : dataset
							});

					this.userResults = true;
				},

				deleteEval : function() {
					// Avoid multiple firing of one click
					if (this.sending === true)
						return false;
					this.sending = true;

					var self = this;
					Utils.spinner("evalDetailsMessage");
					$.when(this.model.deleteEvaluation()).done(function() {
						Backbone.history.navigate("evaluation", true);
						location.reload(true);
						self.sending = false;
					}).fail(function(jqXHR, textStatus, errorThrown) {
						Utils.error("evalDetailsMessage", jqXHR);
						self.sending = false;
					});
				},

				changeEval : function() {
					// Avoid multiple firing of one click
					if (this.sending === true)
						return false;
					this.sending = true;

					var self = this;
					Utils.spinner("changeEvalSettingsMessage");
					$.when(this.model.changeEvaluation()).done(function() {
						location.reload(true);
						self.sending = false;
					}).fail(function(jqXHR, textStatus, errorThrown) {
						Utils.error("changeEvalSettingsMessage", jqXHR);
						self.sending = false;
					});
				},

				// Send Information
				loading : function() {
					Utils.spinner("evaldetailsContent");
				}

			});
	return EvaluationDetailsView;
});