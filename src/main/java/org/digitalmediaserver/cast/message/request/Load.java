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
package org.digitalmediaserver.cast.message.request;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.digitalmediaserver.cast.message.entity.LoadOptions;
import org.digitalmediaserver.cast.message.entity.Media;
import org.digitalmediaserver.cast.message.entity.QueueData;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * A request to load media.
 */
public class Load extends StandardRequest {

	/**
	 * The {@link List} of track IDs that are active. If the array is not
	 * provided, the default tracks will be active
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	protected final List<Integer> activeTrackIds;

	/**
	 * If the autoplay parameter is specified and {@code true}, the media
	 * player will begin playing the content when it is loaded. Even if
	 * autoplay is not specified, the media player implementation may choose
	 * to begin playback immediately
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Boolean autoplay;

	/** Optional user credentials */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final String credentials;

	/**
	 * Optional credentials type. The type '{@code cloud}' is a reserved
	 * type used by load requests that were originated by voice assistant
	 * commands.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final String credentialsType;

	/**
	 * Seconds since beginning of content. If the content is live content,
	 * and {@code currentTime} is not specified, the stream will start at
	 * the live position.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Double currentTime;

	/**
	 * The custom application data to send to the remote application with
	 * the load command
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	protected final Map<String, Object> customData;

	/** Additional load options */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final LoadOptions loadOptions;

	/** The {@link Media} to load */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Media media;

	/** The media playback rate */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Double playbackRate;

	/** The queue to load */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final QueueData queueData;

	/**
	 * Creates a new request to load the specified {@link Media}.
	 *
	 * @param activeTrackIds the {@link List} of track IDs that are active.
	 *            If the list is not provided, the default tracks will be
	 *            active.
	 * @param autoplay {@code true} to ask the remote application to start
	 *            playback as soon as the {@link Media} has been loaded,
	 *            {@code false} to ask it to transition to a paused state
	 *            after loading. If {@code autoplay} is not specified, the
	 *            media player implementation may choose to begin playback
	 *            immediately.
	 * @param credentials the user credentials, if any.
	 * @param credentialsType the credentials type, if any. The type
	 *            '{@code cloud}' is a reserved type used by load requests
	 *            that were originated by voice assistant commands.
	 * @param currentTime the position in seconds from the start for where
	 *            playback is to start in the loaded {@link Media}. If the
	 *            content is live content, and {@code currentTime} is not
	 *            specified, the stream will start at the live position.
	 * @param customData the custom application data to send to the remote
	 *            application with the load command.
	 * @param loadOptions the additional load options, if any.
	 * @param media the {@link Media} to load.
	 * @param playbackRate the media playback rate.
	 * @param queueData the queue to load.
	 * @throws IllegalArgumentException If {@code media} is {@code null}.
	 */
	public Load(
		@Nullable List<Integer> activeTrackIds,
		@Nullable Boolean autoplay,
		@Nullable String credentials,
		@Nullable String credentialsType,
		@Nullable Double currentTime,
		@Nullable Map<String, Object> customData,
		@Nullable LoadOptions loadOptions,
		@Nullable Media media,
		@Nullable Double playbackRate,
		@Nullable QueueData queueData
	) {
		this.activeTrackIds = activeTrackIds;
		this.autoplay = autoplay;
		this.credentials = credentials;
		this.credentialsType = credentialsType;
		this.currentTime = currentTime;
		this.customData = customData;
		this.loadOptions = loadOptions;
		this.media = media;
		this.playbackRate = playbackRate;
		this.queueData = queueData;
	}

	/**
	 * @return The {@link List} of track IDs that are active. If the list is
	 *         not provided, the default tracks will be active.
	 */
	@Nullable
	public List<Integer> getActiveTrackIds() {
		return activeTrackIds;
	}

	/**
	 * @return {@code true} to ask the remote application to start playback
	 *         as soon as the {@link Media} has been loaded, {@code false}
	 *         to ask it to transition to a paused state after loading. If
	 *         {@code autoplay} is not specified, the media player
	 *         implementation may choose to begin playback immediately.
	 */
	@Nullable
	public Boolean getAutoplay() {
		return autoplay;
	}

	/**
	 * @return The user credentials, if any.
	 */
	@Nullable
	public String getCredentials() {
		return credentials;
	}

	/**
	 * @return The credentials type, if any. The type '{@code cloud}' is a
	 *         reserved type used by load requests that were originated by
	 *         voice assistant commands.
	 */
	@Nullable
	public String getCredentialsType() {
		return credentialsType;
	}

	/**
	 * @return The position in seconds from the start for where
	 *            playback is to start in the loaded {@link Media}. If the
	 *            content is live content, and {@code currentTime} is not
	 *            specified, the stream will start at the live position.
	 */
	@Nullable
	public Double getCurrentTime() {
		return currentTime;
	}

	/**
	 * @return The custom application data to send to the remote application
	 *         with the load command.
	 */
	@Nullable
	public Map<String, Object> getCustomData() {
		return customData;
	}

	/**
	 * @return The additional load options, if any.
	 */
	@Nullable
	public LoadOptions getLoadOptions() {
		return loadOptions;
	}

	/**
	 * @return The {@link Media} to load.
	 */
	@Nullable
	public Media getMedia() {
		return media;
	}

	/**
	 * @return The media playback rate.
	 */
	@Nullable
	public Double getPlaybackRate() {
		return playbackRate;
	}

	/**
	 * @return The queue to load.
	 */
	@Nullable
	public QueueData getQueueData() {
		return queueData;
	}
}
