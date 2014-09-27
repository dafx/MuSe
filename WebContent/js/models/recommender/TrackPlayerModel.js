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
  'backbone'
], function($, _, Backbone){
  // TrackPlayer Model Definition
  // -------------------------------
  var TrackPlayerModel = Backbone.Model.extend({
    defaults : {
      tracksLoaded : false,
      tracks : [],
      urls : ""
    },

    initialize : function() {
      this.on("change:tracks", this.isFinished, this);
    },

    isFinished : function() {
      self = this;
      if (this.get("tracks").length === 10) {
        var urlString = this.get("urls");
        $.each(this.get("tracks"), function(index, track) {
          if (track && track !== "") {
            var url = track.split(":");
            if (url.length > 2) {
              if (index < self.get("tracks").length - 1) {
                urlString += url[2] + ",";
              } else {
                urlString += url[2];
              }
            }
          }
        });
        this.set("urls", urlString);
        this.set("tracksLoaded", true);
      }
    }
  });

  return TrackPlayerModel;
});
