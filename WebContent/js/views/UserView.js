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
  'utils',
  'models/UserModel'
], function($, _, Backbone, Bootstrap, Session, Utils, UserModel){
  // User View Definition
  // ---------------------
  var UserView = Backbone.View.extend({
    el : 'body',

    // Flag for click events
    clicked : false,

    initialize : function() {
      $("#logoutPill").click(this.logout);
      $("#forgotButton").click(function() {
        $("#loginDialog").modal('hide');
        $("#recoverDialog").modal('show');
      });
      this.changeView();
    },

    // Initialize event listeners
    events : {
      "click #loginButton" : "checkLogin",
      "click #registerButton" : "checkRegister",
      "click #deleteButton" : "deleteUser",
      "click #changeButton" : "changeProfile",
      "click #forgotPasswordButton" : "forgotPassword",
      "click #recoverButton" : "recoverPassword",
      "click #inviteEvalButtonYes" : "processInvite",
      "click #inviteEvalButtonNo" : "processInvite",
      "click #quitEvalButton" : "quitEvaluation",
      "click #registerInfo" : function() {
        if ($("#registerInfo").attr("data-open-text") === "Closed") {
          $("#registerInfo").attr("data-open-text", "Open");
          $("#registerInfo").text("- Weniger Informationen");
        } else {
          $("#registerInfo").attr("data-open-text", "Closed");
          $("#registerInfo").text("+ Mehr Informationen");
        }
      }
    },

    // Start registration process
    checkRegister : function() {
      // Avoid multiple click events fired
      if (this.clicked === true)
        return false;
      this.clicked = true;

      // Show loading indicator
      Utils.spinner("registerMessage");

      // Get the inserted data
      var name = $("#registerDialog :input[name='name']").val();
      var password = $("#registerDialog :input[name='password']").val();
      var passwordRepeat = $("#registerDialog " +
        ":input[name='passwordRepeat']").val();
      var email = $("#registerDialog :input[name='email']").val();
      var sex = $("#registerDialog :input[name='sex']").val();
      var birth = $("#registerDialog :input[name='birthyear']").val();
      var lfm = $("#registerDialog :input[name='lfmaccount']").val();
      var langs = [];
      $("#registerDialog :input[name='langs']:checked").each(
          function(i, item) {
            langs.push(item.value);
          });
      var terms = $("#registerDialog :input[name='terms']:checked")
          .val();

      // Create user model
      this.model = new UserModel({
        name : name,
        password : password,
        passwordRepeat : passwordRepeat,
        email : email,
        sex : sex,
        birthyear : birth,
        lfmaccount : lfm,
        langs : langs,
        terms : terms
      });

      // Check user data and register if it is correct
      var errors = this.model.evaluateData();

      if (errors.length === 0) {
        // Save user to database
        var self = this;
        $.when(this.model.register()).done(
          function(result) {
            // If server responded
            if (result === "success") {
              // Render success message
              Utils.msg("registerMessage", "success",
                "Successfully registered. You will be logged in automatically.");

              // Login automatically
              $("#loginForm :input[name='username']").val(name);
              $("#loginForm :input[name='password']").val(password);
              self.clicked = false;
              self.checkLogin();
            } else if (result === "lfm") {
              // LFM not valid
              Utils.msg("registerMessage", "danger",
                "The provided LastFM Account doesn't seem to exist.");
              self.clicked = false;
            } else if (result === "name") {
              // Name already taken
              Utils.msg("registerMessage", "danger",
                "This name is already taken.");
              self.clicked = false;
            }
          }).fail(function(jqXHR, textStatus, errorThrown) {
            Utils.error("registerMessage", jqXHR);
            self.clicked = false;
          });

      } else {

        // Compose error message
        var msg = "";
        for (var i = 0; i < errors.length; i++) {
          msg += "&bull; " + errors[i] + "<br>";
        }

        // Render error message
        Utils.msg("registerMessage", "danger", msg);
        this.clicked = false;
      }
    },

    // Start login process
    checkLogin : function() {
      // Avoid multiple click events fired
      if (this.clicked === true)
        return false;
      this.clicked = true;

      // Show loading indicator
      Utils.spinner("loginMessage");

      // Get the inserted data
      var name = $("#loginForm :input[name='username']").val();
      var password = $("#loginForm :input[name='password']").val();

      // Create user model
      this.model = new UserModel({
        name : name,
        password : password
      });

      // Handle callbacks
      var self = this;
      $.when(this.model.login()).done(
          function(result) {
            if (result !== 'none') {
              // Successfully logged in
              // Save information to session storage
              userObj = JSON.parse(result);
              userObj.name = userObj.name.toLowerCase();
              if (!userObj.lfmaccount) {
                userObj.lfmaccount = "";
              }
              Session.user = userObj;
              Session.token = Session.createToken(
                  Session.user.name, password.toString());
              window.sessionStorage.setItem("authToken", Session.token);
              Session.saveToLocalStorage(userObj);

              // Load views and redirect to options
              self.changeView();
              $("#loginDialog").modal('hide');
              $("#registerDialog").modal('hide');
              $("#loginMessage").html("");
              Backbone.history.navigate("recommender", true);
              if (userObj.evalParticipant === 'U' &&
                userObj.newcomer === false) {
                $("#inviteEvalDialog").modal('show');
              }
            } else {
              // Render error message
              Utils.msg("loginMessage", "danger",
                  "Wrong username and/or password.");
            }
            self.clicked = false;
          }).fail(function(jqXHR, textStatus, errorThrown) {
        Utils.error("loginMessage", jqXHR);
        self.clicked = false;
      });
    },

    // Start delete process
        deleteUser : function() {
          // Avoid multiple click events fired
          if (this.clicked === true)
            return false;
          this.clicked = true;

          // Show loading indicator
          Utils.spinner("deleteMessage");

          // Get the inserted data
          var pw = $("#deleteForm :input[name='passwordDelete']").val();

          // Create user model
          var user = new UserModel({
            name : Session.user.name,
            password : pw
          });

          // Handle callbacks
          var self = this;
          $.when(user.deleteUser()).done(
              function(result) {
                if (result === 'credentials') {
                  // Render error message
                  Utils.msg("deleteMessage", "danger",
                      "Wrong password.");
                } else {
                  self.logout();
                }
                self.clicked = false;
              }).fail(function(jqXHR, textStatus, errorThrown) {
            Utils.error("deleteMessage", jqXHR);
            self.clicked = false;
          });
        },

        // Start change process
        changeProfile : function() {
          // Avoid multiple click events fired
          if (this.clicked === true)
            return false;
          this.clicked = true;

          // Show loading indicator
          Utils.spinner("changeMessage");

          // Get the inserted data
          var password = $("#changeForm :input[name='password']").val();
          var passwordNew = $("#changeForm :input[name='passwordNew']").val();
          var passwordRepeat = $("#changeForm :input[name='passwordRepeat']"
            ).val();
          var emailNew = $("#changeForm :input[name='email']").val();
          var lfmNew = $("#changeForm :input[name='lfmaccount']").val();
          
          // Create user model
          this.model = new UserModel({
            name : Session.user.name,
            password : password,
            lfmaccount : lfmNew,
            email : emailNew
          });

          // Check for errors
          var errors = [];
          if (passwordNew === "" && emailNew === "" && lfmNew === "") {
            errors.push("You haven't changed anything.");
          }
          // Check for empty input (mustn't be changed)
          if (passwordNew !== "") {
            errors = this.model.evaluatePassword(passwordNew);
          }
          if (passwordNew !== passwordRepeat) {
            errors.push("New passwords do not match.");
          }
          if (password === "") {
            errors.push("Current Password is necessary.");
          }
          if (errors.length !== 0) {
            // Compose error message
            var msg = "";
            for (var i = 0; i < errors.length; i++) {
              msg += "&bull; " + errors[i] + "<br>";
            }

            // Render error message
            Utils.msg("changeMessage", "danger", msg);

            // Don't check server if new password is wrong
            this.clicked = false;
            return;
          }

          if (passwordNew !== "") {
            // (Hack!! Use gender field to transport new password to the
            // server...deserialization issues on backend side)
            this.model.set("sex", passwordNew);
          }

          // Handle callbacks
          var self = this;
          $.when(self.model.changeProfile()).done(
              function(result) {
                if (result === 'success') {
                  // Also change information in local cached user
                  if (lfmNew !== "") {
                    Session.user.lfmaccount = lfmNew;
                  }
                  if (emailNew !== "") {
                    Session.user.email = emailNew;
                  }
                  Session.trigger("changed");
                  
                  // Render success message
                  Utils.msg("changeMessage", "success",
                      "Successfuly changed profile.");
                } else if (result === 'password') {
                  // Render error message
                  Utils.msg("changeMessage", "danger",
                      "Current password not correct.");
                }
                self.clicked = false;
              }).fail(function(jqXHR, textStatus, errorThrown) {
            Utils.error("changeMessage", jqXHR);
            self.clicked = false;
          });
        },

        // Start forgot password
        forgotPassword : function() {
          // Avoid multiple click events fired
          if (this.clicked === true)
            return false;
          this.clicked = true;

          // Show loading indicator
          Utils.spinner("recoverMessage");

          // Get the inserted data
          var username = $("#recoverForm :input[name='username']").val()
              .toLowerCase();

          if (username.length === 0) {
            // Render error message
            Utils.msg("recoverMessage", "danger",
                "Please provide your username.");

            // skip execution if no name was provided
            this.clicked = false;
            return;
          }

          // Create user model
          this.model = new UserModel({
            name : username
          });

          // Change password
          var self = this;
          $.when(this.model.forgotPassword()).done(
              function(result) {
                if (result === 'success') {
                  // Render success message
                  Utils.msg("recoverMessage", "success",
                      "An email was sent to your address.");
                } else if (result === 'user') {
                  // Render error message
                  Utils.msg("recoverMessage", "danger",
                      "User not found.");
                }
                self.clicked = false;
              }).fail(function(jqXHR, textStatus, errorThrown) {
            Utils.error("recoverMessage", jqXHR);
            self.clicked = false;
          });
        },

        // Start recover password
        recoverPassword : function() {
          // Avoid multiple click events fired
          if (this.clicked === true)
            return false;
          this.clicked = true;

          // Show loading indicator
          Utils.spinner("recoverMessage");

          // Get the inserted data
          var token = $("#recoverForm :input[name='token']").val();
          var password = $("#recoverForm :input[name='password']").val();
          var passwordRepeat = $("#recoverForm :input[name='passwordRepeat']"
            ).val();

          // Create user model
          this.model = new UserModel({
            name : token,
            password : password,
            passwordRepeat : passwordRepeat
          });

          if (token.length === 0) {
            // Render error message
            Utils.msg("recoverMessage", "danger",
                "Please enter the token.");

            // Skip execution if no token was provided
            this.clicked = false;
            return;
          }
          var errors = this.model.evaluatePassword(password);
          if (password !== passwordRepeat) {
            errors.push("Passwords do not match.");
          }
          if (errors.length !== 0) {
            // Compose error message
            var msg = "";
            for (var i = 0; i < errors.length; i++) {
              msg += "&bull; " + errors[i] + "<br>";
            }

            // Render error message
            Utils.msg("recoverMessage", "danger", msg);

            // Don't check server if new password is wrong
            this.clicked = false;
            return;
          }

          // Change password
          var self = this;
          $.when(this.model.recoverPassword()).done(
              function(result) {
                if (result === 'success') {
                  // Render success message
                  Utils.msg("recoverMessage", "success",
                      "Successfuly recovered password.");
                } else if (result === 'token') {
                  // Render error message
                  Utils.msg("recoverMessage", "danger",
                      "Invalid token.");
                }
                self.clicked = false;
              }).fail(function(jqXHR, textStatus, errorThrown) {
            Utils.error("recoverMessage", jqXHR);
            self.clicked = false;
          });
        },

        // Start invitation process
        processInvite : function(event) {
          // Avoid multiple click events fired
          if (this.clicked === true)
            return false;
          this.clicked = true;

          // Show loading indicator
          Utils.spinner("inviteEvalMessage");

          // Get the inserted data
          var answer = event.toElement.id;
          var user = Session.user.name;
          if (answer === "inviteEvalButtonYes") {
            answer = "yes";
          } else {
            answer = "no";
          }

          // Create user model
          this.model = new UserModel({
            name : user
          });

          // Handle callbacks
          var self = this;
          $.when(this.model.processInvite(answer))
              .done(
                  function(result) {
                    if (result === "success") {
                      if (answer === "yes") {
                        self.model.injectParticipantFlag(
                            'Y', true);
                        document.location.reload(true);
                      } else {
                        $("#inviteEvalDialog")
                            .modal('hide');
                      }
                    } else if (result === "eval") {
                      // Render error message
                      Utils
                          .msg("inviteEvalMessage",
                              "danger",
                              "There is no evaluation running.");
                    } else if (result === "match") {
                      // Render error message
                      Utils
                          .msg("inviteEvalMessage",
                              "danger",
                              "Internal server error. Please try again.");
                    }
                    self.clicked = false;
                  }).fail(
                  function(jqXHR, textStatus, errorThrown) {
                    Utils
                        .error("inviteEvalMessage", jqXHR);
                    self.clicked = false;
                  });
        },

        logout : function() {
          // Destroy all views and clear session
          window.sessionStorage.clear();
          Backbone.history.navigate("home", true);
          window.location = "seeya.html";
        },

        changeView : function() {
          if(Session.user === '')
            return;

          // Hide login/register elements
          $("#loginPill").hide();
          $("#loginLink").hide();

          // Show user elements
          $("#profilePill").show();
          $("#logoutPill").show();
          $("#navrecommender").show();
          if (Session.user.evalParticipant !== 'Y') {
            $("#navoptions").show();
          }
          if (Session.user.password === 'admin') {
            $("#adminDivider").show();
            $("#adminHeader").show();
            $("#adminStats").show();
            $("#adminConfig").show();
            $("#adminLog").show();
            $("#adminEval").show();
          }
        },

        // Quit the running evaluation
        quitEvaluation : function() {
          // Avoid multiple click events fired
          if (this.clicked === true)
            return false;
          this.clicked = true;

          // Show loading indicator
          Utils.spinner("quitMessage");

          // Create user model
          this.model = new UserModel({
            name : Session.user.name
          });

          // Handle callbacks
          var self = this;
          $.when(this.model.quitEvaluation()).done(function() {
            // Set participants flags to false
            self.model.injectParticipantFlag('N', false);
            document.location.reload(true);
            self.clicked = false;
          }).fail(function(jqXHR, textStatus, errorThrown) {
            Utils.error("quitMessage", jqXHR);
            self.clicked = false;
          });
        }
      });

      return UserView;
});
