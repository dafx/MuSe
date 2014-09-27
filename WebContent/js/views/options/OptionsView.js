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
  'session',
  'models/options/OptionsModel',
  'views/options/OptionsListView',
  'text!templates/options/options.html'
], function($, _, Backbone, Session, OptionsModel, OptionsListView, OptionsTemplate){
    // OptionsView Definition
    // ----------------------
    var OptionsView = Backbone.View.extend({
      // Anchor element
      el : '#optionsview',

      events : {
        "click input[name='behavior']" : "updateBehavior"
      },

      initialize : function() {
        // Get the template
        this.template = OptionsTemplate;
        this.model = new OptionsModel();
        this.model.parentView = this;
      },

      // Render the content to the html node
      render : function() {
        this.$el.html(this.template);
        this.optionsListView = new OptionsListView({parentView: this});
        this.optionsListView.parentView = this;
        return this;
      },

      // Update selected behavior
      updateBehavior : function() {
        var behavior = $("input[name='behavior']:checked").val();
        this.model.set("behavior", behavior);
      },

      // Rebuild settings in the view
      restoreOptions : function(settings) {
        // Restore recommenders
        var recs = $("input[name='recommenders']");
        $.each(recs, function(index, rec) {
          var current = $(rec);
          var ids = settings.recommenders;
          for (var i = 0; i < ids.length; i++) {
            if (current.val() == ids[i]) {
              current.attr("checked", "checked");
            }
            // Check for last.fm account
            if (Session.user.lfmaccount === "" &&
              (current.val() == 4 || current.val() == 5)) {
              current.attr("disabled", "disabled");
            }
          }
        });

        // Restore behavior
        $("input[name='behavior'][value='" + settings.behavior + "']").attr(
            "checked", "checked");
        this.optionsListView.updateOverallSettings();
      }
    });

    return OptionsView;
});
