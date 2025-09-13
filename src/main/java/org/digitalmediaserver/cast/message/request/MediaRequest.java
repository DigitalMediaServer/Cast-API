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
 * An abstract request for an action with a media referenced by a specific
 * media session ID.
 */
public abstract class MediaRequest extends StandardRequest {

	/** The media session ID */
	@JsonProperty
	protected final int mediaSessionId;

	/** The session ID */
	@JsonProperty
	protected final String sessionId;

	/**
	 * Abstract constructor.
	 *
	 * @param mediaSessionId the media session ID.
	 * @param sessionId the session ID.
	 */
	public MediaRequest(int mediaSessionId, String sessionId) {
		this.mediaSessionId = mediaSessionId;
		this.sessionId = sessionId;
	}

	/**
	 * @return The media session ID.
	 */
	public int getMediaSessionId() {
		return mediaSessionId;
	}

	/**
	 * @return The session ID.
	 */
	public String getSessionId() {
		return sessionId;
	}
}
