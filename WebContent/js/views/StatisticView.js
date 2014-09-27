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
  'highcharts',
  'utils',
  'models/StatisticModel',
  'text!templates/statistics.html'
], function($, _, Backbone, Highcharts, Utils, StatisticModel, StatisticTemplate){
	// StatisticView Definition
	// -------------------
	var StatisticView = Backbone.View.extend({
				// Anchor element
				el : '#statisticsview',
				anchor : '#statisticsContent',
				sending : false,
				tab : false,

				// Initialize View
				initialize : function() {
					// Get the template
					this.template = _.template(StatisticTemplate);
					this.model = this.model || new StatisticModel();
					var self = this;
					this.model.on("request", function(model, xhr, opts) {
						self.loading();
						$.when(xhr).done(function(result) {
							// Render if all was going fine
							self.render();
						}).fail(function(jqXHR, textStatus, errorThrown) {
							Utils.error("statisticsContent", jqXHR);
						});
					});

					// Fetch recommendations from server
					this.model.fetch();
				},

				// Initialize event listeners
				events : {
					"click #rangeBtn" : "updateDateRange",
					"click #recTabBtn" : "renderRecommendationCharts"
				},

				// Render the content to the html node
				render : function() {
					// Render template
					$(this.anchor).html(this.template(this.model.toJSON()));
					// Render Charts
					this.renderUserCharts();
					return this;
				},

				updateDateRange : function() {
					// Avoid multiple firing of one click
					if (this.sending === true)
						return false;
					this.sending = true;
					var rangeFrom = ($("#rangeFrom").val() === "" ? "none" : $(
							"#rangeFrom").val());
					var rangeTo = ($("#rangeTo").val() === "" ? "none" : $(
							"#rangeTo").val());
					stats = new StatisticModel({
						from : rangeFrom,
						to : rangeTo
					});
					new StatisticView({
						model : stats
					});
				},

				// Send Information
				loading : function() {
					Utils.spinner("statisticsContent");
				},

				// Render User Tab Charts
				renderUserCharts : function() {

					// Compose data by cycling trough all object properties
					var props = [];
					var values = [];
					for ( var p in this.model.get("ageDistribution")) {
						props.push(p);
						values.push(this.model.get("ageDistribution")[p]);
					}
					// Draw chart for the age distribution
					$('#ageDist').highcharts({
						chart : {
							height : 200,
							width : 300
						},
						title : {
							text : null
						},
						legend : {
							enabled : false
						},
						xAxis : {
							categories : props,
							labels : {
								maxStaggerLines : 1,
								step : 6
							}
						},
						yAxis : {
							allowDecimals : false,
							title : {
								text : null
							},
							plotLines : [ {
								value : 0,
								width : 1,
								color : '#808080'
							} ]
						},
						series : [ {
							name : 'Number of users',
							data : values
						} ]
					});

					// Draw gender distribution
					$('#genderDist')
							.highcharts(
									{
										chart : {
											height : 200,
											width : 300
										},
										title : null,
										plotOptions : {
											pie : {
												dataLabels : {
													enabled : true,
													format : '<b>{point.name}</b>: {point.percentage:.1f} %',
													distance : 5,
													borderWidth : 4
												}
											}
										},
										series : [ {
											type : 'pie',
											name : 'Number of users',
											data : [
													[
															'Male',
															this.model
																	.get('genderDistribution').Male ],
													[
															'Female',
															this.model
																	.get('genderDistribution').Female ] ]
										} ]
									});

					// Draw language distribution
					// Compose data by cycling trough all object properties
					table = [];
					for (p in this.model.get("langDistribution")) {
						table.push([ p, this.model.get("langDistribution")[p] ]);
					}
					$('#langDist')
							.highcharts(
									{
										chart : {
											height : 200,
											width : 350
										},
										title : null,
										plotOptions : {
											pie : {
												dataLabels : {
													enabled : true,
													format : '<b>{point.name}</b>: {point.percentage:.1f} %',
													distance : 5
												}
											}
										},
										series : [ {
											type : 'pie',
											name : 'Number of users',
											data : table
										} ]
									});

					// Compose data by cycling trough all object properties
					props = [];
					values = [];
					for (p in this.model.get("visitDistribution")) {
						props.push(p);
						values.push(this.model.get("visitDistribution")[p]);
					}

					$('#visitDist').highcharts({
						chart : {
							type : 'column',
							width : 300,
							height : 200
						},
						title : {
							text : null
						},
						legend : {
							enabled : false
						},
						tooltip : {
							pointFormat : '{series.name}: <b>{point.y:.1f}</b>'
						},
						xAxis : {
							categories : props,
							labels : {
								style : {
									textTransform : 'capitalize'
								}
							}
						},
						yAxis : {
							allowDecimals : false,
							title : {
								text : null
							},
							min : 0
						},
						series : [ {
							name : 'Time spent',
							data : values

						} ]
					});

					var dataset = [];
					for (p in this.model.get("visits")) {
						var item = [];
						item.push(new Date(p).getTime());
						item.push(this.model.get("visits")[p]);
						dataset.push(item);
					}

					$('#visitStats').highcharts(
							{
								chart : {
									type : 'spline'
								},
								title : null,
								legend : {
									enabled : false
								},
								xAxis : {
									type : 'datetime',
									dateTimeLabelFormats : { // don't display the
										// dummy year
										month : '%e. %b',
										year : '%b'
									}
								},
								yAxis : {
									title : null,
									min : 0
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

								series : [ {
									name : 'Visitors',
									data : dataset
								} ]
							});
					
					var dataset = [];
					for (p in this.model.get("ratings")) {
						var item = [];
						item.push(new Date(p).getTime());
						item.push(this.model.get("ratings")[p]);
						dataset.push(item);
					}

					$('#ratingStats').highcharts(
							{
								chart : {
									type : 'spline'
								},
								title : null,
								legend : {
									enabled : false
								},
								xAxis : {
									type : 'datetime',
									dateTimeLabelFormats : {
										month : '%e. %b',
										year : '%b'
									}
								},
								yAxis : {
									title : null,
									min : 0
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

								series : [ {
									name : 'Ratings',
									data : dataset
								} ]
							});
				},

				// Render Recommendation Tab Charts
				renderRecommendationCharts : function() {
					// Render only once
					if (this.tab === true)
						return;
					this.tab = true;

					// Draw rating distribution
					// Compose data by cycling trough all object properties
					var table = [];
					for ( var p in this.model.get("ratingDistribution")) {
						var rating = "unknown";
						if (p === "1") {
							rating = "Liked";
						} else if (p === "2") {
							rating = "Loved";
						} else if (p === "0") {
							rating = "Not rated";
						} else if (p === "-1") {
							rating = "Disliked";
						}
						table.push([ rating,
								this.model.get("ratingDistribution")[p] ]);
					}
					$('#ratingDist')
							.highcharts(
									{
										chart : {
											height : 200,
											width : 350
										},
										title : null,
										plotOptions : {
											pie : {
												dataLabels : {
													enabled : true,
													format : '<b>{point.name}</b>: {point.percentage:.1f} %',
													distance : 5
												}
											}
										},
										series : [ {
											type : 'pie',
											name : 'Number of ratings',
											data : table
										} ]
									});

					// Draw rating distribution for each recommender
					// Compose data by cycling trough all object properties
					var recommenders = [];
					var ratedOne = [];
					var ratedTwo = [];
					var ratedNone = [];
					var ratedNeg = [];
					for (p in this.model.get("recommenderRatingDistribution")) {
						recommenders.push(p);
						for ( var prop in this.model
								.get("recommenderRatingDistribution")[p]) {
							if (prop === "1") {
								ratedOne
										.push(this.model
												.get("recommenderRatingDistribution")[p][prop]);
							} else if (prop === "2") {
								ratedTwo
										.push(this.model
												.get("recommenderRatingDistribution")[p][prop]);
							} else if (prop === "0") {
								ratedNone
										.push(this.model
												.get("recommenderRatingDistribution")[p][prop]);
							} else if (prop === "-1") {
								ratedNeg
										.push(this.model
												.get("recommenderRatingDistribution")[p][prop]);
							}
						}
					}
					$('#recRatingDist').highcharts({
						chart : {
							type : 'bar'
						},
						title : null,
						xAxis : {
							categories : recommenders,
							title : {
								text : null
							}
						},
						yAxis : {
							min : 0,
							title : 'Recommender IDs'
						},
						plotOptions : {
							bar : {
								dataLabels : {
									enabled : true
								}
							}
						},
						legend : {
							layout : 'vertical',
							align : 'right',
							verticalAlign : 'bottom',
							y : -20,
							floating : true,
							borderWidth : 1,
							backgroundColor : '#FFFFFF',
							shadow : true
						},
						credits : {
							enabled : false
						},
						series : [ {
							name : 'Not rated',
							data : ratedNone
						}, {
							name : 'Liked',
							data : ratedOne
						}, {
							name : 'Loved',
							data : ratedTwo
						}, {
							name : 'Disliked',
							data : ratedNeg
						} ]
					});

					content = [];
					for (p in this.model.get("recommenderRatingDistribution")) {
						var values = [];
						var amount = 0;
						values.push(p);
						for ( var k in this.model
								.get("recommenderRatingDistribution")[p]) {
							amount += this.model
									.get("recommenderRatingDistribution")[p][k];
						}
						values.push(amount);
						content.push(values);
					}

					$('#recommendationDist')
							.highcharts(
									{
										chart : {
											height : 300,
											width : 600
										},
										title : null,
										tooltip : {
											pointFormat : '{series.name}: <b>{point.y}</b>'
										},
										plotOptions : {
											pie : {
												dataLabels : {
													enabled : true,
													format : '<b>{point.name}</b>: {point.percentage:.1f} %',
													distance : 5
												}
											}
										},
										series : [ {
											type : 'pie',
											name : 'Created Recommendations',
											innerSize : '50%',
											data : content
										} ]
									});

					// Compose data by cycling trough all object properties
					var props = [];
					var data = [];
					for (p in this.model.get("meanAbsoluteErrors")) {
						props.push(p);
						data.push(this.model.get("meanAbsoluteErrors")[p]);
					}

					$('#maeDist').highcharts({
						chart : {
							type : 'column',
							width : 300,
							height : 200
						},
						title : {
							text : null
						},
						legend : {
							enabled : false
						},
						tooltip : {
							pointFormat : '{series.name}: <b>{point.y:.2f}</b>'
						},
						xAxis : {
							categories : props
						},
						yAxis : {
							title : {
								text : null
							},
							min : -1
						},
						series : [ {
							name : 'MAE',
							data : data

						} ]
					});
				}
			});
	
	return StatisticView;
});