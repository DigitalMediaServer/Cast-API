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
package org.digitalmediaserver.cast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Map;

/**
 * Parent class for transport object representing messages received FROM
 * ChromeCast device.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "responseType")
@JsonSubTypes({
	@JsonSubTypes.Type(name = "PING", value = StandardResponse.PingResponse.class),
	@JsonSubTypes.Type(name = "PONG", value = StandardResponse.PongResponse.class),
	@JsonSubTypes.Type(name = "RECEIVER_STATUS", value = StandardResponse.ReceiverStatusResponse.class),
	@JsonSubTypes.Type(name = "GET_APP_AVAILABILITY", value = StandardResponse.AppAvailabilityResponse.class),
	@JsonSubTypes.Type(name = "INVALID_REQUEST", value = StandardResponse.InvalidResponse.class),
	@JsonSubTypes.Type(name = "MEDIA_STATUS", value = StandardResponse.MediaStatusResponse.class),
	@JsonSubTypes.Type(name = "MULTIZONE_STATUS", value = StandardResponse.MultizoneStatusResponse.class),
	@JsonSubTypes.Type(name = "CLOSE", value = StandardResponse.CloseResponse.class),
	@JsonSubTypes.Type(name = "LOAD_FAILED", value = StandardResponse.LoadFailedResponse.class),
	@JsonSubTypes.Type(name = "LAUNCH_ERROR", value = StandardResponse.LaunchErrorResponse.class),
	@JsonSubTypes.Type(name = "DEVICE_ADDED", value = StandardResponse.DeviceAddedResponse.class),
	@JsonSubTypes.Type(name = "DEVICE_UPDATED", value = StandardResponse.DeviceUpdatedResponse.class),
	@JsonSubTypes.Type(name = "DEVICE_REMOVED", value = StandardResponse.DeviceRemovedResponse.class)
})
public abstract class StandardResponse implements Response {

	protected Long requestId;

	@Override
	public final Long getRequestId() {
		return requestId;
	}

	@Override
	public final void setRequestId(Long requestId) {
		this.requestId = requestId;
	}

	/**
	 * Request to send 'Pong' message in reply.
	 */
	public static class PingResponse extends StandardResponse {
	}

	/**
	 * Response in reply to 'Ping' message.
	 */
	public static class PongResponse extends StandardResponse {
	}

	/**
	 * Request to 'Close' connection.
	 */
	public static class CloseResponse extends StandardResponse {
	}

	/**
	 * Identifies that loading of media has failed.
	 */
	public static class LoadFailedResponse extends StandardResponse {
	}

	/**
	 * Request was invalid for some <code>reason</code>.
	 */
	public static class InvalidResponse extends StandardResponse {

		protected final String reason;

		public InvalidResponse(@JsonProperty("reason") String reason) {
			this.reason = reason;
		}
	}

	/**
	 * Application cannot be launched for some <code>reason</code>.
	 */
	public static class LaunchErrorResponse extends StandardResponse {

		protected final String reason;

		public LaunchErrorResponse(@JsonProperty("reason") String reason) {
			this.reason = reason;
		}
	}

	/**
	 * Response to "ReceiverStatus" request.
	 */
	public static class ReceiverStatusResponse extends StandardResponse {

		@JsonProperty
		protected final ReceiverStatus status;

		public ReceiverStatusResponse(@JsonProperty("status") ReceiverStatus status) {
			this.status = status;
		}
	}

	/**
	 * Response to "MediaStatus" request.
	 */
	public static class MediaStatusResponse extends StandardResponse {

		protected final MediaStatus[] statuses;

		public MediaStatusResponse(@JsonProperty("status") MediaStatus... statuses) {
			this.statuses = statuses;
		}
	}

	/**
	 * Response to "AppAvailability" request.
	 */
	public static class AppAvailabilityResponse extends StandardResponse {

		@JsonProperty
		protected Map<String, String> availability;
	}

	/**
	 * Multi-Zone status for the case when there are multiple cast devices in
	 * different zones (rooms).
	 */
	public static class MultizoneStatusResponse extends StandardResponse {

		@JsonProperty
		protected final MultizoneStatus status;

		public MultizoneStatusResponse(@JsonProperty("status") MultizoneStatus status) {
			this.status = status;
		}
	}

	/**
	 * Received when power is cycled on ChromeCast Audio devices in a group.
	 */
	public static class DeviceAddedResponse extends StandardResponse {

		protected final Device device;

		public DeviceAddedResponse(@JsonProperty("device") Device device) {
			this.device = device;
		}
	}

	/**
	 * Received when volume is changed in ChromeCast Audio group.
	 */
	public static class DeviceUpdatedResponse extends StandardResponse {

		protected final Device device;

		public DeviceUpdatedResponse(@JsonProperty("device") Device device) {
			this.device = device;
		}
	}

	/**
	 * Received when power is cycled on ChromeCast Audio devices in a group.
	 */
	public static class DeviceRemovedResponse extends StandardResponse {

		protected final String deviceId;

		public DeviceRemovedResponse(@JsonProperty("deviceId") String deviceId) {
			this.deviceId = deviceId;
		}
	}
}
