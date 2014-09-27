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
  'utils'
], function($, _, Backbone, Raty, Session, Utils){
  // RecommendationModel Definition
  // -------------------------------
  var RecommendationModel = Backbone.Model.extend({

    defaults : {
      metadata : "pending"
    },

    initialize : function() {
      // Map to percent value of 2
      this.set("score", this.get("score") * 50);
      // Match recommendation with spotify to produce a player
      this.matchToSpotify();
    },

    matchToSpotify : function() {
      // Create query string
      var self = this;
      var artist = this.get('song').artist;
      var name = this.get('song').name;
      var searchTerm = artist + " " + name;
      var tracks = {};

      // Query Spotify metadata API
      var query = "https://ws.spotify.com/search/1/track.json?q=" +
        encodeURIComponent(searchTerm);
      $.ajax({
        url : query,
        type : 'GET',
        cache : true,
        dataType : 'json'
      }).done(function(data) {
        if ($.type(data.tracks[0]) !== "undefined") {
          self.set({
            metadata : data.tracks[0].href
          });
          tracks = self.parentView.trackPlayerView.model.get("tracks").slice();
          tracks[self.get("order")] = data.tracks[0].href;
          self.parentView.trackPlayerView.model.set("tracks", tracks);

        } else {
          self.set({
            metadata : " "
          });
          tracks = self.parentView.trackPlayerView.model.get("tracks").slice();
          tracks[self.get("order")] = "";
          self.parentView.trackPlayerView.model.set("tracks", tracks);
        }
      });
    }

  });

  return RecommendationModel;
});
