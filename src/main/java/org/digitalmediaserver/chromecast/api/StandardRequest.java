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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Parent class for transport object representing messages sent TO ChromeCast
 * device.
 */
public abstract class StandardRequest extends StandardMessage implements Request { //TODO: (Nad) Immutable..

	/** The request ID */
	protected long requestId;

	@Override
	public void setRequestId(long requestId) {
		this.requestId = requestId;
	}

	@Override
	public long getRequestId() {
		return requestId;
	}

	/**
	 * Request for current status of ChromeCast device.
	 */
	public static class GetStatus extends StandardRequest {
	}

	/**
	 * Request for availability of applications with specified identifiers.
	 */
	public static class GetAppAvailability extends StandardRequest {

		@JsonProperty
		private final String[] appId;

		public GetAppAvailability(String... appId) {
			this.appId = appId;
		}

		public String[] getAppId() {
			return appId;
		}
	}

	/**
	 * Request to launch application with specified identifiers.
	 */
	public static class Launch extends StandardRequest {

		@JsonProperty
		private final String appId;

		public Launch(@JsonProperty("appId") String appId) {
			this.appId = appId;
		}

		public String getAppId() {
			return appId;
		}
	}

	/**
	 * Request to stop the currently running application.
	 */
	public static class Stop extends StandardRequest {

		@JsonProperty
		private final String sessionId;

		public Stop(String sessionId) {
			this.sessionId = sessionId;
		}

		public String getSessionId() {
			return sessionId;
		}
	}

	/**
	 * A request to load media.
	 */
	public static class Load extends StandardRequest {

		@JsonProperty
		private final String sessionId;
		@JsonProperty
		private final Media media;
		@JsonProperty
		private final boolean autoplay;
		@JsonProperty
		private final double currentTime;
		@JsonProperty
		private final Object customData;

		/**
		 * Creates a new request to load the specified {@link Media}.
		 *
		 * @param sessionId the session ID to use.
		 * @param media the {@link Media} to load.
		 * @param autoplay {@code true} to ask the remote application to start
		 *            playback as soon as the {@link Media} has been loaded,
		 *            {@code false} to ask it to transition to a paused state
		 *            after loading.
		 * @param currentTime the position in seconds where playback are to be
		 *            started in the loaded {@link Media}.
		 * @param customData the custom application data to send to the remote
		 *            application with the load command.
		 */
		public Load(
			String sessionId,
			Media media,
			boolean autoplay,
			double currentTime,
			final Map<String, String> customData
		) {
			this.sessionId = sessionId;
			this.media = media;
			this.autoplay = autoplay;
			this.currentTime = currentTime;

			this.customData = customData == null ? null : new Object() {

				@JsonProperty
				Map<String, String> payload = customData;
			};
		}

		public String getSessionId() {
			return sessionId;
		}

		public Media getMedia() {
			return media;
		}

		public boolean isAutoplay() {
			return autoplay;
		}

		public double getCurrentTime() {
			return currentTime;
		}

		public Object getCustomData() {
			return customData;
		}
	}

	/**
	 * Abstract request for an action with the current media.
	 */
	public abstract static class MediaRequest extends StandardRequest {

		/** The media session ID */
		@JsonProperty
		private final long mediaSessionId;

		/** The session ID */
		@JsonProperty
		private final String sessionId;

		/**
		 * Abstract constructor.
		 *
		 * @param mediaSessionId the media session ID.
		 * @param sessionId the session ID.
		 */
		public MediaRequest(long mediaSessionId, String sessionId) {
			this.mediaSessionId = mediaSessionId;
			this.sessionId = sessionId;
		}

		/**
		 * @return The media session ID.
		 */
		public long getMediaSessionId() {
			return mediaSessionId;
		}

		/**
		 * @return The session ID.
		 */
		public String getSessionId() {
			return sessionId;
		}
	}

	/**
	 * A request to start/resume playback.
	 */
	public static class Play extends MediaRequest {

		/**
		 * Creates a new request to start playing the media referenced by the
		 * specified media session ID.
		 *
		 * @param mediaSessionId the media session ID for which the play request
		 *            applies.
		 * @param sessionId the session ID to use.
		 */
		public Play(long mediaSessionId, String sessionId) {
			super(mediaSessionId, sessionId);
		}
	}

	/**
	 * A request to pause playback.
	 */
	public static class Pause extends MediaRequest {

		/**
		 * Creates a new request to pause playback of the media referenced by
		 * the specified media session ID.
		 *
		 * @param mediaSessionId the media session ID for which the pause
		 *            request applies.
		 * @param sessionId the session ID to use.
		 */
		public Pause(long mediaSessionId, String sessionId) {
			super(mediaSessionId, sessionId);
		}
	}

	/**
	 * Request to change current playback position.
	 */
	public static class Seek extends MediaRequest {

		/** The new playback position in seconds */
		@JsonProperty
		protected final double currentTime;

		/** Custom data for the receiver application */
		@Nullable
		@JsonProperty
		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		protected final Map<String, Object> customData;

		/**
		 * The desired media player state after the seek is complete. If
		 * {@code null}, it will retain the state it had before seeking
		 */
		@Nullable
		@JsonProperty
		@JsonInclude(JsonInclude.Include.NON_NULL)
		protected final ResumeState resumeState;

		/**
		 * Creates a new request to move the playback position of the media
		 * referenced by the specified media session ID to the specified
		 * position.
		 *
		 * @param mediaSessionId the media session ID for which the seek request
		 *            applies.
		 * @param sessionId the session ID to use.
		 * @param currentTime the new playback position in seconds.
		 * @param resumeState the desired media player state after the seek is
		 *            complete. If {@code null}, it will retain the state it had
		 *            before seeking.
		 */
		public Seek(
			long mediaSessionId,
			@Nonnull String sessionId,
			double currentTime,
			@Nullable ResumeState resumeState
		) {
			super(mediaSessionId, sessionId);
			this.currentTime = currentTime;
			this.customData = null;
			this.resumeState = resumeState;
		}

		/**
		 * Creates a new request to move the playback position of the media
		 * referenced by the specified media session ID to the specified
		 * position.
		 *
		 * @param mediaSessionId the media session ID for which the seek request
		 *            applies.
		 * @param sessionId the session ID to use.
		 * @param currentTime the new playback position in seconds.
		 * @param customData the custom data for the receiver application.
		 * @param resumeState the desired media player state after the seek is
		 *            complete. If {@code null}, it will retain the state it had
		 *            before seeking.
		 */
		public Seek(
			long mediaSessionId,
			@Nonnull String sessionId,
			double currentTime,
			@Nullable Map<String, Object> customData,
			@Nullable ResumeState resumeState
		) {
			super(mediaSessionId, sessionId);
			this.currentTime = currentTime;
			this.customData = customData;
			this.resumeState = resumeState;
		}

		/**
		 * @return The new playback position in seconds.
		 */
		public double getCurrentTime() {
			return currentTime;
		}

		/**
		 * @return The custom data for the receiver application.
		 */
		@Nullable
		public Map<String, Object> getCustomData() {
			return customData;
		}

		/**
		 * @return The desired media player state after the seek is complete. If
		 *         {@code null}, it will retain the state it had before seeking.
		 */
		@Nullable
		public ResumeState getResumeState() {
			return resumeState;
		}
	}

	/**
	 * A request to set volume level or mute at the receiver.
	 */
	public static class SetVolume extends StandardRequest {

		@JsonProperty
		private final Volume volume;

		/**
		 * Creates a new request using the specified parameters.
		 *
		 * @param volume the new volume of the cast device. At least one of
		 *            level or muted must be set.
		 */
		public SetVolume(Volume volume) {
			this.volume = volume;
		}

		/**
		 * @return The new volume of the cast device. At least one of level or
		 *         muted must be set.
		 */
		public Volume getVolume() {
			return volume;
		}
	}

	/**
	 * A request to set the stream volume of the playing media.
	 * <p>
	 * <b>Note</b> This should be a {@link MediaRequest}, but since that would
	 * also make it a {@link StandardMessage} which maps {@code type} using
	 * Jackson subtypes, it isn't. The reason is that another implementation,
	 * {@link SetVolume}, is already mapped to "{@code SET_VOLUME}" which is the
	 * same {@code type} as this request uses. The differences between the two
	 * is the namespace, but that isn't captured by the Jackson subtype logic,
	 * which is why this implementation is only a {@link Request} that "manually
	 * implements" the remaining fields required for a {@link MediaRequest}.
	 */
	public static class VolumeRequest implements Request {

		/** The media session ID */
		@JsonProperty
		private final long mediaSessionId;

		/** The session ID */
		@JsonProperty
		private final String sessionId;

		/** the request ID */
		@JsonProperty
		private long requestId;

		@JsonProperty
		private final String type = "SET_VOLUME";

		/**
		 * The new volume of the stream. At least one of level or muted must be
		 * set.
		 */
		@Nonnull
		@JsonProperty
		protected final MediaVolume volume;

		/** Custom data for the receiver application */
		@Nullable
		@JsonProperty
		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		protected final Map<String, Object> customData;

		/**
		 * Creates a new request using the specified parameters.
		 *
		 * @param sessionId the session ID to use.
		 * @param mediaSessionId the media session ID for which the seek request
		 *            applies.
		 * @param volume the new volume of the stream. At least one of level or
		 *            muted must be set.
		 * @param customData the custom data for the receiver application.
		 * @throws IllegalArgumentException If {@code sessionId} or
		 *             {@code volume} is {@code null}.
		 */
		public VolumeRequest(
			String sessionId,
			long mediaSessionId,
			@Nonnull MediaVolume volume,
			@Nullable Map<String, Object> customData
		) {
			Util.requireNotBlank(sessionId, "sessionId");
			Util.requireNotNull(volume, "volume");
			this.sessionId = sessionId;
			this.mediaSessionId = mediaSessionId;
			this.volume = volume;
			this.customData = customData;
		}

		/**
		 * @return The new volume of the stream. At least one of level or muted
		 *         must be set.
		 */
		@Nonnull
		public MediaVolume getVolume() {
			return volume;
		}

		/**
		 * @return The custom data for the receiver application.
		 */
		@Nullable
		public Map<String, Object> getCustomData() {
			return customData;
		}

		@Override
		public void setRequestId(long requestId) {
			this.requestId = requestId;
		}

		@Override
		public long getRequestId() {
			return requestId;
		}

		/**
		 * @return The media session ID.
		 */
		public long getMediaSessionId() {
			return mediaSessionId;
		}

		/**
		 * @return the session ID.
		 */
		public String getSessionId() {
			return sessionId;
		}
	}

	@Nonnull
	public static GetStatus status() {
		return new GetStatus();
	}

	@Nonnull
	public static GetAppAvailability getAppAvailability(String... applicationId) {
		return new GetAppAvailability(applicationId);
	}

	@Nonnull
	public static Launch launch(String applicationId) {
		return new Launch(applicationId);
	}

	@Nonnull
	public static Stop stop(String sessionId) {
		return new Stop(sessionId);
	}

	/**
	 * Creates a new request to load the specified {@link Media}.
	 *
	 * @param sessionId the session ID to use.
	 * @param media the {@link Media} to load.
	 * @param autoplay {@code true} to ask the remote application to start
	 *            playback as soon as the {@link Media} has been loaded,
	 *            {@code false} to ask it to transition to a paused state after
	 *            loading.
	 * @param currentTime the position in seconds where playback are to be
	 *            started in the loaded {@link Media}.
	 * @param customData the custom application data to send to the remote
	 *            application with the load command.
	 * @return the new {@link Load} request.
	 */
	@Nonnull
	public static Load load(
		String sessionId,
		Media media,
		boolean autoplay,
		double currentTime,
		Map<String, String> customData
	) {
		return new Load(sessionId, media, autoplay, currentTime, customData);
	}

	/**
	 * Creates a new request to start playing the media referenced by the
	 * specified media session ID.
	 *
	 * @param sessionId the session ID to use.
	 * @param mediaSessionId the media session ID for which the play request
	 *            applies.
	 * @return The new {@link Play} request.
	 */
	@Nonnull
	public static Play play(String sessionId, long mediaSessionId) {
		return new Play(mediaSessionId, sessionId);
	}

	/**
	 * Creates a new request to pause playback of the media referenced by the
	 * specified media session ID.
	 *
	 * @param sessionId the session ID to use.
	 * @param mediaSessionId the media session ID for which the pause request
	 *            applies.
	 * @return The new {@link Pause} request.
	 */
	@Nonnull
	public static Pause pause(String sessionId, long mediaSessionId) {
		return new Pause(mediaSessionId, sessionId);
	}

	/**
	 * Creates a new request to move the playback position of the media
	 * referenced by the specified media session ID to the specified position.
	 *
	 * @param sessionId the session ID to use.
	 * @param mediaSessionId the media session ID for which the seek request
	 *            applies.
	 * @param currentTime the new playback position in seconds.
	 * @param resumeState the desired media player state after the seek is
	 *            complete. If {@code null}, it will retain the state it had
	 *            before seeking.
	 * @return The new {@link Seek} request.
	 */
	@Nonnull
	public static Seek seek(
		@Nonnull String sessionId,
		long mediaSessionId,
		double currentTime,
		@Nullable ResumeState resumeState
	) {
		return new Seek(mediaSessionId, sessionId, currentTime, resumeState);
	}

	/**
	 * Creates a new request using the specified parameters.
	 *
	 * @param volume the new volume of the cast device. At least one of level or
	 *            muted must be set.
	 * @return The new {@link SetVolume}.
	 */
	@Nonnull
	public static SetVolume setVolume(Volume volume) {
		return new SetVolume(volume);
	}

	/**
	 * Creates a new {@link VolumeRequest} for setting volume level and mute on
	 * a <i>media stream</i> using the specified parameters.
	 *
	 * @param sessionId the session ID to use.
	 * @param mediaSessionId the media session ID for which the seek request
	 *            applies.
	 * @param volume the new volume of the stream. At least one of level or
	 *            muted must be set.
	 * @param customData the custom data for the receiver application.
	 * @return The new {@link VolumeRequest}.
	 * @throws IllegalArgumentException If {@code sessionId} or {@code volume}
	 *             is {@code null}.
	 */
	@Nonnull
	public static VolumeRequest volumeRequest(
		@Nonnull String sessionId,
		long mediaSessionId,
		@Nonnull MediaVolume volume,
		@Nullable Map<String, Object> customData
	) {
		return new VolumeRequest(sessionId, mediaSessionId, volume, customData);
	}

	/**
	 * States of the media player after resuming.
	 */
	public enum ResumeState {

		/** Force media to start */
		PLAYBACK_START,

		/** Force media to pause */
		PLAYBACK_PAUSE
	}
}
