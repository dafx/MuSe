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
package de.mrms.web;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * API exception. Like standard {@see} WebApplicatioException but providing a
 * messsage what went wrong for the frontend.
 */
@SuppressWarnings("serial")
public class MuseWebException extends WebApplicationException {
  public MuseWebException(String message) {
    super(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(message).type(MediaType.TEXT_PLAIN).build());
  }
}
