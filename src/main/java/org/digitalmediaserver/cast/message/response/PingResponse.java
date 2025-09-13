/*
 * Copyright (C) 2021 Digital Media Server developers.
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
package org.digitalmediaserver.cast.message.response;

import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.cast.event.CastEvent.CastEventType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Request to send 'Pong' message in reply.
 */
@Immutable
public class PingResponse extends StandardResponse {

	/**
	 * Creates a new instance using the specified request ID.
	 *
	 * @param requestId the request ID.
	 */
	protected PingResponse(@JsonProperty("requestId") long requestId) {
		super(requestId);
	}

	@JsonIgnore
	@Override
	public CastEventType getEventType() {
		// Should never be an event
		return null;
	}

	@Override
	public String toString() {
		return new StringBuilder(getClass().getSimpleName())
			.append(" [requestId=").append(getRequestId()).append("]")
			.toString();
	}
}
