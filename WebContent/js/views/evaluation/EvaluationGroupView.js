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
  'models/evaluation/EvaluationGroupModel',
  'text!templates/evaluation/group.html'
], function($, _, Backbone, EvaluationGroupModel, EvaluationGroupTemplate){

	// Evaluation GroupView Definition
	// --------------------------
	var EvaluationGroupView = Backbone.View.extend({
		anchor : "#groupsview",

		// Initialize View
		initialize : function(options) {
			this.options = options || {};
			this.model = new EvaluationGroupModel({
				numGroup : this.options.numGroup,
				recommenderList : this.options.recommenders
			});
			// Get the template
			this.template = _.template(EvaluationGroupTemplate);
			this.render();
		},

		// Initialize event listeners
		events : {
			"change .evalRecommenders" : "setRecommenders",
			"change .evalBehavior" : "setBehavior"
		},

		// Render the content to the html node
		render : function() {
			// Render template
			$(this.anchor).append(this.template(this.model.toJSON()));
			this.setElement("#group" + this.options.numGroup);
			return this;
		},

		setRecommenders : function() {
			var recommenders = [];
			var self = this;
			$.each($("#" + $(this.el).attr("id") + " .evalRecommenders:checked"),
					function(index, elem) {
						recommenders.push($(elem).val());
					});
			this.model.set("recommenders", recommenders);
		},

		setBehavior : function() {
			this.model.set("behavior", $(
					"#" + $(this.el).attr("id") + " .evalBehavior:checked").val());
		}
	});
	
	return EvaluationGroupView;
});