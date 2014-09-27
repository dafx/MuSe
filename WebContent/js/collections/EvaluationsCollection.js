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
  'models/evaluation/EvaluationModel'
], function($, _, Backbone, Utils, EvaluationModel) {
  // EvaluationList Collection Definition
  // ----------------------------------------
  var EvaluationsCollection = Backbone.Collection.extend({

    // The corresponding model
    model: EvaluationModel,

    // REST URL
    url: "getEvaluationHistory",

    initialize: function() {
      this.url = Utils.serviceUrl + this.url;
    }
  });

  return EvaluationsCollection;
});
