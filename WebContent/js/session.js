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
  // Object representing the active user
  user: '',
  // Authentication token
  token: '',
  // Flag indicating wether the user is mobile
  isMobile: false,

  // Check if there is an active session
  restore: function() {
    // Check the sessionStorage
    var token = window.sessionStorage.getItem("authToken");
    if (token) {
      // Session found. Load other information from session
      this.token = token;
      this.user = this.loadFromLocalStorage();
    }
  },

  createToken: function(user, password) {
    var tok = user + ':' + password,
      hash = btoa(tok);
    return "Basic " + hash;
  },

  saveToLocalStorage: function(user) {
    var obj = btoa(unescape(encodeURIComponent(JSON.stringify(user))));
    window.sessionStorage.setItem("userObj", obj);
  },

  loadFromLocalStorage: function() {
    return JSON.parse(decodeURIComponent(escape(window
      .atob(window.sessionStorage.getItem("userObj")))));
  }
});
