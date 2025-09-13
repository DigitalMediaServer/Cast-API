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
import org.digitalmediaserver.cast.message.entity.ReceiverStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Response to "ReceiverStatus" request.
 */
@Immutable
public class ReceiverStatusResponse extends StandardResponse {

	/** The {@link ReceiverStatus} */
	@JsonProperty
	protected final ReceiverStatus status;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param requestId the request ID.
	 * @param status the {@link ReceiverStatus}.
	 */
	public ReceiverStatusResponse(
		@JsonProperty("requestId") long requestId,
		@JsonProperty("status") ReceiverStatus status
	) {
		super(requestId);
		this.status = status;
	}

	/**
	 * @return The {@link ReceiverStatus}.
	 */
	public ReceiverStatus getStatus() {
		return status;
	}

	@JsonIgnore
	@Override
	public CastEventType getEventType() {
		return CastEventType.RECEIVER_STATUS;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [").append("requestId=").append(getRequestId());
		if (status != null) {
			builder.append(", status=").append(status);
		}
		builder.append("]");
		return builder.toString();
	}
}
