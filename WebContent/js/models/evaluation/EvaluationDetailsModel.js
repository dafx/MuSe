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
	// EvaluationDetails Model Definition
	// ----------------------------------------
	var EvaluationDetailsModel = Backbone.Model.extend({
		// Model attributes
		defaults : {
			id : 0
		},

		// REST URL
		url : "getEvaluation",

		initialize : function() {
			this.url = Utils.serviceUrl + this.url + "/id/" + this.get("id");
		},

		deleteEvaluation : function() {
			var data = JSON.stringify(this.id);
			var uri = Utils.serviceUrl + "deleteEvaluation";
			return $.ajax({
				url : uri,
				type : 'POST',
				data : data,
				contentType : "application/json"
			});
		},

		changeEvaluation : function() {
			var name = $("#changeEvalName").val();
			var from = $("#changeEvalFrom").val();
			var to = $("#changeEvalTo").val();
			var composition = [];
			$.each($(".changeComposition:checked"), function(index, elem) {
				composition.push($(elem).val());
			});

			var dataObj = {};
			dataObj.evaluationId = this.id;
			if(name) {
				dataObj.name = name;
			}
			if (from) {
				dataObj.from = from;
			}
			if (to) {
				dataObj.to = to;
			}
			if (composition.length > 0) {
				dataObj.composition = composition;
			}

			var data = JSON.stringify(dataObj);
			var uri = Utils.serviceUrl + "changeEvaluation";
			return $.ajax({
				url : uri,
				type : 'POST',
				data : data,
				contentType : "application/json"
			});
		},

		changeGroup : function(groupId, dataObj) {
			dataObj.evaluationId = this.get("id");
			dataObj.numGroup = groupId;

			var data = JSON.stringify(dataObj);
			var uri = Utils.serviceUrl + "changeGroup";
			return $.ajax({
				url : uri,
				type : 'POST',
				data : data,
				contentType : "application/json"
			});
		},

		addGroup : function(groupId, dataObj) {
			dataObj.evaluationId = this.get("id");
			dataObj.numGroup = groupId;

			var data = JSON.stringify(dataObj);
			var uri = Utils.serviceUrl + "addGroup";
			return $.ajax({
				url : uri,
				type : 'POST',
				data : data,
				contentType : "application/json"
			});
		},

		deleteGroup : function(groupId) {
			var dataArray = [ this.get("id"), groupId ];

			var data = JSON.stringify(dataArray);
			var uri = Utils.serviceUrl + "deleteGroup";
			return $.ajax({
				url : uri,
				type : 'POST',
				data : data,
				contentType : "application/json"
			});
		},

		deleteParticipant : function(name) {
			var dataArray = [ this.get("id"), name ];
			console.log(dataArray);
			var data = JSON.stringify(dataArray);
			var uri = Utils.serviceUrl + "deleteParticipant";
			return $.ajax({
				url : uri,
				type : 'POST',
				data : data,
				contentType : "application/json"
			});
		},

		moveParticipant : function(name, groupId) {
			var dataArray = [ this.get("id"), groupId, name ];

			var data = JSON.stringify(dataArray);
			var uri = Utils.serviceUrl + "moveParticipant";
			return $.ajax({
				url : uri,
				type : 'POST',
				data : data,
				contentType : "application/json"
			});
		},

		checkChangeData : function(data) {
			var errors = [];
			if (data.recommenders.length === 0) {
				errors.push("Please select at least one recommender.");
			}
			if (!data.behavior) {
				errors.push("Please select list composition.");
			}
			return errors;
		},

		getRecResults : function() {
			var data = JSON.stringify(this.id);
			var uri = Utils.serviceUrl + "getEvaluationResults/source=recommender";
			return $.ajax({
				url : uri,
				type : 'POST',
				data : data,
				contentType : "application/json"
			});
		},

		getUserResults : function() {
			var data = JSON.stringify(this.id);
			var uri = Utils.serviceUrl + "getEvaluationResults/source=user";
			return $.ajax({
				url : uri,
				type : 'POST',
				data : data,
				contentType : "application/json"
			});
		}
	});
	
	return EvaluationDetailsModel;
});