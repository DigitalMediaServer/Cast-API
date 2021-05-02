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
	@JsonSubTypes.Type(name = "PING", value = StandardResponse.Ping.class),
	@JsonSubTypes.Type(name = "PONG", value = StandardResponse.Pong.class),
	@JsonSubTypes.Type(name = "RECEIVER_STATUS", value = StandardResponse.Status.class),
	@JsonSubTypes.Type(name = "GET_APP_AVAILABILITY", value = StandardResponse.AppAvailability.class),
	@JsonSubTypes.Type(name = "INVALID_REQUEST", value = StandardResponse.Invalid.class),
	@JsonSubTypes.Type(name = "MEDIA_STATUS", value = StandardResponse.MediaStatus.class),
	@JsonSubTypes.Type(name = "MULTIZONE_STATUS", value = StandardResponse.MultizoneStatus.class),
	@JsonSubTypes.Type(name = "CLOSE", value = StandardResponse.Close.class),
	@JsonSubTypes.Type(name = "LOAD_FAILED", value = StandardResponse.LoadFailed.class),
	@JsonSubTypes.Type(name = "LAUNCH_ERROR", value = StandardResponse.LaunchError.class),
	@JsonSubTypes.Type(name = "DEVICE_ADDED", value = StandardResponse.DeviceAdded.class),
	@JsonSubTypes.Type(name = "DEVICE_UPDATED", value = StandardResponse.DeviceUpdated.class),
	@JsonSubTypes.Type(name = "DEVICE_REMOVED", value = StandardResponse.DeviceRemoved.class)
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
	public static class Ping extends StandardResponse {
	}

	/**
	 * Response in reply to 'Ping' message.
	 */
	public static class Pong extends StandardResponse {
	}

	/**
	 * Request to 'Close' connection.
	 */
	public static class Close extends StandardResponse {
	}

	/**
	 * Identifies that loading of media has failed.
	 */
	public static class LoadFailed extends StandardResponse {
	}

	/**
	 * Request was invalid for some <code>reason</code>.
	 */
	public static class Invalid extends StandardResponse {

		protected final String reason;

		public Invalid(@JsonProperty("reason") String reason) {
			this.reason = reason;
		}
	}

	/**
	 * Application cannot be launched for some <code>reason</code>.
	 */
	public static class LaunchError extends StandardResponse {

		protected final String reason;

		public LaunchError(@JsonProperty("reason") String reason) {
			this.reason = reason;
		}
	}

	/**
	 * Response to "Status" request.
	 */
	public static class Status extends StandardResponse {

		@JsonProperty
		protected final org.digitalmediaserver.cast.Status status;

		public Status(@JsonProperty("status") org.digitalmediaserver.cast.Status status) {
			this.status = status;
		}
	}

	/**
	 * Response to "MediaStatus" request.
	 */
	public static class MediaStatus extends StandardResponse {

		protected final org.digitalmediaserver.cast.MediaStatus[] statuses;

		public MediaStatus(@JsonProperty("status") org.digitalmediaserver.cast.MediaStatus... statuses) {
			this.statuses = statuses;
		}
	}

	/**
	 * Response to "AppAvailability" request.
	 */
	public static class AppAvailability extends StandardResponse {

		@JsonProperty
		protected Map<String, String> availability;
	}

	/**
	 * Multi-Zone status for the case when there are multiple ChromeCast devices
	 * in different zones (rooms).
	 */
	public static class MultizoneStatus extends StandardResponse {

		@JsonProperty
		protected final org.digitalmediaserver.cast.MultizoneStatus status;

		public MultizoneStatus(@JsonProperty("status") org.digitalmediaserver.cast.MultizoneStatus status) {
			this.status = status;
		}
	}

	/**
	 * Received when power is cycled on ChromeCast Audio device in a group.
	 */
	public static class DeviceAdded extends StandardResponse {

		protected final Device device;

		public DeviceAdded(@JsonProperty("device") Device device) {
			this.device = device;
		}
	}

	/**
	 * Received when volume is changed in ChromeCast Audio group.
	 */
	public static class DeviceUpdated extends StandardResponse {

		protected final Device device;

		public DeviceUpdated(@JsonProperty("device") Device device) {
			this.device = device;
		}
	}

	/**
	 * Received when power is cycled on ChromeCast Audio device in a group.
	 */
	public static class DeviceRemoved extends StandardResponse {

		protected final String deviceId;

		public DeviceRemoved(@JsonProperty("deviceId") String deviceId) {
			this.deviceId = deviceId;
		}
	}
}
