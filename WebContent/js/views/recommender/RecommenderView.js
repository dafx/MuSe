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
  'session',
  'raty',
  'collections/RecommendationsCollection',
  'views/recommender/RecommendationItemView',
  'views/recommender/TrackPlayerView',
  'text!templates/recommender/home.html'
], function($, _, Backbone, Utils, Session, Raty, RecommendationsCollection,
    RecommendationItemView, TrackPlayerView, RecommenderTemplate){
  // RecommendationView Definition
  // ------------------------------
  var RecommenderView = Backbone.View.extend({
        // Anchor element
        el : '#recommenderview',
        anchor : '#recommenderContent',
        trackList : '#trackList',
        details : {},

        // Other members
        sending : false,

        // Initialize View
        initialize : function() {

          // Get the template
          this.template = _.template(RecommenderTemplate);

          // Set the model
          var self = this;
          this.model = new RecommendationsCollection();
          this.model.parentView = this;
          this.model.on("request", function(model, xhr, opts) {
        	self.loading();
            $.when(xhr).done(function(result) {
              // Render if all was going fine
              self.render();
            }).fail(function() {
              Utils.error("recommenderview", xhr);
            });
          });

          // Fetch recommendations from server
          this.model.fetch({cache: false});
        },

        events : {
          "click #sendButton" : "sendRatings",
          "click #refreshButton" : "refreshList"
        },

        // Render the content to the html node
        render : function() {
          // Render template
          var opts = {
            changed : true,
            eval : Session.user.evalParticipant,
            duration : Session.user.evalDuration,
            newcomer : Session.user.newcomer,
            newcomerRatings : Session.user.newcomerRatings
          };
          if (this.optionsView) {
            if (this.optionsView.model.checkEqual() === false) {
              this.optionsView.model.storeBackend();
              opts.changed = false;
            }
          }

          $(this.anchor).html(this.template(opts));
          $("#recInfo").popover();
          $("#listRating").raty({
            path : 'img'
          });
          $('#ratingBar').affix({
            offset : {
              top : $('#ratingBar').offset().top
            }
          });

          // Check if list was empty (No more recommendations)
          if (this.model.models.length === 0) {
            $("#recOut").show();
            $("#sendButton").hide();
            return;
          }

          // Render each recommendation
          this.trackPlayerView = new TrackPlayerView();
          var self = this;
          $.each(this.model.models, function(index, rec) {
            rec.set("order", index);
            rec.set("newcomer", Session.user.newcomer);
            rec.parentView = self;
            $(self.trackList).append(new RecommendationItemView({
              model : rec
            }).render().el);
          });

          return this;
        },

        // Send Information
        loading : function() {
          Utils.spinner("recommenderContent");
        },

        // Send Information
        sendRatings : function(e) {
          // Avoid multiple firing of one click
          if (this.sending === true)
            return false;
          this.sending = true;
          var numRatings = $(".rating").serializeArray().length;

          $(window).scrollTop($('#messageRecommendationTab').offset().top);

          // Check for minimum number of recommenders
          var errorMsg;
          if (!this.model.checkOptions()) {
            errorMsg = "Please choose at least one recommender in the settings.";
            Utils.msg("messageRecommendationTab", "danger", errorMsg);
            this.sending = false;
          }
          // Check for minimum number of ratings
          else if (!this.model.checkRatings()) {
            var fields = $(".rating").length;
            errorMsg = "Please rate at least " + Math.floor(fields / 6)
                + " recommendations.";
            Utils.msg("messageRecommendationTab", "danger", errorMsg);
            this.sending = false;
          } else if (!$("#listRating").raty('score')) {
            errorMsg = "Please provide a number of stars for this list.";
            Utils.msg("messageRecommendationTab", "danger", errorMsg);
            this.sending = false;
          } else {

            // Show loading indicator
            Utils.spinner("messageRecommendationTab");

            // Save ratings
            var self = this;
            $.when(this.model.saveRatings()).done(function() {
              // Save rating count for newcomers
              if (Session.user.newcomer === true) {
                Session.user.newcomerRatings += numRatings;
                Session.saveToLocalStorage(Session.user);
              }
              self.refreshView();
            }).fail(function() {
              // Render error message
              Utils.msg("messageRecommendationTab", "danger",
                "Sending failed. Please try again.");
              self.sending = false;
            });
          }
        },

        // Refresh recommendation list without ratings when settings have
        // changed.
        refreshList : function() {
          // Avoid multiple firing of one click
          if (this.sending === true)
            return false;
          this.sending = true;

          // Check for minimum number of recommenders
          if (!this.model.checkOptions()) {
            var errorMsg = "Please choose at least one recommender in the settings tab.";
            Utils.msg("messageRecommendationTab", "danger",
                errorMsg);
            this.sending = false;
          } else {
            $("#optsChanged").hide();
            this.refreshView();
          }
        },

        // Refresh the view with a new list of recommendations
        refreshView : function() {
          // Show loading indicator
          $(window).scrollTop($('#messageRecommendationTab').offset().top);
          Utils.spinner("messageRecommendationTab");

          // Get a new list and redraw view
          var self = this;
          $.when(this.model.createRecommendationsList()).done(function() {
            $(self.anchor).empty();
            self.undelegateEvents();
            new RecommenderView();
          }).fail(function() {
            // Render error message
            Utils.msg("messageRecommendationTab", "danger",
              "Sending Failed. Please try again.");
            self.sending = false;
          });
        }
      });

      return RecommenderView;
});
