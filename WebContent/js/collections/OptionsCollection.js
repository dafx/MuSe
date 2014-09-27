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
  'models/options/OptionItemModel'
], function($, _, Backbone, Utils, OptionItemModel) {
  // OptionsCollection Definition
  // ----------------------------------------
  var OptionsCollection = Backbone.Collection.extend({
    // The corresponding model
    model: OptionItemModel,

    // REST URL
    url: "getRecommenders/filter/active",

    initialize: function() {
      this.url = Utils.serviceUrl + this.url;
    },

    // Get mean values of all attributes of the selected recommenders
    getIntersectionValues: function(ids) {
      // Check for legal input
      if (ids.length === 0) {
        return -1;
      }

      var recommenders = this.models[0].attributes,
        result = {},
        tags = recommenders[ids[0]].tagDistribution,
        tag, i, k, prop, rec;

      // Generate result object
      for (tag in tags) {
        result[tag] = 0;
      }

      // Cycle trough all selected recommenders
      for (i = 0; i < ids.length; i++) {
        // Sum up all the attribute values
        rec = recommenders[ids[i]].tagDistribution;
        for (k in rec) {
          result[k] += rec[k];
        }
      }

      // Compute means of all the attributes
      for (prop in result) {
        result[prop] = result[prop] / ids.length;
      }
      return result;
    }
  });
  return OptionsCollection;
});
