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

import java.util.Map;

/**
 * Parent class for transport object representing messages sent TO ChromeCast
 * device.
 */
public abstract class StandardRequest extends StandardMessage implements Request {

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
		protected final String[] appId;

		public GetAppAvailability(String... appId) {
			this.appId = appId;
		}
	}

	/**
	 * Request to launch application with specified identifiers.
	 */
	public static class Launch extends StandardRequest {

		@JsonProperty
		protected final String appId;

		public Launch(@JsonProperty("appId") String appId) {
			this.appId = appId;
		}
	}

	/**
	 * Request to stop currently running application.
	 */
	public static class Stop extends StandardRequest {

		@JsonProperty
		protected final String sessionId;

		public Stop(String sessionId) {
			this.sessionId = sessionId;
		}
	}

	/**
	 * Request to load media.
	 */
	public static class Load extends StandardRequest {

		@JsonProperty
		protected final String sessionId;
		@JsonProperty
		protected final Media media;
		@JsonProperty
		protected final boolean autoplay;
		@JsonProperty
		protected final double currentTime;
		@JsonProperty
		protected final Object customData;

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
	}

	/**
	 * Abstract request for an action with currently played media.
	 */
	public abstract static class MediaRequest extends StandardRequest {

		@JsonProperty
		protected final long mediaSessionId;
		@JsonProperty
		protected final String sessionId;

		public MediaRequest(long mediaSessionId, String sessionId) {
			this.mediaSessionId = mediaSessionId;
			this.sessionId = sessionId;
		}
	}

	/**
	 * Request to start/resume playback.
	 */
	public static class Play extends MediaRequest {

		public Play(long mediaSessionId, String sessionId) {
			super(mediaSessionId, sessionId);
		}
	}

	/**
	 * Request to pause playback.
	 */
	public static class Pause extends MediaRequest {

		public Pause(long mediaSessionId, String sessionId) {
			super(mediaSessionId, sessionId);
		}
	}

	/**
	 * Request to change current playback position.
	 */
	public static class Seek extends MediaRequest {

		@JsonProperty
		protected final double currentTime;

		public Seek(long mediaSessionId, String sessionId, double currentTime) {
			super(mediaSessionId, sessionId);
			this.currentTime = currentTime;
		}
	}

	/**
	 * Request to change volume.
	 */
	public static class SetVolume extends StandardRequest {

		@JsonProperty
		protected final Volume volume;

		public SetVolume(Volume volume) {
			this.volume = volume;
		}
	}

	public static GetStatus status() {
		return new GetStatus();
	}

	public static GetAppAvailability getAppAvailability(String... applicationId) {
		return new GetAppAvailability(applicationId);
	}

	public static Launch launch(String applicationId) {
		return new Launch(applicationId);
	}

	public static Stop stop(String sessionId) {
		return new Stop(sessionId);
	}

	public static Load load(
		String sessionId,
		Media media,
		boolean autoplay,
		double currentTime,
		Map<String, String> customData
	) {
		return new Load(sessionId, media, autoplay, currentTime, customData);
	}

	public static Play play(String sessionId, long mediaSessionId) {
		return new Play(mediaSessionId, sessionId);
	}

	public static Pause pause(String sessionId, long mediaSessionId) {
		return new Pause(mediaSessionId, sessionId);
	}

	public static Seek seek(String sessionId, long mediaSessionId, double currentTime) {
		return new Seek(mediaSessionId, sessionId, currentTime);
	}

	public static SetVolume setVolume(Volume volume) {
		return new SetVolume(volume);
	}
}
