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


/**
 * A request to pause playback of a media referenced by a specific media
 * session ID.
 */
public class Pause extends MediaRequest {

	/**
	 * Creates a new request to pause playback of the media referenced by
	 * the specified media session ID.
	 *
	 * @param mediaSessionId the media session ID for which the pause
	 *            request applies.
	 * @param sessionId the session ID to use.
	 */
	public Pause(int mediaSessionId, String sessionId) {
		super(mediaSessionId, sessionId);
	}
}
