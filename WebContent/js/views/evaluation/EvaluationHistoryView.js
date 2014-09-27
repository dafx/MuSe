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
  'collections/EvaluationsCollection',
  'text!templates/evaluation/history.html'
], function($, _, Backbone, Utils, EvaluationsCollection, EvaluationHistoryTemplate){
	// Evaluation HistoryView Definition
	// --------------------------
	var EvaluationHistoryView = Backbone.View
			.extend({
				anchor : "#evalhistoryview",

				// Initialize View
				initialize : function() {
					this.model = new EvaluationsCollection();
					this.template = _.template(EvaluationHistoryTemplate);
					this.model.on("fetch", this.loading, this);
					this.model.on("reset", this.render, this);
					this.model.fetch({
						reset : true
					}).fail(function(jqXHR, textStatus, errorThrown) {
						Utils.error("evalhistoryview", jqXHR);
					});
				},

				// Render the content to the html node
				render : function() {
					$(this.anchor).empty();
					// Render template
					var today = new Date(Utils.getTodayStr()).getTime();

					for (var i = 0; i < this.model.models.length; i++) {
						var tempTo = new Date(this.model.models[i].get("to"))
								.getTime();
						var tempFrom = new Date(this.model.models[i].get("from"))
								.getTime();

						if (tempTo >= today && tempFrom <= today) {
							this.model.models[i].set("running", "current");
							$(this.anchor).prepend(
									this.template(this.model.models[i].toJSON()));
						} else if (tempFrom > today) {
							this.model.models[i].set("running", "scheduled");
							$(this.anchor).append(
									this.template(this.model.models[i].toJSON()));
						} else {
							this.model.models[i].set("running", "finished");
							$(this.anchor).append(
									this.template(this.model.models[i].toJSON()));
						}
					}
					return this;
				},

				// Send Information
				loading : function() {
					Utils.spinner("evalhistoryview");
				}
			});
	
	return EvaluationHistoryView;
});