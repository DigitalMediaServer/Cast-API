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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Application cannot be launched for some <code>reason</code>.
 */
@Immutable
public class LaunchErrorResponse extends StandardResponse {

	/** The error reason */
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final String reason;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param requestId the request ID.
	 * @param reason the error reason.
	 */
	public LaunchErrorResponse(@JsonProperty("requestId") long requestId, @JsonProperty("reason") String reason) {
		super(requestId);
		this.reason = reason;
	}

	/**
	 * @return The error reason.
	 */
	public String getReason() {
		return reason;
	}

	@JsonIgnore
	@Override
	public CastEventType getEventType() {
		return CastEventType.LAUNCH_ERROR;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [").append("requestId=").append(getRequestId());
		if (reason != null) {
			builder.append(", reason=").append(reason);
		}
		builder.append("]");
		return builder.toString();
	}
}
