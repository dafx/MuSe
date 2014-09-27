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
  'highcharts',
  'session',
  'utils',
  'collections/OptionsCollection',
  'models/options/OptionItemModel',
  'views/options/OptionItemView'
], function($, _, Backbone, Highcharts, Session, Utils, OptionsCollection, OptionItemModel, OptionItemView){
  // OptionsListView Definition
  // -----------------------------
  var OptionsListView = Backbone.View.extend({
    // Anchor element
    el : '#metricList',

    self : this,

    initialize : function() {
      // Set the model
      this.model = new OptionsCollection();
      Utils.recommenderList = this.model;
      this.model.on("fetch", this.loading, this);
      this.model.on("reset", this.render, this);
      this.model.fetch({
        reset : true
      }).fail(function(jqXHR, textStatus, errorThrown) {
        Utils.error("metricList", jqXHR);
      });
    },

    events : {
      "click input[name='recommenders']" : "updateRecommenders"
    },

    // Render the content to the html node
    render : function() {
      this.$el.html("");

      // Render overall diagram
      $("#recSettings").highcharts({
        chart : {
          height : 250,
          width : 350
        },
        title : {
          text : null
        },
        tooltip : {
          enabled : 'false'
        },
        legend : {
          enabled : false
        },
        yAxis : {
          title : {
            text : null
          },
          plotLines : [ {
            value : 0,
            width : 1,
            color : '#808080'
          } ],
          labels : {
            formatter : function() {
              return this.value + " %";
            }
          },
          max : 100,
          min : 0
        }
      });

      // Render each recommender
      var recommenders = this.model.models[0].attributes;
      for ( var prop in recommenders) {
        // Create View for a single recommender
        var rec = new OptionItemModel(recommenders[prop]);
        this.$el.append(new OptionItemView({
          model : rec
        }).render().el);

        // Compose data by cycling trough all object properties
        var props = [];
        var values = [];
        for (var p in rec.get("tagDistribution")) {
          props.push(p);
          values.push(rec.get("tagDistribution")[p]);
        }

        $("#" + rec.get("ID") + "Metric").highcharts({
          chart : {
            height : 150,
            width : 300
          },
          title : {
            text : null
          },
          tooltip : {
            enabled : 'false'
          },
          legend : {
            enabled : false
          },
          xAxis : {
            categories : props
          },
          yAxis : {
            title : {
              text : null
            },
            plotLines : [ {
              value : 0,
              width : 1,
              color : '#808080'
            } ],
            labels : {
              formatter : function() {
                return this.value + " %";
              }
            },
            max : 100,
            min : 0
          },
          plotOptions : {
            line : {
              dataLabels : {
                enabled : false
              },
              enableMouseTracking : false
            }
          },
          series : [ {
            name : 'Characteristics',
            data : values
          } ]
        });
      }

      // Init information popover
      $("#optionsInfo").popover();
      this.parentView.model.getSettings();
      return this;
    },

    // Send Information
    loading : function() {
      Utils.spinner("metricList");
    },

    updateRecommenders : function() {
      var recs = $("input[name='recommenders']:checked");
      var ids = [];
      $.each(recs, function(index, rec) {
        ids.push($(rec).val());
      });
      // Set model values
      this.parentView.model.set("recommenders", ids);
      this.updateOverallSettings();
    },

    // Update selected settings
    updateOverallSettings : function() {
      var recs = $("input[name='recommenders']:checked");
      var ids = [];
      $.each(recs, function(index, rec) {
        ids.push($(rec).val());
      });

      // Draw chart for hybrid recommender attributes
      $("#recSettings").html(" ");

      // Compose data by cycling trough all object properties
      var props = [];
      var values = [];
      var data = this.model.getIntersectionValues(ids);
      for (var p in data) {
        props.push(p);
        values.push(data[p]);
      }
      $("#recSettings").highcharts({
        chart : {
          height : 250,
          width : 350
        },
        title : {
          text : null
        },
        tooltip : {
          enabled : 'false'
        },
        legend : {
          enabled : false
        },
        xAxis : {
          categories : props
        },
        yAxis : {
          title : {
            text : null
          },
          plotLines : [ {
            value : 0,
            width : 1,
            color : '#808080'
          } ],
          labels : {
            formatter : function() {
              return this.value + " %";
            }
          },
          max : 100,
          min : 0
        },
        plotOptions : {
          line : {
            dataLabels : {
              enabled : false
            },
            enableMouseTracking : false
          }
        },
        series : [ {
          name : 'Characteristics',
          data : values
        } ]
      });
    }
  });

  return OptionsListView;
});
