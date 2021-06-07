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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
//	@JsonSubTypes.Type(name = "INVALID_REQUEST", value = StandardResponse.InvalidResponse.class),
	@JsonSubTypes.Type(name = "INVALID_REQUEST", value = StandardResponse.ErrorResponse.class),
	@JsonSubTypes.Type(name = "MEDIA_STATUS", value = StandardResponse.MediaStatusResponse.class),
	@JsonSubTypes.Type(name = "MULTIZONE_STATUS", value = StandardResponse.MultizoneStatusResponse.class),
	@JsonSubTypes.Type(name = "CLOSE", value = StandardResponse.CloseResponse.class),
//	@JsonSubTypes.Type(name = "LOAD_CANCELLED", value = StandardResponse.LoadCancelledResponse.class),
//	@JsonSubTypes.Type(name = "LOAD_FAILED", value = StandardResponse.LoadFailedResponse.class),
	@JsonSubTypes.Type(name = "LOAD_CANCELLED", value = StandardResponse.ErrorResponse.class),
	@JsonSubTypes.Type(name = "LOAD_FAILED", value = StandardResponse.ErrorResponse.class),
	@JsonSubTypes.Type(name = "LAUNCH_ERROR", value = StandardResponse.LaunchErrorResponse.class),
	@JsonSubTypes.Type(name = "DEVICE_ADDED", value = StandardResponse.DeviceAddedResponse.class),
	@JsonSubTypes.Type(name = "DEVICE_UPDATED", value = StandardResponse.DeviceUpdatedResponse.class),
	@JsonSubTypes.Type(name = "DEVICE_REMOVED", value = StandardResponse.DeviceRemovedResponse.class)
})
@Immutable
public abstract class StandardResponse implements Response {

	protected final long requestId;

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

		protected PingResponse(@JsonProperty("requestId") long requestId) {
			super(requestId);
		}

		@JsonIgnore
		@Override
		public CastEventType getEventType() {
			return CastEventType.PING;
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
			return CastEventType.PONG;
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

		@JsonProperty
		private final ErrorReason reason;

		public InvalidResponse(@JsonProperty("requestId") long requestId, @JsonProperty("reason") ErrorReason reason) {
			super(requestId);
			this.reason = reason;
		}

		public ErrorReason getReason() {
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

	@Immutable
	public static class ErrorResponse extends StandardResponse {

		@Nullable
		protected final Map<String, Object> customData;

		@Nullable
		protected final Integer detailedErrorCode;

		@Nullable
		protected final ErrorReason reason;

		@Nullable
		protected final ErrorType type; //TODO: (NAd) JavaDocs...

		public ErrorResponse(
			@JsonProperty("customData") Map<String, Object> customData,
			@JsonProperty("detailedErrorCode") Integer detailedErrorCode,
			@JsonProperty("reason") ErrorReason reason,
			@JsonProperty("requestId") long requestId,
			@JsonProperty("responseType") ErrorType type
		) {
			super(requestId);
			this.customData = customData;
			this.detailedErrorCode = detailedErrorCode;
			this.reason = reason;
			this.type = type;
		}

		@Nullable
		public Map<String, Object> getCustomData() {
			return customData;
		}

		@Nullable
		public Integer getDetailedErrorCode() {
			return detailedErrorCode;
		}

		public ErrorReason getReason() {
			return reason;
		}

		public ErrorType getType() {
			return type;
		}

		@JsonIgnore
		@Override
		public CastEventType getEventType() {
			return CastEventType.INVALID; //TODO: (Nad) Fix
		}

		@Override
		public int hashCode() {
			return Objects.hash(requestId, customData, detailedErrorCode, reason, type);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof ErrorResponse)) {
				return false;
			}
			ErrorResponse other = (ErrorResponse) obj;
			return
				Objects.equals(requestId, other.requestId) &&
				Objects.equals(customData, other.customData) &&
				Objects.equals(detailedErrorCode, other.detailedErrorCode) &&
				reason == other.reason &&
				type == other.type;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ErrorResponse [");
			if (customData != null) {
				builder.append("customData=").append(customData).append(", ");
			}
			if (detailedErrorCode != null) {
				builder.append("detailedErrorCode=").append(detailedErrorCode).append(", ");
			}
			if (reason != null) {
				builder.append("reason=").append(reason).append(", ");
			}
			if (type != null) {
				builder.append("type=").append(type).append(", ");
			}
			builder.append("requestId=").append(requestId).append("]");
			return builder.toString();
		}
	}

	/**
	 * Application cannot be launched for some <code>reason</code>.
	 */
	@Immutable
	public static class LaunchErrorResponse extends StandardResponse {

		private final String reason;

		public LaunchErrorResponse(@JsonProperty("requestId") long requestId, @JsonProperty("reason") String reason) {
			super(requestId);
			this.reason = reason;
		}

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

		private final ReceiverStatus status;

		public ReceiverStatusResponse(@JsonProperty("requestId") long requestId, @JsonProperty("status") ReceiverStatus status) {
			super(requestId);
			this.status = status;
		}

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

		private final List<MediaStatus> statuses;

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

		@Nonnull
		private final Map<String, String> availability;

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
	 * Multi-Zone status for the case when there are multiple ChromeCast devices
	 * in different zones (rooms).
	 */
	@Immutable
	public static class MultizoneStatusResponse extends StandardResponse {

		private final MultizoneStatus status;

		public MultizoneStatusResponse(
			@JsonProperty("requestId") long requestId,
			@JsonProperty("status") MultizoneStatus status
		) {
			super(requestId);
			this.status = status;
		}

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
	 * Received when power is cycled on ChromeCast Audio device in a group.
	 */
	@Immutable
	public static class DeviceAddedResponse extends StandardResponse {

		private final Device device;

		public DeviceAddedResponse(@JsonProperty("requestId") long requestId, @JsonProperty("device") Device device) {
			super(requestId);
			this.device = device;
		}

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

		private final Device device;

		public DeviceUpdatedResponse(@JsonProperty("requestId") long requestId, @JsonProperty("device") Device device) {
			super(requestId);
			this.device = device;
		}

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
	 * Received when power is cycled on ChromeCast Audio device in a group.
	 */
	@Immutable
	public static class DeviceRemovedResponse extends StandardResponse {

		private final String deviceId;

		public DeviceRemovedResponse(
			@JsonProperty("requestId") long requestId,
			@JsonProperty("deviceId") String deviceId
		) {
			super(requestId);
			this.deviceId = deviceId;
		}

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

	/**
	 * Represents media error message reasons.
	 */
	public enum ErrorReason {

		/** Returned when the command is not valid or not implemented */
		INVALID_COMMAND,

		/**
		 * Returned when the params are not valid or a non optional param is
		 * missing
		 */
		INVALID_PARAMS,

		/** Returned when the media session does not exist */
		INVALID_MEDIA_SESSION_ID,

		/** Returned when cannot skip more items due to reaching skip limit */
		SKIP_LIMIT_REACHED,

		/** Returned when the request is not supported by the application */
		NOT_SUPPORTED,

		/** Returned when the requested language is not supported */
		LANGUAGE_NOT_SUPPORTED,

		/**
		 * Returned when skip is not possible due to going back beyond the first
		 * item or forward beyond the last item in the queue
		 */
		END_OF_QUEUE,

		/**
		 * Returned when the request ID is not unique (the receiver is
		 * processing a request with the same ID)
		 */
		DUPLICATE_REQUEST_ID,

		/**
		 * Returned when the request cannot be completed because a video-capable
		 * device is required
		 */
		VIDEO_DEVICE_REQUIRED,

		/**
		 * Returned when premium account is required for the request to succeed
		 */
		PREMIUM_ACCOUNT_REQUIRED,

		/** Returned when the application state is invalid to fulfill the request */
		APP_ERROR,

		/**
		 * Returned when a request cannot be performed because authentication
		 * has expired, e.g. user changed password or the token was revoked
		 */
		AUTHENTICATION_EXPIRED,

		/** Returned when too many concurrent streams are detected */
		CONCURRENT_STREAM_LIMIT,

		/** Returned when the content is blocked due to parental controls */
		PARENTAL_CONTROL_RESTRICTED,

		/** Returned when the content is blocked due to filter */
		CONTENT_FILTERED,

		/**
		 * Returned when the content is blocked due to being regionally
		 * unavailable
		 */
		NOT_AVAILABLE_IN_REGION,

		/** Returned when the requested content is already playing */
		CONTENT_ALREADY_PLAYING,

		/** Returned when the request is not valid */
		INVALID_REQUEST,

		/** Returned when the load request encounter intermittent issue */
		GENERIC_LOAD_ERROR;

		/**
		 * Parses the specified string and returns the corresponding
		 * {@link ErrorReason}, or {@code null} if no match could be found.
		 *
		 * @param errorReason the string to parse.
		 * @return The resulting {@link ErrorReason} or {@code null}.
		 */
		@Nullable
		@JsonCreator
		public static ErrorReason typeOf(String errorReason) {
			if (Util.isBlank(errorReason)) {
				return null;
			}
			String typeString = errorReason.toUpperCase(Locale.ROOT);
			for (ErrorReason type : values()) {
				if (typeString.equals(type.name())) {
					return type;
				}
			}
			return null;
		}
	}

	/**
	 * Represents media error message types.
	 */
	public enum ErrorType {

		/** Returned when the player state is invalid to fulfill the request */
		INVALID_PLAYER_STATE,

		/** Returned when the LOAD request failed */
		LOAD_FAILED,

		/**
		 * Returned when the LOAD request is cancelled by a second incoming LOAD
		 * request
		 */
		LOAD_CANCELLED,

		/** Returned when the request is not valid */
		INVALID_REQUEST,

		/** Generic error, for any other error case */
		ERROR;

		/**
		 * Parses the specified string and returns the corresponding
		 * {@link ErrorType}, or {@code null} if no match could be found.
		 *
		 * @param errorType the string to parse.
		 * @return The resulting {@link ErrorType} or {@code null}.
		 */
		@Nullable
		@JsonCreator
		public static ErrorType typeOf(String errorType) {
			if (Util.isBlank(errorType)) {
				return null;
			}
			String typeString = errorType.toUpperCase(Locale.ROOT);
			for (ErrorType type : values()) {
				if (typeString.equals(type.name())) {
					return type;
				}
			}
			return null;
		}
	}
}
