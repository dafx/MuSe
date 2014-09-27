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
  'text!templates/help.html'
], function($, _, Backbone, helpTemplate){
  // Help View Definition
  // -------------------
  var HelpView = Backbone.View.extend({
    // Anchor element
    el : '#helpview',

    // Initialize View
    initialize : function() {
      // Render right away
      this.render();
    },

    // Render the content to the html node
    render : function() {
      // Render template
      this.$el.html(helpTemplate);
      return this;
    }
  });

  return HelpView;
});
