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
define([ 'jquery', 'underscore', 'backbone', 'session', 'utils',
		'collections/ConfigurationsCollection',
		'text!templates/configuration.html' ], function($, _, Backbone,
		Session, Utils, ConfigurationsCollection, configurationTemplate) {
	// Configuration View Definition
	// -------------------
	var ConfigurationView = Backbone.View.extend({
		// Anchor element
		el : '#configurationContent',

		// Flag for click events
		clicked : false,

		// Initialize event listeners
		events : {
			"click #saveConfiguration" : "saveConfig",
			"click #uploadJar" : "uploadJar",
			"click #removeBtn" : "removeRecommender",
			"click #updateBtn" : "updateRecommenderAction",
			"click #updateRecommenderBtn" : "updateRecommender"
		},

		// Initialize View
		initialize : function() {
			// Get the template
			this.template = _.template(configurationTemplate);

			this.model = new ConfigurationsCollection();
			var self = this;
			this.model.on("request", function(model, xhr, opts) {
				self.loading();
				$.when(xhr).done(function(result) {
					// Render if all was
					// going fine
					self.render();
				}).fail(function(jqXHR, textStatus, errorThrown) {
					Utils.error("configurationContent", jqXHR);
				});
			});

			// Fetch recommendations from server
			this.model.fetch();
		},

		// Start save config process
		saveConfig : function() {
			var changedConfig = $(".configItem").serializeArray();

			// Avoid multiple click events fired
			if (this.clicked === true)
				return false;
			this.clicked = true;

			// Show loading indicator
			Utils.spinner("messageConfiguration");

			// Get the inserted data
			var changedConfig = $(".configItem").serializeArray();

			// Handle callbacks
			var self = this;
			$.when(this.model.updateConfig(changedConfig)).done(
					function(result) {
						// Render error message
						Utils.msg("messageConfiguration", "success",
								"Succesfully updated configuration.");
						self.clicked = false;
						location.reload(true);
					}).fail(function(jqXHR, textStatus, errorThrown) {
				Utils.error("messageConfiguration", jqXHR);
				self.clicked = false;
			});
		},

		// Start remove process
		removeRecommender : function(event) {
			var recid = event.currentTarget.attributes["data-recid"].nodeValue;

			// Avoid multiple click events fired
			if (this.clicked === true)
				return false;
			this.clicked = true;

			// Show loading indicator
			Utils.spinner("messageConfiguration");

			// Handle callbacks
			var self = this;
			$.when(this.model.removeRecommender(recid)).done(function(result) {
				location.reload(true);
				self.clicked = false;
			}).fail(function(jqXHR, textStatus, errorThrown) {
				Utils.msg("messageConfiguration", "danger", jqXHR.responseText);
				self.clicked = false;
			});
		},

		updateRecommenderAction : function(event) {
			var id = event.currentTarget.attributes["data-recid"].nodeValue;
			$("#updateRecommenderId").text(id);
			$("#updateRecommenderDialog").modal("show");
		},

		// Start update process
		updateRecommender : function() {
			// Avoid multiple click events fired
			if (this.clicked === true)
				return false;
			this.clicked = true;

			// Show loading indicator
			Utils.spinner("messageUpdateRecommender");

			// Get the jar
			var file = $("#updateFilePath").get(0).files[0];
			var classname = $("#updateClassName").val();

			// Check for errors
			var errors = [];
			if (!file) {
				errors.push("No jar specified.");
			} else {
				if (file.type !== "application/java-archive") {
					errors.push("Only .jar is allowed.");
				}
				if (file.size > 3000000) {
					errors.push("File mustn't be bigger than 3MB.");
				}
			}
			if (!classname) {
				errors.push("Classname must be specified.")
			}

			// No errors? Continue
			if (errors.length === 0) {
				var formData = new FormData();
				formData.append("updateId", $("#updateRecommenderId").text());
				formData.append("file", file, file.name);
				formData.append("classname", classname);

				var self = this;
				$.when(this.model.updateRecommender(formData)).done(
						function(result) {
							Utils.msg("messageUpdateRecommender", "success",
									"Update successful.");
							$("#updateRecommenderDialog").modal("hide");
							self.clicked = false;
							location.reload(true);
						}).fail(function(jqXHR, textStatus, errorThrown) {
					Utils.error("messageUpdateRecommender", jqXHR);
					self.clicked = false;
				});
			} else {

				// Compose error message
				var msg = "";
				for (var i = 0; i < errors.length; i++) {
					msg += "&bull; " + errors[i] + "<br>";
				}

				// Render error message
				Utils.msg("messageUpdateRecommender", "danger", msg);
				this.clicked = false;
			}
		},

		uploadJar : function() {
			// Avoid multiple click events fired
			if (this.clicked === true)
				return false;
			this.clicked = true;

			// Show loading indicator
			Utils.spinner("messageUploadJar");

			// Get the jar
			var file = $("#filePath").get(0).files[0];
			var classname = $("#className").val();

			// Check for errors
			var errors = [];
			if (!file) {
				errors.push("No jar specified.");
			} else {
				if (file.type !== "application/java-archive") {
					errors.push("Only .jar is allowed.");
				}
				if (file.size > 3000000) {
					errors.push("File mustn't be bigger than 3MB.");
				}
			}
			if (!classname) {
				errors.push("Classname must be specified.")
			}

			// No errors? Continue
			if (errors.length === 0) {
				var formData = new FormData();
				formData.append("file", file, file.name);
				formData.append("classname", classname);

				var self = this;
				$.when(this.model.addRecommender(formData)).done(
						function(result) {
							Utils.msg("messageUploadJar", "success",
									"Adding recommender successful.");
							self.clicked = false;
							location.reload(true);
						}).fail(function(jqXHR, textStatus, errorThrown) {
					Utils.error("messageUploadJar", jqXHR);
					self.clicked = false;
				});
			} else {

				// Compose error message
				var msg = "";
				for (var i = 0; i < errors.length; i++) {
					msg += "&bull; " + errors[i] + "<br>";
				}

				// Render error message
				Utils.msg("messageUploadJar", "danger", msg);
				this.clicked = false;
			}
		},

		// Render the content to the html node
		render : function() {
			// Render template
			this.$el.html(this.template({
				config : this.model.toJSON()
			}));
			this.restoreOptions();
			return this;
		},

		// Checks the radio buttons to represent the config
		restoreOptions : function() {
			// Restore recommenders
			var config = this.model.toJSON();
			for (var i = 0; i < config.length; i++) {
				var currentID = config[i].id;
				var currentValue = config[i].status;
				$(
						".configItem[name=" + currentID + "][value="
								+ currentValue + "]")
						.attr("checked", "checked");
			}
		},

		loading : function() {
			Utils.spinner("configurationContent");
		}
	});

	return ConfigurationView;
});
