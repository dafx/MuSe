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
  'models/options/OptionItemModel'
], function($, _, Backbone, Utils, Session, OptionItemModel){
  // OptionsModel Definition
  // -------------------------------
  var OptionsModel = Backbone.Model.extend({
    // Model attributes
    defaults : {
      recommenders : "",
      behavior : ""
    },

    // REST URL
    url : "getOptions/user/",

    initialize : function() {
      this.url = Utils.serviceUrl + this.url + Session.user.name;
      this.on("change", this.storeLocal);
    },

    // Get settings of the user
    getSettings : function(callback) {
      // Check if session contains settings
      var settings = JSON.parse(window.sessionStorage.getItem("opts"));
      if (settings) {
        this.setAll(settings);
        this.parentView.restoreOptions(this.attributes);
        return;
      }

      // Fetch settings
      var self = this;
      $.getJSON(Utils.serviceUrl + "getOptions/user/" + Session.user.name,
              function(data) {
                self.setAll(data);
                // Store settings to session storage
                window.sessionStorage.setItem("opts", JSON
                    .stringify(data));
                window.sessionStorage.setItem("optsOriginal", JSON
                    .stringify(data));
                self.parentView.restoreOptions(self.attributes);
              });
    },

    // Restore received values to the model
    setAll : function(settings) {
      this.set("recommenders", settings.recommenders);
      this.set("behavior", settings.behavior);
    },

    // Store changed settings in session storage
    storeLocal : function() {
      window.sessionStorage.setItem("opts", JSON.stringify(this.attributes));
    },

    // Store changed settings in database
    storeBackend : function() {
      var url = Utils.serviceUrl + "updateOptions/user/" + Session.user.name;
      var data = JSON.stringify(this.attributes);

      $.ajax({
        url : url,
        type : 'PUT',
        data : data,
        headers : {
          "Authorization" : Session.token
        },
        contentType : "application/json",
        success : function() {
          $("#optsChanged").show();
        }
      });
      window.sessionStorage.setItem("optsOriginal", JSON
          .stringify(this.attributes));

    },

    // Check for equal options
    checkEqual : function() {
      var settings = JSON
          .parse(window.sessionStorage.getItem("optsOriginal"));
      // Check for equal behavior
      if (this.get("behavior") != settings.behavior ||
          !this.arrayIsEqual(this.get("recommenders"), settings.recommenders)) {
        return false;
      } else {
        return true;
      }
    },

    // Check for equal arrays of recommender ids
    arrayIsEqual : function(arrA, arrB) {

      // Check if lengths are different
      if (arrA.length !== arrB.length)
        return false;

      // String compare
      var cA = arrA.slice().sort().join(",");
      var cB = arrB.slice().sort().join(",");

      return cA === cB;
    }

  });

  return OptionsModel;
});
