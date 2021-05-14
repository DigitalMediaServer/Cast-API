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
package org.digitalmediaserver.chromecast.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.chromecast.api.CastEvent.CastEventType;

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
	@JsonSubTypes.Type(name = "LOAD_CANCELLED", value = StandardResponse.LoadCancelledResponse.class),
	@JsonSubTypes.Type(name = "LOAD_FAILED", value = StandardResponse.LoadFailedResponse.class),
	@JsonSubTypes.Type(name = "LAUNCH_ERROR", value = StandardResponse.LaunchErrorResponse.class),
	@JsonSubTypes.Type(name = "DEVICE_ADDED", value = StandardResponse.DeviceAddedResponse.class),
	@JsonSubTypes.Type(name = "DEVICE_UPDATED", value = StandardResponse.DeviceUpdatedResponse.class),
	@JsonSubTypes.Type(name = "DEVICE_REMOVED", value = StandardResponse.DeviceRemovedResponse.class)
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

	/**
	 * Request to send 'Pong' message in reply.
	 */
	@Immutable
	public static class PingResponse extends StandardResponse {

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

	/**
	 * Response in reply to 'Ping' message.
	 */
	@Immutable
	public static class PongResponse extends StandardResponse {

		/**
		 * Creates a new instance.
		 */
		protected PongResponse() {
			super(0);
		}

		@Override
		@JsonIgnore
		public long getRequestId() {
			return super.getRequestId();
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

	/**
	 * Request to 'Close' connection.
	 */
	@Immutable
	public static class CloseResponse extends StandardResponse {

		/**
		 * Creates a new instance using the specified request ID.
		 *
		 * @param requestId the request ID.
		 */
		protected CloseResponse(@JsonProperty("requestId") long requestId) {
			super(requestId);
		}

		@JsonIgnore
		@Override
		public CastEventType getEventType() {
			return CastEventType.CLOSE;
		}

		@Override
		public String toString() {
			return new StringBuilder(getClass().getSimpleName())
				.append(" [requestId=").append(getRequestId()).append("]")
				.toString();
		}
	}

	@Immutable
	public static class LoadCancelledResponse extends StandardResponse {

		private final Integer itemId;

		protected LoadCancelledResponse(@JsonProperty("requestId") long requestId, @JsonProperty("itemId") Integer itemId) {
			super(requestId);
			this.itemId = itemId;
		}

		public Integer getItemId() {
			return itemId;
		}

		@JsonIgnore
		@Override
		public CastEventType getEventType() {
			return CastEventType.LOAD_CANCELLED;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder(getClass().getSimpleName());
			builder.append(" [").append("requestId=").append(getRequestId());
			if (itemId != null) {
				builder.append(", itemId=").append(itemId);
			}
			builder.append("]");
			return builder.toString();
		}
	}

	/**
	 * Identifies that loading of media has failed.
	 */
	@Immutable
	public static class LoadFailedResponse extends StandardResponse {

		protected LoadFailedResponse(@JsonProperty("requestId") long requestId) {
			super(requestId);
		}

		@JsonIgnore
		@Override
		public CastEventType getEventType() {
			return CastEventType.LOAD_FAILED;
		}

		@Override
		public String toString() {
			return new StringBuilder(getClass().getSimpleName())
				.append(" [requestId=").append(getRequestId()).append("]")
				.toString();
		}
	}

	/**
	 * Request was invalid for some <code>reason</code>.
	 */
	@Immutable
	public static class InvalidResponse extends StandardResponse {

		protected final String reason;

		public InvalidResponse(@JsonProperty("requestId") long requestId, @JsonProperty("reason") String reason) {
			super(requestId);
			this.reason = reason;
		}

		public String getReason() {
			return reason;
		}

		@JsonIgnore
		@Override
		public CastEventType getEventType() {
			return CastEventType.INVALID;
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

	/**
	 * Application cannot be launched for some <code>reason</code>.
	 */
	@Immutable
	public static class LaunchErrorResponse extends StandardResponse {

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

	/**
	 * Response to "ReceiverStatus" request.
	 */
	@Immutable
	public static class ReceiverStatusResponse extends StandardResponse {

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

	/**
	 * Response to "MediaStatus" request.
	 */
	@Immutable
	@JsonDeserialize(using = MediaStatusResponseDeserializer.class)
	public static class MediaStatusResponse extends StandardResponse {

		/** The {@link List} of {@link MediaStatus}es */
		@JsonProperty
		protected final List<MediaStatus> statuses;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param requestId the request ID.
		 * @param statuses the {@link MediaStatus}es.
		 */
		public MediaStatusResponse(
			@JsonProperty("requestId") long requestId,
			@JsonProperty("status") MediaStatus... statuses
		) {
			super(requestId);
			if (statuses == null || statuses.length == 0) {
				this.statuses = Collections.emptyList();
			} else {
				this.statuses = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(statuses)));
			}
		}

		/**
		 * @return The {@link List} of {@link MediaStatus}es.
		 */
		@Nonnull
		@JsonProperty("status")
		public List<MediaStatus> getStatuses() {
			return statuses;
		}

		@JsonIgnore
		@Override
		public CastEventType getEventType() {
			return CastEventType.MEDIA_STATUS;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder(getClass().getSimpleName());
			builder.append(" [").append("requestId=").append(getRequestId());
			if (statuses != null) {
				builder.append(", statuses=").append(statuses);
			}
			builder.append("]");
			return builder.toString();
		}
	}

	/**
	 * Response to "AppAvailability" request.
	 */
	@Immutable
	public static class AppAvailabilityResponse extends StandardResponse {

		/** The {@link Map} of application ID and availability information */
		@Nonnull
		@JsonProperty
		protected final Map<String, String> availability;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param requestId the request ID.
		 * @param availability the {@link Map} containing the availability information.
		 */
		public AppAvailabilityResponse(
			@JsonProperty("requestId") long requestId,
			@JsonProperty("availability") Map<String, String> availability
		) {
			super(requestId);
			if (availability == null || availability.isEmpty()) {
				this.availability = Collections.emptyMap();
			} else {
				this.availability = Collections.unmodifiableMap(new LinkedHashMap<>(availability));
			}
		}

		/**
		 * @return The {@link Map} containing the availability information.
		 */
		@Nonnull
		public Map<String, String> getAvailability() {
			return availability;
		}

		@JsonIgnore
		@Override
		public CastEventType getEventType() {
			return CastEventType.APPLICATION_AVAILABILITY;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder(getClass().getSimpleName());
			builder.append(" [").append("requestId=").append(getRequestId());
			if (availability != null) {
				builder.append(", availability=").append(availability);
			}
			builder.append("]");
			return builder.toString();
		}
	}

	/**
	 * Multi-Zone status for the case when there are multiple cast devices in
	 * different zones (rooms).
	 */
	@Immutable
	public static class MultizoneStatusResponse extends StandardResponse {

		/** The {@link MultizoneStatus} */
		@JsonProperty
		protected final MultizoneStatus status;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param requestId the request ID.
		 * @param status the {@link MultizoneStatus} status.
		 */
		public MultizoneStatusResponse(
			@JsonProperty("requestId") long requestId,
			@JsonProperty("status") MultizoneStatus status
		) {
			super(requestId);
			this.status = status;
		}

		/**
		 * @return The {@link MultizoneStatus}.
		 */
		public MultizoneStatus getStatus() {
			return status;
		}

		@JsonIgnore
		@Override
		public CastEventType getEventType() {
			return CastEventType.MULTIZONE_STATUS;
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

	/**
	 * Received when power is cycled on ChromeCast Audio devices in a group.
	 */
	@Immutable
	public static class DeviceAddedResponse extends StandardResponse {

		/** The {@link Device} */
		@JsonProperty
		protected final Device device;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param requestId the request ID.
		 * @param device the {@link Device}.
		 */
		public DeviceAddedResponse(@JsonProperty("requestId") long requestId, @JsonProperty("device") Device device) {
			super(requestId);
			this.device = device;
		}

		/**
		 * @return The {@link Device}.
		 */
		public Device getDevice() {
			return device;
		}

		@JsonIgnore
		@Override
		public CastEventType getEventType() {
			return CastEventType.DEVICE_ADDED;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder(getClass().getSimpleName());
			builder.append(" [").append("requestId=").append(getRequestId());
			if (device != null) {
				builder.append(", device=").append(device);
			}
			builder.append("]");
			return builder.toString();
		}
	}

	/**
	 * Received when volume is changed.
	 */
	@Immutable
	public static class DeviceUpdatedResponse extends StandardResponse {

		/** The {@link Device} */
		@JsonProperty
		protected final Device device;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param requestId the request ID.
		 * @param device the {@link Device}.
		 */
		public DeviceUpdatedResponse(
			@JsonProperty("requestId") long requestId,
			@JsonProperty("device") Device device
		) {
			super(requestId);
			this.device = device;
		}

		/**
		 * @return The {@link Device}.
		 */
		public Device getDevice() {
			return device;
		}

		@JsonIgnore
		@Override
		public CastEventType getEventType() {
			return CastEventType.DEVICE_UPDATED;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder(getClass().getSimpleName());
			builder.append(" [").append("requestId=").append(getRequestId());
			if (device != null) {
				builder.append(", device=").append(device);
			}
			builder.append("]");
			return builder.toString();
		}
	}

	/**
	 * Received when power is cycled on ChromeCast Audio devices in a group.
	 */
	@Immutable
	public static class DeviceRemovedResponse extends StandardResponse {

		/** The {@link Device} */
		@JsonProperty
		protected final String deviceId;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param requestId the request ID.
		 * @param deviceId the device ID.
		 */
		public DeviceRemovedResponse(
			@JsonProperty("requestId") long requestId,
			@JsonProperty("deviceId") String deviceId
		) {
			super(requestId);
			this.deviceId = deviceId;
		}

		/**
		 * @return The device ID.
		 */
		public String getDeviceId() {
			return deviceId;
		}

		@JsonIgnore
		@Override
		public CastEventType getEventType() {
			return CastEventType.DEVICE_REMOVED;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder(getClass().getSimpleName());
			builder.append(" [").append("requestId=").append(getRequestId());
			if (deviceId != null) {
				builder.append(", deviceId=").append(deviceId);
			}
			builder.append("]");
			return builder.toString();
		}
	}
}
