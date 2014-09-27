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
  'session',
  'utils',
  'models/evaluation/EvaluationModel',
  'views/evaluation/EvaluationHistoryView',
  'views/evaluation/EvaluationGroupView',
  'text!templates/evaluation/home.html',
  'text!templates/evaluation/participants.html'
], function($, _, Backbone, Session, Utils, EvaluationModel, EvaluationHistoryView, EvaluationGroupView,
		EvaluationTemplate, EvaluationParticipantTemplate){
	// Evaluation View Definition
	// -------------------
	var EvaluationView = Backbone.View
			.extend({
				// Anchor element
				el : '#evaluationview',
				anchor : '#evaluationContent',
				currentStep : 1,
				sending : false,

				// Initialize View
				initialize : function() {
					this.model = new EvaluationModel();
					// Get the template
					this.template = _.template(EvaluationTemplate);
					this.participantsTemplate = _.template(EvaluationParticipantTemplate);
					var self = this;
					$.when(Utils.getRecommenderList()).done(function(result) {
						self.model.set("recommenders", result);
						// Render if all was going fine
						self.render();
					}).fail(function(jqXHR, textStatus, errorThrown) {
						Utils.error("evalContent", jqXHR);
					});
				},

				// Initialize event listeners
				events : {
					"change #numberGroups" : "adaptGroups",
					"change #evalFrom" : "setFrom",
					"change #evalTo" : "setTo",
					"change #evalName": "setName",
					"change .composition" : "setComposition",
					"click .nextStep" : "nextStep",
					"click .prevStep" : "prevStep",
					"click #createEvalFinal" : "createEval"
				},

				defaults : {},

				// Render the content to the html node
				render : function() {
					// Render template
					$(this.anchor).html(this.template());
					this.historyView = new EvaluationHistoryView();
					this.adaptGroups();

					return this;
				},

				renderParticipantsMatching : function(participants) {
					$("#createEvalFinal").show();
					for (var i = 1; i <= this.model.get("groupsDetails").length; i++) {
						$("#participantsGroup" + i).html("");
					}
					for ( var p in participants) {
						var user = JSON.parse(p);
						$("#participantsGroup" + participants[p]).append(
								"<tr><td>" + user.name + "</td>" + "<td>"
										+ user.birthyear + "</td>" + "<td>"
										+ user.sex + "</td>" + "<td>" + user.langs
										+ "</td></tr>");
					}
				},

				// Send Information
				loading : function() {
					Utils.spinner("evaluationContent");
				},

				setFrom : function() {
					this.model.set("from", $("#evalFrom").val());
				},

				setTo : function() {
					this.model.set("to", $("#evalTo").val());
				},
				
				setName : function() {
					this.model.set("name", $("#evalName").val().trim());
				},

				setComposition : function() {
					var composition = [];
					$.each($(".composition:checked"), function(index, elem) {
						composition.push($(elem).val());
					});
					this.model.set("composition", composition);
				},

				adaptGroups : function() {
					this.model.set("summaryCreated", 0);
					$("#groupsview").empty();
					$("#summaryview").empty();
					var numGroups = $("#numberGroups").val();
					var groups = [];
					for (var i = 0; i < numGroups; i++) {
						var group = new EvaluationGroupView({
							numGroup : i + 1,
							recommenders : this.model.get("recommenders")
						});
						groups.push(group.model);
						$("#summaryview").append(this.participantsTemplate({
							numGroup : i
						}));
					}
					this.model.set("groupsDetails", groups);
				},

				nextStep : function(event) {
					$("#step" + this.currentStep).hide(300);
					$("#navStep" + this.currentStep).removeClass("active");
					$("#navStepElem" + this.currentStep).removeClass("active");
					this.currentStep++;
					$("#step" + this.currentStep).show(300);
					$("#navStep" + this.currentStep).addClass("active");
					$("#navStepElem" + this.currentStep).addClass("active");
					if (this.currentStep == 3) {
						this.getEvaluationSummary();
					}
				},

				prevStep : function(event) {
					$("#step" + this.currentStep).hide(300);
					$("#navStep" + this.currentStep).removeClass("active");
					$("#navStepElem" + this.currentStep).removeClass("active");
					this.currentStep--;
					$("#step" + this.currentStep).show(300);
					$("#navStep" + this.currentStep).addClass("active");
					$("#navStepElem" + this.currentStep).addClass("active");
					$("#evalMessage").html("");
				},

				createEval : function() {
					// Avoid multiple firing of one click
					if (this.sending === true)
						return false;
					this.sending = true;

					// Set meta info
					var today = Utils.getTodayStr();
					this.model.set("created", today);
					this.model.set("creator", Session.user.name);

					// Check data
					var errors = this.model.checkData();
					if (errors.length !== 0) {
						// Compose error message
						var msg = "";
						for (var i = 0; i < errors.length; i++) {
							msg += "&bull; " + errors[i] + "<br>";
						}

						// Render error message
						Utils.msg("evalMessage", "danger", msg);

						// Don't check server if new password is wrong
						this.sending = false;
					} else {
						var self = this;
						Utils.spinner("evalMessage");

						$.when(this.model.saveEvaluation()).done(function() {
							$(self.anchor).empty();
							self.undelegateEvents();
							new EvaluationView(); // NO longer assigned to global view
							self.sending = false;
						}).fail(function(jqXHR, textStatus, errorThrown) {
							Utils.error("evalMessage", jqXHR);
							self.sending = false;
						});
					}
				},

				getEvaluationSummary : function() {
					// Avoid multiple firing of one click
					if (this.sending === true)
						return false;
					this.sending = true;

					// Set meta info
					var today = Utils.getTodayStr();
					this.model.set("created", today);
					this.model.set("creator", Session.user.name);

					// Check data
					var errors = this.model.checkData();
					var evals = this.historyView.model.models;
					var dateFrom = new Date(this.model.get("from")).getTime();
					var dateTo = new Date(this.model.get("to")).getTime();

					for (var i = 0; i < evals.length; i++) {
						var tempFrom = new Date(evals[i].get("from")).getTime();
						var tempTo = new Date(evals[i].get("to")).getTime();

						if (dateFrom >= tempFrom && dateFrom <= tempTo) {
							errors.push("Please choose a start date that is "
									+ "disjoint from other "
									+ "scheduled evaluations.");
							break;
						} else if (dateTo >= tempFrom && dateTo <= tempTo) {
							errors.push("Please choose an end date that is "
									+ "disjoint from other "
									+ "scheduled evaluations.");
							break;
						} else if (dateFrom <= tempFrom && dateTo >= tempTo) {
							errors.push("Please choose a time period that is "
									+ "disjoint from other "
									+ "scheduled evaluations.");
							break;
						}
					}
					if (errors.length !== 0) {
						// Compose error message
						var msg = "";
						for (var j = 0; j < errors.length; j++) {
							msg += "&bull; " + errors[j] + "<br>";
						}

						// Render error message
						Utils.msg("evalMessage", "danger", msg);

						// Don't check server if new password is wrong
						this.sending = false;
					} else {
						var self = this;
						if (this.model.get("summaryCreated") === 0) {
							for (var k = 1; k <= this.model.get("groupsDetails").length; k++) {
								Utils.spinner("participantsGroup" + k);
							}

							$.when(this.model.getEvaluationSummary()).done(
									function(data) {
										self.renderParticipantsMatching(data);
										self.model.set("summaryCreated", 1);
										self.sending = false;
									}).fail(
									function(jqXHR, textStatus, errorThrown) {
										Utils.error("evalMessage", jqXHR);
										self.sending = false;
									});
						} else {
							self.sending = false;
						}
					}
				}
			});
	
	return EvaluationView;
});