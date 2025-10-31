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
package org.digitalmediaserver.cast.message.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.cast.message.enumeration.IdleReason;
import org.digitalmediaserver.cast.message.enumeration.PlayerState;
import org.digitalmediaserver.cast.message.enumeration.RepeatMode;
import org.digitalmediaserver.cast.message.enumeration.SupportedMediaCommand;
import org.digitalmediaserver.cast.util.Util;


/**
 * Current media player status - which media is played, volume, time position,
 * etc.
 *
 * @see <a href=
 *      "https://web.archive.org/web/20170321214906/https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.MediaStatus">
 *      Original definition</a>
 * @see <a href=
 *      "https://developers.google.com/cast/docs/reference/web_receiver/cast.framework.messages.MediaStatus">
 *      Updated definition</a>
 */
@Immutable
public class MediaStatus {

	/** The {@link List} of IDs corresponding to the active tracks */
	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	protected final List<Integer> activeTrackIds;

	/** The ID of the media item that originated the status change */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Integer currentItemId;

	/** The current playback position */
	@JsonProperty
	protected final double currentTime;

	/** Application-specific media status data */
	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	protected final Map<String, Object> customData;

	/** Extended media status information */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final ExtendedMediaStatus extendedStatus;

	/**
	 * If the state is {@link PlayerState#IDLE}, the reason the player went to
	 * {@code IDLE} state.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final IdleReason idleReason;

	/** The {@link List} of media queue items */
	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	protected final List<QueueItem> items;

	/**
	 * The seekable range of a live or event stream. It uses relative media time
	 * in seconds. It will be undefined for VOD streams.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final LiveSeekableRange liveSeekableRange;

	/**
	 * The ID of the media Item currently loading. If there is no item being
	 * loaded, it will be {@code null}.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Integer loadingItemId;

	/** The media information */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Media media;

	/** Unique id for the session */
	@JsonProperty
	protected final int mediaSessionId;

	/** The playback rate */
	@JsonProperty
	protected final float playbackRate;

	/** The playback state */
	@Nonnull
	@JsonProperty
	protected final PlayerState playerState;

	/**
	 * ID of the next Item, only available if it has been preloaded. Media items
	 * can be preloaded and cached temporarily in memory, so when they are
	 * loaded later on, the process is faster (as the media does not have to be
	 * fetched from the network).
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Integer preloadedItemId;

	/** The queue data */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final QueueData queueData;

	/** The behavior of the queue when all items have been played */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final RepeatMode repeatMode;

	/** The commands supported by this player */
	@JsonProperty
	protected final int supportedMediaCommands;

	/** The video information */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final VideoInformation videoInfo;

	/** The current stream volume */
	@Nullable
	@JsonProperty
	protected final MediaVolume volume;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param activeTrackIds the {@link List} of IDs corresponding to the active
	 *            tracks.
	 * @param currentItemId the ID of the media item that originated the status
	 *            change.
	 * @param currentTime the current playback position.
	 * @param customData the application-specific media status data.
	 * @param extendedStatus the extended media status information.
	 * @param idleReason the reason the player went to {@link PlayerState#IDLE}
	 *            if the current state is {@link PlayerState#IDLE}, otherwise
	 *            {@code null}.
	 * @param items the {@link List} of media {@link QueueItem}s.
	 * @param liveSeekableRange the seekable range of a live or event stream. It
	 *            uses relative media time in seconds. It will be undefined for
	 *            VOD streams.
	 * @param loadingItemId the ID of the media item currently loading. If there
	 *            is no item being loaded, it will be {@code null}.
	 * @param media the media information.
	 * @param mediaSessionId the unique id for the session.
	 * @param playbackRate the playback rate.
	 * @param playerState the playback state.
	 * @param preloadedItemId the ID of the next Item, only available if it has
	 *            been preloaded. Media items can be preloaded and cached
	 *            temporarily in memory, so when they are loaded later on, the
	 *            process is faster (as the media does not have to be fetched
	 *            from the network).
	 * @param queueData the queue data.
	 * @param repeatMode the behavior of the queue when all items have been
	 *            played.
	 * @param supportedMediaCommands the commands supported by this player.
	 * @param videoInfo the video information.
	 * @param volume the current stream volume.
	 * @throws IllegalArgumentException If {@code playerState} is {@code null}.
	 */
	public MediaStatus(
		@JsonProperty("activeTrackIds") @Nullable List<Integer> activeTrackIds,
		@JsonProperty("currentItemId") @Nullable Integer currentItemId,
		@JsonProperty("currentTime") double currentTime,
		@JsonProperty("customData") @Nullable Map<String, Object> customData,
		@JsonProperty("extendedStatus") @Nullable ExtendedMediaStatus extendedStatus,
		@JsonProperty("idleReason") @Nullable IdleReason idleReason,
		@JsonProperty("items") @Nullable List<QueueItem> items,
		@JsonProperty("liveSeekableRange") @Nullable LiveSeekableRange liveSeekableRange,
		@JsonProperty("loadingItemId") @Nullable Integer loadingItemId,
		@JsonProperty("media") @Nullable Media media,
		@JsonProperty("mediaSessionId") int mediaSessionId,
		@JsonProperty("playbackRate") float playbackRate,
		@JsonProperty("playerState") @Nonnull PlayerState playerState,
		@JsonProperty("preloadedItemId") @Nullable Integer preloadedItemId,
		@JsonProperty("queueData") @Nullable QueueData queueData,
		@JsonProperty("repeatMode") @Nullable RepeatMode repeatMode,
		@JsonProperty("supportedMediaCommands") int supportedMediaCommands,
		@JsonProperty("videoInfo") @Nullable VideoInformation videoInfo,
		@JsonProperty("volume") @Nullable MediaVolume volume
	) {
		Util.requireNotNull(playerState, "playerState");
		if (activeTrackIds == null || activeTrackIds.isEmpty()) {
			this.activeTrackIds = Collections.emptyList();
		} else {
			this.activeTrackIds = Collections.unmodifiableList(new ArrayList<>(activeTrackIds));
		}
		this.currentItemId = currentItemId;
		this.currentTime = currentTime;
		if (customData == null) {
			this.customData = Collections.emptyMap();
		} else {
			this.customData = Collections.unmodifiableMap(new LinkedHashMap<>(customData));
		}
		this.extendedStatus = extendedStatus;
		this.idleReason = idleReason;
		if (items == null || items.isEmpty()) {
			this.items = Collections.emptyList();
		} else {
			this.items = Collections.unmodifiableList(new ArrayList<>(items));
		}
		this.liveSeekableRange = liveSeekableRange;
		this.loadingItemId = loadingItemId;
		this.media = media;
		this.mediaSessionId = mediaSessionId;
		this.playbackRate = playbackRate;
		this.playerState = playerState;
		this.preloadedItemId = preloadedItemId;
		this.queueData = queueData;
		this.repeatMode = repeatMode;
		this.supportedMediaCommands = supportedMediaCommands;
		this.videoInfo = videoInfo;
		this.volume = volume;
	}

	/**
	 * @return The {@link List} of IDs corresponding to the active tracks.
	 */
	@Nonnull
	public List<Integer> getActiveTrackIds() {
		return activeTrackIds;
	}

	/**
	 * @return The ID of the media item that originated the status change.
	 */
	@Nullable
	public Integer getCurrentItemId() {
		return currentItemId;
	}

	/**
	 * @return The current playback position.
	 */
	public double getCurrentTime() {
		return currentTime;
	}

	/**
	 * @return The application-specific media status data.
	 */
	@Nonnull
	public Map<String, Object> getCustomData() {
		return customData;
	}

	/**
	 * @return The extended media status information.
	 */
	@Nullable
	public ExtendedMediaStatus getExtendedStatus() {
		return extendedStatus;
	}

	/**
	 * @return The reason the player went to {@link PlayerState#IDLE} if the
	 *         current state is {@link PlayerState#IDLE}, otherwise
	 *         {@code null}.
	 */
	@Nullable
	public IdleReason getIdleReason() {
		return idleReason;
	}

	/**
	 * @return The {@link List} of media {@link QueueItem}s.
	 */
	@Nonnull
	public List<QueueItem> getItems() {
		return items;
	}

	/**
	 * @return The seekable range of a live or event stream. It uses relative
	 *         media time in seconds. It will be undefined for VOD streams.
	 */
	@Nullable
	public LiveSeekableRange getLiveSeekableRange() {
		return liveSeekableRange;
	}

	/**
	 * @return The ID of the media item currently loading. If there is no item
	 *         being loaded, it will be {@code null}.
	 */
	@Nullable
	public Integer getLoadingItemId() {
		return loadingItemId;
	}

	/**
	 * @return The media information.
	 */
	@Nullable
	public Media getMedia() {
		return media;
	}

	/**
	 * @return The unique id for the session.
	 */
	public int getMediaSessionId() {
		return mediaSessionId;
	}

	/**
	 * @return The playback rate.
	 */
	public float getPlaybackRate() {
		return playbackRate;
	}

	/**
	 * @return The playback state.
	 */
	@Nonnull
	public PlayerState getPlayerState() {
		return playerState;
	}

	/**
	 * @return The ID of the next Item, only available if it has been preloaded.
	 *         Media items can be preloaded and cached temporarily in memory, so
	 *         when they are loaded later on, the process is faster (as the
	 *         media does not have to be fetched from the network).
	 */
	@Nullable
	public Integer getPreloadedItemId() {
		return preloadedItemId;
	}

	/**
	 * @return The queue data.
	 */
	@Nullable
	public QueueData getQueueData() {
		return queueData;
	}

	/**
	 * @return The behavior of the queue when all items have been played.
	 */
	@Nullable
	public RepeatMode getRepeatMode() {
		return repeatMode;
	}

	/**
	 * The sum of the codes for the various supported commends. Use
	 * {@link SupportedMediaCommand#parseCommands(int)} to convert this value
	 * into a set of {@link SupportedMediaCommand}s.
	 *
	 * @return The commands supported by this player.
	 */
	public int getSupportedMediaCommands() {
		return supportedMediaCommands;
	}

	/**
	 * @return The video information.
	 */
	@Nullable
	public VideoInformation getVideoInfo() {
		return videoInfo;
	}

	/**
	 * @return The current stream volume.
	 */
	@Nullable
	public MediaVolume getVolume() {
		return volume;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			activeTrackIds, currentItemId, currentTime, customData, extendedStatus, idleReason, items,
			liveSeekableRange, loadingItemId, media, mediaSessionId, playbackRate, playerState,
			preloadedItemId, queueData, repeatMode, supportedMediaCommands, videoInfo, volume
		);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MediaStatus)) {
			return false;
		}
		MediaStatus other = (MediaStatus) obj;
		return
			Objects.equals(activeTrackIds, other.activeTrackIds) &&
			Objects.equals(currentItemId, other.currentItemId) &&
			Double.doubleToLongBits(currentTime) == Double.doubleToLongBits(other.currentTime) &&
			Objects.equals(customData, other.customData) &&
			Objects.equals(extendedStatus, other.extendedStatus) &&
			idleReason == other.idleReason &&
			Objects.equals(items, other.items) &&
			Objects.equals(liveSeekableRange, other.liveSeekableRange) &&
			Objects.equals(loadingItemId, other.loadingItemId) &&
			Objects.equals(media, other.media) &&
			mediaSessionId == other.mediaSessionId &&
			Float.floatToIntBits(playbackRate) == Float.floatToIntBits(other.playbackRate) &&
			playerState == other.playerState &&
			Objects.equals(preloadedItemId, other.preloadedItemId) &&
			Objects.equals(queueData, other.queueData) &&
			repeatMode == other.repeatMode &&
			supportedMediaCommands == other.supportedMediaCommands &&
			Objects.equals(videoInfo, other.videoInfo) &&
			Objects.equals(volume, other.volume
		);
	}


	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		if (activeTrackIds != null) {
			builder.append("activeTrackIds=").append(activeTrackIds).append(", ");
		}
		if (currentItemId != null) {
			builder.append("currentItemId=").append(currentItemId).append(", ");
		}
		builder.append("currentTime=").append(currentTime).append(", ");
		if (customData != null) {
			builder.append("customData=").append(customData).append(", ");
		}
		if (extendedStatus != null) {
			builder.append("extendedStatus=").append(extendedStatus).append(", ");
		}
		if (idleReason != null) {
			builder.append("idleReason=").append(idleReason).append(", ");
		}
		if (items != null) {
			builder.append("items=").append(items).append(", ");
		}
		if (liveSeekableRange != null) {
			builder.append("liveSeekableRange=").append(liveSeekableRange).append(", ");
		}
		if (loadingItemId != null) {
			builder.append("loadingItemId=").append(loadingItemId).append(", ");
		}
		if (media != null) {
			builder.append("media=").append(media).append(", ");
		}
		builder.append("mediaSessionId=").append(mediaSessionId)
			.append(", playbackRate=").append(playbackRate).append(", ")
			.append("playerState=").append(playerState).append(", ");
		if (preloadedItemId != null) {
			builder.append("preloadedItemId=").append(preloadedItemId).append(", ");
		}
		if (queueData != null) {
			builder.append("queueData=").append(queueData).append(", ");
		}
		if (repeatMode != null) {
			builder.append("repeatMode=").append(repeatMode).append(", ");
		}
		builder.append("supportedMediaCommands=").append(supportedMediaCommands).append(", ");
		if (videoInfo != null) {
			builder.append(", ").append("videoInfo=").append(videoInfo);
		}
		if (volume != null) {
			builder.append(", ").append("volume=").append(volume);
		}
		return builder.append("]").toString();
	}
}
