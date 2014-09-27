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
define([ 'jquery', 'underscore', 'backbone', 'router', 'session', 'utils',
		'views/UserView' ], function($, _, Backbone, Router, Session, Utils,
		UserView) {
	var initialize = function() {
		// window.onerror = function() {
		// // Clear session & Show exception page
		// window.sessionStorage.clear();
		// Backbone.history.navigate("home", true);
		// window.location = "/MuSe/exception.html";
		// };

		// Check for mobile devices
		var mobile = Utils.isMobile();

		// Check & Restore active session
		_.extend(Session, Backbone.Events);
		Session.restore();

		// Create initial views
		var userView = new UserView();

		// Pass in our Router module and call it's initialize function
		Router.initialize();

		// Extend backbone models with fetch event
		_.each([ "Model", "Collection" ], function(name) {
			// Cache Backbone constructor.
			var ctor = Backbone[name];
			// Cache original fetch.
			var fetch = ctor.prototype.fetch;

			// Override the fetch method to emit a fetch event.
			ctor.prototype.fetch = function() {
				// Trigger the fetch event on the instance.
				this.trigger("fetch", this);

				// Pass through to original fetch.
				return fetch.apply(this, arguments);
			};
		});

	};

	return {
		initialize : initialize
	};
});
