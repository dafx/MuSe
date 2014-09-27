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
	'bootstrap',
	'session',
	'utils'
], function($, _, Backbone, Bootstrap, Session, Utils){
// UserModel Definition
// -------------------------------
var UserModel = Backbone.Model.extend({

			// Initialize members with default values
			defaults : {
				name : "",
				password : "",
				email : "",
				sex : "",
				birthyear : "",
				lfmaccount : "",
				langs : []
			},

			// Check user data constraints
			evaluateData : function() {
				// Validate user input
				var errors = [];
				// Check if all attributes are provided
				for ( var prop in this.attributes) {
					if (this.attributes[prop] === "") {
						if (prop !== "lfmaccount") {
							errors
									.push("All fields except Lastfm account are required.");
							break;
						}
					}
				}

				// Check for violations
				if (this.attributes.terms !== "agree") {
					errors.push("You have to agree to our terms of service.");
				}
				if (this.attributes.password !== this.attributes.passwordRepeat) {
					errors.push("Passwords do not match.");
				}
				var pwErrors = this.evaluatePassword(this.attributes.password);
				if (pwErrors.length > 0) {
					for (var i = 0; i < pwErrors.length; i++) {
						errors.push(pwErrors[i]);
					}
				}
				if (this.attributes.name.indexOf("'") !== -1 ||
					this.attributes.name.indexOf("\"") !== -1) {
					errors.push("\" or ' are not allowed in the username.");
				}
				if (this.attributes.birthyear < 1950 ||
					this.attributes.birthyear > 2000) {
					errors.push("Year of birth must be provided.");
				}

				return errors;
			},

			// Check password constraints
			evaluatePassword : function(password) {
				var errors = [];
				// 8 characters long
				if (password.length < 8) {
					errors.push("Password must be at least 8 characters long.");
				}
				// At least one Upercase
				if (password === password.toLowerCase()) {
					errors.push("Password must have an uppercase letter.");
				}
				// At least one digit
				var regex = /\d/g;
				if (!regex.test(password)) {
					errors.push("Password must have a number.");
				}

				return errors;
			},

			register : function() {
				this.unset('passwordRepeat', 'silent');
				this.set('name', this.get('name').toLowerCase());

				// Send user data to the backend
				var data = JSON.stringify(this.toJSON());
				return $.ajax({
					url : Utils.serviceUrl + "putUser",
					type : 'PUT',
					data : data,
					contentType : "application/json"
				});
			},
			
			socialLogin : function(auth) {
				var data = {
						status: auth.status,
						accessToken: auth.authResponse.accessToken,
						expiresIn: auth.authResponse.expiresIn,
						signedRequest: auth.authResponse.signedRequest,
						userId: auth.authResponse.userID
					}
				var data = JSON.stringify(data);
				// Check with the server for proper credentials
				return $.ajax({
					url : Utils.serviceUrl + "checkSocialLogin",
					type : 'POST',
					data : data,
					contentType : "application/json"
				});
			},

			// Check credentials with the server and login user
			login : function() {
				// Compose credentials as JSON String
				var data = {name: this.get('name'), password: this.get('password')};
				data = JSON.stringify(data);

				// Check with the server for proper credentials
				return $.ajax({
					url : Utils.serviceUrl + "checkLogin",
					type : 'POST',
					data : data,
					contentType : "application/json"
				});
			},
			
			// Start delete user process
			deleteUser : function() {
				// Compose credentials as JSON String
				var data = {name:this.get('name') ,password: this.get('password')};
				data = JSON.stringify(data);

				// Check with the server for proper credentials
				return $.ajax({
					url : Utils.serviceUrl + "deleteUser",
					type : 'DELETE',
					data : data,
					contentType : "application/json",
					headers : {
						"Authorization" : Session.token
					}
				});
			},

			// Start change profile process
			changeProfile : function() {
				// Delete all fields that are not set
				for (var prop in this.attributes) {
					if (this.attributes[prop] === "" || prop === "langs") {
						this.unset(prop);
					}
				}

				// Compose credentials as JSON String
				var data = JSON.stringify(this.attributes);

				// Check with the server for proper credentials
				return $.ajax({
					url : Utils.serviceUrl + "changeProfile",
					type : 'POST',
					data : data,
					contentType : "application/json",
					headers : {
						"Authorization" : Session.token
					}
				});
			},

			// Start forgot password process
			forgotPassword : function() {
				var data = {name: this.get('name')};
				data = JSON.stringify(data);

				// Send forgot password request to the server
				return $.ajax({
					url : Utils.serviceUrl + "forgotPassword",
					type : 'POST',
					data : data,
					contentType : "application/json"
				});
			},

			// Start recover password process
			recoverPassword : function() {
				// Compose credentials as JSON String
				var data = {name:this.get('name') ,password: this.get('password')};
				data = JSON.stringify(data);

				// Send forgot password request to the server
				return $.ajax({
					url : Utils.serviceUrl + "recoverPassword",
					type : 'POST',
					data : data,
					contentType : "application/json"
				});
			},

			// Log activity to DB
			logActivity : function(time, site) {
				// Compose credentials as JSON String
				var data = {userName:this.get('name') ,duration: time, site: site};
				data = JSON.stringify(data);

				// Save activity to DB
				$.ajax({
					url : Utils.serviceUrl + "putActivity",
					type : 'PUT',
					data : data,
					contentType : "application/json"
				});
			},

			// Process evaluation invite
			processInvite : function(answer) {
				var urlSuffix = "processEvaluationInvite/user/" +
					this.get('name');
				return $.ajax({
					url : Utils.serviceUrl + urlSuffix,
					type : 'POST',
					data : answer,
					contentType : "application/json"
				});
			},

			// Inject evaluation participant flags
			injectParticipantFlag : function(participant, newc) {
				Session.user.evalParticipant = participant;
				Session.user.newcomer = newc;
				Session.user.newcomerRatings = 0;
				Utils.saveToLocalStorage(Session.user);
			},

			quitEvaluation : function() {
				return $.ajax({
					url : Utils.serviceUrl + "quitEvaluation",
					type : 'POST',
					data : Session.user.name,
					contentType : "application/json"
				});
			}
	});

	return UserModel;
});
