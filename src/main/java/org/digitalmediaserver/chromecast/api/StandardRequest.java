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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Parent class for transport object representing messages sent TO ChromeCast
 * device.
 */
public abstract class StandardRequest extends StandardMessage implements Request { //TODO: (Nad) Immutable..

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
	 * Request to stop currently running application.
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
	 * Abstract request for an action with currently played media.
	 */
	public abstract static class MediaRequest extends StandardRequest {

		@JsonProperty
		private final long mediaSessionId;
		@JsonProperty
		private final String sessionId;

		public MediaRequest(long mediaSessionId, String sessionId) {
			this.mediaSessionId = mediaSessionId;
			this.sessionId = sessionId;
		}

		public long getMediaSessionId() {
			return mediaSessionId;
		}

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
		 * Creates a new request to pause playback of the media referenced by the
		 * specified media session ID.
		 *
		 * @param mediaSessionId the media session ID for which the pause request
		 *            applies.
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

		@JsonProperty
		private final double currentTime;

		/**
		 * Creates a new request to move the playback position of the media
		 * referenced by the specified media session ID to the specified
		 * position.
		 *
		 * @param mediaSessionId the media session ID for which the seek request
		 *            applies.
		 * @param sessionId the session ID to use.
		 * @param currentTime the new playback position in seconds.
		 */
		public Seek(long mediaSessionId, String sessionId, double currentTime) {
			super(mediaSessionId, sessionId);
			this.currentTime = currentTime;
		}

		public double getCurrentTime() {
			return currentTime;
		}
	}

	/**
	 * Request to change volume.
	 */
	public static class SetVolume extends StandardRequest {

		@JsonProperty
		private final Volume volume;

		public SetVolume(Volume volume) {
			this.volume = volume;
		}

		public Volume getVolume() {
			return volume;
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
	 *            {@code false} to ask it to transition to a paused state
	 *            after loading.
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
	 * specified media session ID
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
	 * specified media session ID
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
	 * @return The new {@link Seek} request.
	 */
	@Nonnull
	public static Seek seek(String sessionId, long mediaSessionId, double currentTime) {
		return new Seek(mediaSessionId, sessionId, currentTime);
	}

	@Nonnull
	public static SetVolume setVolume(Volume volume) { //TODO: (Nad) Look into - setting the whole Volume..?
		return new SetVolume(volume);
	}
}
