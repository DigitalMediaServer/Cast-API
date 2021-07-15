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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.chromecast.api.CastEvent.CastEventType;


/**
 * Parent class for transport object representing messages received FROM cast
 * devices.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "responseType", visible = true)
@JsonSubTypes({
	@JsonSubTypes.Type(name = "PING", value = StandardResponse.PingResponse.class),
	@JsonSubTypes.Type(name = "PONG", value = StandardResponse.PongResponse.class),
	@JsonSubTypes.Type(name = "RECEIVER_STATUS", value = StandardResponse.ReceiverStatusResponse.class),
	@JsonSubTypes.Type(name = "ERROR", value = StandardResponse.ErrorResponse.class),
	@JsonSubTypes.Type(name = "GET_APP_AVAILABILITY", value = StandardResponse.AppAvailabilityResponse.class),
	@JsonSubTypes.Type(name = "INVALID_PLAYER_STATE", value = StandardResponse.ErrorResponse.class),
	@JsonSubTypes.Type(name = "INVALID_REQUEST", value = StandardResponse.ErrorResponse.class),
	@JsonSubTypes.Type(name = "MEDIA_STATUS", value = StandardResponse.MediaStatusResponse.class),
	@JsonSubTypes.Type(name = "MULTIZONE_STATUS", value = StandardResponse.MultizoneStatusResponse.class),
	@JsonSubTypes.Type(name = "CLOSE", value = StandardResponse.CloseResponse.class),
	@JsonSubTypes.Type(name = "LOAD_CANCELLED", value = StandardResponse.ErrorResponse.class),
	@JsonSubTypes.Type(name = "LOAD_FAILED", value = StandardResponse.ErrorResponse.class),
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

	/**
	 * An error response from the cast device.
	 *
	 * @author Nadahar
	 */
	@Immutable
	public static class ErrorResponse extends StandardResponse {

		/** Application specific data */
		@Nullable
		protected final Map<String, Object> customData;

		/** The {@link DetailedErrorCode} */
		@Nullable
		protected final DetailedErrorCode detailedErrorCode;

		/** The item ID */
		@Nullable
		private final Integer itemId;

		/** The {@link ErrorReason} */
		@Nullable
		protected final ErrorReason reason;

		/** The {@link ErrorType} */
		@Nullable
		protected final ErrorType type;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param customData the application specific data.
		 * @param detailedErrorCode the {@link DetailedErrorCode}.
		 * @param itemId the item ID.
		 * @param reason the {@link ErrorReason}.
		 * @param requestId the request ID.
		 * @param type the {@link ErrorType}.
		 */
		public ErrorResponse(
			@JsonProperty("customData") @Nullable Map<String, Object> customData,
			@JsonProperty("detailedErrorCode") @Nullable DetailedErrorCode detailedErrorCode,
			@JsonProperty("itemId") @Nullable Integer itemId,
			@JsonProperty("reason") @Nullable ErrorReason reason,
			@JsonProperty("requestId") long requestId,
			@JsonProperty("responseType") @Nullable ErrorType type
		) {
			super(requestId);
			this.customData = customData;
			this.detailedErrorCode = detailedErrorCode;
			this.itemId = itemId;
			this.reason = reason;
			this.type = type;
		}

		/**
		 * @return The application specific data or {@code null}.
		 */
		@Nullable
		public Map<String, Object> getCustomData() {
			return customData;
		}

		/**
		 * @return The {@link DetailedErrorCode} or {@code null}.
		 */
		@Nullable
		public DetailedErrorCode getDetailedErrorCode() {
			return detailedErrorCode;
		}

		/**
		 * @return The item ID or {@code null}.
		 */
		@Nullable
		public Integer getItemId() {
			return itemId;
		}

		/**
		 * @return The {@link ErrorReason} or {@code null}.
		 */
		@Nullable
		public ErrorReason getReason() {
			return reason;
		}

		/**
		 * @return The {@link ErrorType} or {@code null}.
		 */
		@Nullable
		public ErrorType getType() {
			return type;
		}

		@JsonIgnore
		@Override
		public CastEventType getEventType() {
			return CastEventType.ERROR_RESPONSE;
		}

		@Override
		public int hashCode() {
			return Objects.hash(requestId, customData, detailedErrorCode, itemId, reason, type);
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
				Objects.equals(itemId, other.itemId) &&
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
			if (itemId != null) {
				builder.append("itemId=").append(itemId).append(", ");
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

		@JsonProperty
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private final String reason;

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

		@JsonProperty
		private final ReceiverStatus status;

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

		@JsonProperty
		private final List<MediaStatus> statuses;

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

		@Nonnull
		@JsonProperty
		private final Map<String, String> availability;

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

		@JsonProperty
		private final MultizoneStatus status;

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

		@JsonProperty
		private final Device device;

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

		@JsonProperty
		private final Device device;

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

		@JsonProperty
		private final String deviceId;

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

	/**
	 * Represents detailed error codes.
	 */
	public enum DetailedErrorCode {

		/**
		 * Returned when the HTMLMediaElement throws an error, but the specified
		 * error isn't recognized
		 */
		MEDIA_UNKNOWN(100),

		/**
		 * Returned when the fetching process for the media resource was aborted
		 * by the user agent at the user's request
		 */
		MEDIA_ABORTED(101),

		/**
		 * Returned when an error occurred while decoding the media resource,
		 * after the resource was established to be usable
		 */
		MEDIA_DECODE(102),

		/**
		 * Returned when a network error caused the user agent to stop fetching
		 * the media resource, after the resource was established to be usable
		 */
		MEDIA_NETWORK(103),

		/**
		 * Returned when the media resource indicated by the src attribute was
		 * not suitable
		 */
		MEDIA_SRC_NOT_SUPPORTED(104),

		/** Returned when a source buffer cannot be added to the MediaSource */
		SOURCE_BUFFER_FAILURE(110),

		/** Returned when there is an unknown error with media keys */
		MEDIAKEYS_UNKNOWN(200),

		/** Returned when there is a media keys failure due to a network issue */
		MEDIAKEYS_NETWORK(201),

		/** Returned when a MediaKeySession object cannot be created */
		MEDIAKEYS_UNSUPPORTED(202),

		/** Returned when crypto failed */
		MEDIAKEYS_WEBCRYPTO(203),

		/** Returned when there was an unknown network issue */
		NETWORK_UNKNOWN(300),

		/** Returned when a segment fails to download */
		SEGMENT_NETWORK(301),

		/** Returned when an HLS master playlist fails to download */
		HLS_NETWORK_MASTER_PLAYLIST(311),

		/** Returned when an HLS playlist fails to download */
		HLS_NETWORK_PLAYLIST(312),

		/** Returned when an HLS key fails to download */
		HLS_NETWORK_NO_KEY_RESPONSE(313),

		/** Returned when a request for an HLS key fails before it is sent */
		HLS_NETWORK_KEY_LOAD(314),

		/** Returned when an HLS segment is invalid */
		HLS_NETWORK_INVALID_SEGMENT(315),

		/** Returned when an HLS segment fails to parse */
		HLS_SEGMENT_PARSING(316),

		/**
		 * Returned when an unknown network error occurs while handling a DASH
		 * stream
		 */
		DASH_NETWORK(321),

		/** Returned when a DASH stream is missing an init */
		DASH_NO_INIT(322),

		/**
		 * Returned when an unknown network error occurs while handling a Smooth
		 * stream
		 */
		SMOOTH_NETWORK(331),

		/** Returned when a Smooth stream is missing media data */
		SMOOTH_NO_MEDIA_DATA(332),

		/** Returned when an unknown error occurs while parsing a manifest */
		MANIFEST_UNKNOWN(400),

		/**
		 * Returned when an error occurs while parsing an HLS master manifest
		 */
		HLS_MANIFEST_MASTER(411),

		/** Returned when an error occurs while parsing an HLS playlist */
		HLS_MANIFEST_PLAYLIST(412),

		/**
		 * Returned when an unknown error occurs while parsing a DASH manifest
		 */
		DASH_MANIFEST_UNKNOWN(420),

		/** Returned when a DASH manifest is missing periods */
		DASH_MANIFEST_NO_PERIODS(421),

		/** Returned when a DASH manifest is missing a MimeType */
		DASH_MANIFEST_NO_MIMETYPE(422),

		/** Returned when a DASH manifest contains invalid segment info */
		DASH_INVALID_SEGMENT_INFO(423),

		/** Returned when an error occurs while parsing a Smooth manifest */
		SMOOTH_MANIFEST(431),

		/** Returned when an unknown segment error occurs */
		SEGMENT_UNKNOWN(500),

		/** An unknown error occurred with a text stream */
		TEXT_UNKNOWN(600),

		/**
		 * Returned when an error occurs outside of the framework (e.g., if an
		 * event handler throws an error)
		 */
		APP(900),

		/** Returned when break clip load interceptor fails */
		BREAK_CLIP_LOADING_ERROR(901),

		/** Returned when break seek interceptor fails */
		BREAK_SEEK_INTERCEPTOR_ERROR(902),

		/** Returned when an image fails to load */
		IMAGE_ERROR(903),

		/** A load was interrupted by an unload, or by another load */
		LOAD_INTERRUPTED(904),

		/** A load command failed */
		LOAD_FAILED(905),

		/** An error message was sent to the sender */
		MEDIA_ERROR_MESSAGE(906),

		/** Returned when an unknown error occurs */
		GENERIC(999);

		private final int code;

		private DetailedErrorCode(int code) {
			this.code = code;
		}

		/**
		 * @return The numerical code of this {@link DetailedErrorCode}.
		 */
		public int getCode() {
			return code;
		}

		/**
		 * Returns the {@link DetailedErrorCode} that corresponds to the
		 * specified integer code, or {@code null} if nothing corresponds.
		 *
		 * @param code the integer code whose corresponding
		 *            {@link DetailedErrorCode} to find.
		 * @return The {@link DetailedErrorCode} or {@code null}.
		 */
		@JsonCreator
		@Nullable
		public static DetailedErrorCode typeOf(int code) {
			for (DetailedErrorCode errorCode : values()) {
				if (errorCode.code == code) {
					return errorCode;
				}
			}
			return null;
		}
	}
}
