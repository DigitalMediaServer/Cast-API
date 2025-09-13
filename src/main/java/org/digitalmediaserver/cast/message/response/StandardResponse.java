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
package org.digitalmediaserver.cast.message.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.cast.event.CastEvent.CastEventType;


/**
 * Parent class for transport object representing messages received FROM cast
 * devices.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "responseType", visible = true)
@JsonSubTypes({
	@JsonSubTypes.Type(name = "PING", value = PingResponse.class),
	@JsonSubTypes.Type(name = "PONG", value = PongResponse.class),
	@JsonSubTypes.Type(name = "RECEIVER_STATUS", value = ReceiverStatusResponse.class),
	@JsonSubTypes.Type(name = "ERROR", value = ErrorResponse.class),
	@JsonSubTypes.Type(name = "GET_APP_AVAILABILITY", value = AppAvailabilityResponse.class),
	@JsonSubTypes.Type(name = "INVALID_PLAYER_STATE", value = ErrorResponse.class),
	@JsonSubTypes.Type(name = "INVALID_REQUEST", value = ErrorResponse.class),
	@JsonSubTypes.Type(name = "MEDIA_STATUS", value = MediaStatusResponse.class),
	@JsonSubTypes.Type(name = "MULTIZONE_STATUS", value = MultizoneStatusResponse.class),
	@JsonSubTypes.Type(name = "CLOSE", value = CloseResponse.class),
	@JsonSubTypes.Type(name = "LOAD_CANCELLED", value = ErrorResponse.class),
	@JsonSubTypes.Type(name = "LOAD_FAILED", value = ErrorResponse.class),
	@JsonSubTypes.Type(name = "LAUNCH_ERROR", value = LaunchErrorResponse.class),
	@JsonSubTypes.Type(name = "DEVICE_ADDED", value = DeviceAddedResponse.class),
	@JsonSubTypes.Type(name = "DEVICE_UPDATED", value = DeviceUpdatedResponse.class),
	@JsonSubTypes.Type(name = "DEVICE_REMOVED", value = DeviceRemovedResponse.class)
})
@Immutable
public abstract class StandardResponse implements Response {

	/** The request ID */
	protected final long requestId;

	/**
	 * Abstract constructor.
	 *
	 * @param requestId the request ID.
	 */
	protected StandardResponse(@JsonProperty("requestId") long requestId) {
		this.requestId = requestId;
	}

	@Override
	@JsonProperty
	public long getRequestId() {
		return requestId;
	}

	/**
	 * @return The {@link CastEventType} to use when receiving this
	 *         {@link StandardResponse}.
	 */
	@Nullable
	@JsonIgnore
	public abstract CastEventType getEventType();
}
