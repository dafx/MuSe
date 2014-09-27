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
	'text!templates/profile.html'
], function($, _, Backbone, Session, profileTemplate){
	// Profile View Definition
	// -------------------
	var ProfileView = Backbone.View.extend({
		// Anchor element
		el : '#profileview',

		// Initialize View
		initialize : function() {
			// Get the template
			this.template = _.template(profileTemplate);
			this.listenTo(Session, "changed", function() {
				this.$el.empty();
				this.render();
			});
			// Render the view
			this.render();
		},

		// Render the content to the html node
		render : function() {
			// Render template
			this.$el.html(this.template(Session.user));
			return this;
		}
	});

	return ProfileView;
});
