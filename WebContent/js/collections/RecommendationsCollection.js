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
  'raty',
  'session',
  'utils',
  'models/recommender/RecommendationModel'
], function($, _, Backbone, Raty, Session, Utils, RecommendationModel) {
  // RecommendationsCollection Definition
  // ----------------------------------------
  var RecommendationsCollection = Backbone.Collection.extend({

    // The corresponding model
    model: RecommendationModel,

    // REST URL
    url: "getRecommendations/user/",

    initialize: function() {
      this.url = Utils.serviceUrl + this.url + Session.user.name;
    },

    /**
     * Save given ratings of the user to the database.
     */
    saveRatings: function() {
      var data = $(".rating").serializeArray();
      data.push({
        name: "list",
        value: $("#listRating").raty('score')
      });

      return $.ajax({
        url: Utils.serviceUrl + "putRatings",
        type: 'PUT',
        data: data,
        headers: {
          "Authorization": Session.token
        }
      });
    },

    /**
     *
     * Check for minimum number of ratings.
     *
     * @returns {Boolean} True if number of needed ratings done, false
     *          otherwise.
     */
    checkRatings: function() {
      var fields = $(".rating").length;
      var ratings = $(".rating").serializeArray();

      if (ratings.length >= Math.floor(fields / 6)) {
        return true;
      }
      return false;
    },

    /**
     * Check if at least one recommender is set for use in options tab
     *
     * @returns {Boolean} True if at least one recommender is chosen, false
     *          otherwise.
     */
    checkOptions: function() {
      if (this.parentView.optionsView) {
        var settings = JSON.parse(window.sessionStorage.getItem("opts"));
        return settings.recommenders.length !== 0;
      }
      return true;
    },

    /**
     * Create a new list of recommendations for the user.
     */
    createRecommendationsList: function() {
      return $.ajax({
        url: Utils.serviceUrl + "createRecommendations/user/" + Session.user.name,
        type: 'POST',
        headers: {
          "Authorization": Session.token
        }
      });
    }
  });

  return RecommendationsCollection;
});
