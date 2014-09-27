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
  'session'
], function($, _, Backbone, Session) {

  // App Router Definition
  // ---------------------
  var Router = Backbone.Router.extend({
    views: {},
    timeStart: 0,
    timeEnd: 0,
    last: '',

    current: '',

    routes: {
      '': 'home',
      "home": "home",
      "options": "options",
      "recommender": "recommender",
      "statistics": "statistics",
      "help": "help",
      "profile": "profile",
      "configuration": "configuration",
      "log": "log",
      "evaluation?details=:evalId": "details",
      "evaluation": "evaluation"
    },

    home: function() {
      this.current = "home";
      var self = this;
      require(['views/HomeView'], function(HomeView) {
        if (!self.views.homeView) {
          self.views.homeView = new HomeView();
        }
      });
    },

    options: function() {
      // User authentication
      if (Session.user === "") {
        this.navigate("home");
        $("#loginDialog").modal('show');
        return;
      } else if (Session.user.evalParticipant === 'Y') {
        return;
      }

      // Show view
      this.current = "options";
      var self = this;
      require(['views/options/OptionsView'], function(OptionsView) {
        if (!self.views.optionsView) {
          self.views.optionsView = new OptionsView();
          self.views.optionsView.render();
        }
      });
    },

    recommender: function() {
      // User authentication
      if (Session.user === "") {
        this.navigate("home");
        $("#loginDialog").modal('show');
        return;
      }

      // Show view
      this.current = "recommender";
      var self = this;
      require(['views/recommender/RecommenderView'], function(RecommenderView) {
        if (!self.views.recommenderView) {
          self.views.recommenderView = new RecommenderView();
          self.views.recommenderView.optionsView = self.views.optionsView || false;
        } else {
          // Check for changed options
          if (self.views.optionsView) {
            if (self.views.optionsView.model.checkEqual() === false) {
              $("#optsChanged").hide();
              self.views.optionsView.model.storeBackend();
            }
          }
        }
      });
    },

    profile: function() {
      // User authentication
      if (Session.user === "") {
        this.navigate("home");
        $("#loginDialog").modal('show');
        return;
      }

      // Show view
      this.current = "profile";
      var self = this;
      require(['views/ProfileView'], function(ProfileView) {
        if (!self.views.profileView) {
          self.views.profileView = new ProfileView();
        }
      });
    },

    statistics: function() {
      // User authentication
      if (Session.user === "") {
        this.navigate("home");
        $("#loginDialog").modal('show');
        return;
      } else if (Session.user.password !== 'admin') {
        return;
      }

      // Show view
      this.current = "statistics";
      var self = this;
      require(['views/StatisticView'], function(StatisticView) {
        if (!self.views.statisticView) {
          self.views.statisticView = new StatisticView();
        }
      });
    },

    evaluation: function() {
      // User authentication
      if (Session.user === "") {
        this.navigate("home");
        $("#loginDialog").modal('show');
        return;
      } else if (Session.user.password !== 'admin') {
        return;
      }

      // Show view
      this.current = "evaluation";
      var self = this;
      require(['views/evaluation/EvaluationView'], function(EvaluationView) {
        if (!self.views.evaluationView) {
          self.views.evaluationView = new EvaluationView();
        }
      });
    },
    
    configuration: function() {
        // User authentication
        if (Session.user === "") {
          this.navigate("home");
          $("#loginDialog").modal('show');
          return;
        } else if (Session.user.password !== 'admin') {
          return;
        }

        // Show view
        this.current = "configuration";
        var self = this;
        require(['views/ConfigurationView'], function(ConfigurationView) {
          if (!self.views.configurationView) {
            self.views.configurationView = new ConfigurationView();
          }
        });
      },

    help: function() {
      // Show view
      this.current = "help";
      var self = this;
      require(['views/HelpView'], function(HelpView) {
        if (!self.views.helpView) {
          self.views.helpView = new HelpView();
        }
      });
    },

    log: function() {
      // Show view
      this.current = "log";
      var self = this;
      require(['views/LogView'], function(LogView) {
        if (!self.views.logView) {
          self.views.logView = new LogView();
        }
      });
    },

    details: function(evalId) {
      // User authentication
      if (Session.user === "") {
        this.navigate("home");
        $("#loginDialog").modal('show');
        return;
      } else if (Session.user.password !== 'admin') {
        return;
      }

      // Show view
      this.current = "evaldetails";
      var self = this;
      require(['views/evaluation/EvaluationDetailsView'], function(EvaluationDetailsView) {
        if (self.views.detailsView) {
          self.views.detailsView.undelegateEvents();
        }
        self.views.detailsView = new EvaluationDetailsView({
          id: evalId
        });
      });
    },

    changeView: function() {
      // Hide everything
      $(".content").hide();
      $("#header .nav li").removeClass('active');
      // Show current section
      $("#" + this.current + "view").show();
      $("#nav" + this.current).addClass('active');
    }
  });

  var initialize = function() {

    // Start routing
    var router = new Router();
    Backbone.history.start();
    router.changeView();

    // Trigger when routing happens
    router.on('route', function(name, args) {
      // Measure and Log time spent in the view
      if (Session.user && false) {
        var d = new Date();
        if (router.last !== "" && router.last != "statistics" &&
          router.last != "log" && router.last != "evaluation" &&
          router.last != "details") {
          router.timeEnd = d.getTime() / 1000;
          var time = router.timeEnd - router.timeStart;
          this.views.userView.model.logActivity(time, router.last);
        }
        router.timeStart = d.getTime() / 1000;
        router.last = name;
      }

      // Change to the wanted view
      router.changeView();
      $(document).scrollTop(0);
    });
  };
  return {
    initialize: initialize
  };
});
