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
define({
  // API Url to connect to the backend
  serviceUrl: 'API/',

  // Get recommenders that are available in the system
  getRecommenderList: function(filter) {
    return $.getJSON(this.serviceUrl + "getRecommenders/filter/" + filter);
  },

  // Show loading indicator at the given 'anchor' element
  spinner: function(anchor) {
    $("#" + anchor).html("<center><img src='img/loader.gif'" +
      " style='margin-top: 10px;'></img><center>");
  },

  // Show a message at the 'anchor' element, given a certain status and content
  msg: function(anchor, status, content) {
    $("#" + anchor).html("<div class='alert alert-" + status + "'>" +
      content + "</div>");
  },

  // Show an error message at the 'anchor' element, given a xhr callback
  error: function(anchor, xhr) {
    if (xhr.status == 404) {
      $("#" + anchor).html("<div class='alert alert-danger'> Not Found (" +
        xhr.status + "). Server not reachable.</div>");
    } else if (xhr.status === 0) {
      $("#" + anchor).html("<div class='alert alert-danger'> Unknown (" +
        xhr.status + "). Something went wrong.</div>");
    } else if (xhr.responseText.length < 50) {
      $("#" + anchor).html("<div class='alert alert-danger'>" + xhr.statusText +
        " (" + xhr.status + "). " + xhr.responseText + "</div>");
    } else {
      $("#" + anchor).html("<div class='alert alert-danger'>" + xhr.statusText +
        " (" + xhr.status + "). </div>");
    }
  },

  // Get date of today as String
  getTodayStr: function() {
    var today, month, day;
    today = new Date();
    month = today.getMonth() + 1;
    month = month < 10 ? '0' + month : '' + month;
    day = today.getDate() < 10 ? '0' + today.getDate() : '' +
      today.getDate();
    return today.getFullYear() + "-" + month + "-" + day;
  },

  //Check for mobile browsers
  isMobile: function() {
    var isMobile = (/iphone|ipod|android|ie|blackberry|fennec/)
      .test(navigator.userAgent.toLowerCase());
    return isMobile;
  }
});
