/*
 * Copyright 2014 Vitaly Litvak (vitavaque@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.digitalmediaserver.cast.message.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Request to stop an application associated with a specific session ID.
 */
public class Stop extends StandardRequest {

	/** The session ID */
	@JsonProperty
	protected final String sessionId;

	/**
	 * Creates a new instance using the specified session ID.
	 *
	 * @param sessionId the session ID to use.
	 */
	public Stop(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * @return The session ID of this request.
	 */
	public String getSessionId() {
		return sessionId;
	}
}
