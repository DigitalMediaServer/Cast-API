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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Current media player status - which media is played, volume, time position,
 * etc.
 *
 * @see <a href=
 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.MediaStatus">
 *      https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.MediaStatus</a>
 */
@Immutable
public class MediaStatus {

	/**
	 * Playback status.
	 *
	 * @see <a href=
	 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.PlayerState">
	 *      https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.PlayerState</a>
	 * @see <a href=
	 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.ExtendedPlayerState">
	 *      https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.ExtendedPlayerState</a>
	 */
	public enum PlayerState {
		IDLE, BUFFERING, PLAYING, PAUSED, LOADING
	}

	/**
	 * @see <a href=
	 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.repeatMode">
	 *      https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.repeatMode</a>
	 */
	public enum RepeatMode {
		REPEAT_OFF, REPEAT_ALL, REPEAT_SINGLE, REPEAT_ALL_AND_SHUFFLE
	}

	/**
	 * <p>
	 * The reason for the player to be in IDLE state.
	 * </p>
	 *
	 * <p>
	 * Pandora is known to use 'COMPLETED' when the app timesout
	 * </p>
	 *
	 * @see <a href=
	 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.IdleReason">
	 *      https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.IdleReason</a>
	 */
	public enum IdleReason {
		CANCELLED, INTERRUPTED, FINISHED, ERROR, COMPLETED
	}

	@Nonnull
	private final List<Integer> activeTrackIds;
	private final long mediaSessionId;
	private final float playbackRate;
	private final PlayerState playerState;
	private final Integer currentItemId;
	private final double currentTime;

	@Nonnull
	private final Map<String, Object> customData;
	private final Integer loadingItemId;

	@Nonnull
	private final List<Item> items;
	private final Integer preloadedItemId;
	private final int supportedMediaCommands;
	private final Volume volume;
	private final Media media;
	private final RepeatMode repeatMode;
	private final IdleReason idleReason;

	public MediaStatus(
		@JsonProperty("activeTrackIds") List<Integer> activeTrackIds,
		@JsonProperty("mediaSessionId") long mediaSessionId,
		@JsonProperty("playbackRate") float playbackRate,
		@JsonProperty("playerState") PlayerState playerState,
		@JsonProperty("currentItemId") Integer currentItemId,
		@JsonProperty("currentTime") double currentTime,
		@JsonProperty("customData") Map<String, Object> customData,
		@JsonProperty("loadingItemId") Integer loadingItemId,
		@JsonProperty("items") List<Item> items,
		@JsonProperty("preloadedItemId") Integer preloadedItemId,
		@JsonProperty("supportedMediaCommands") int supportedMediaCommands,
		@JsonProperty("volume") Volume volume,
		@JsonProperty("media") Media media,
		@JsonProperty("repeatMode") RepeatMode repeatMode,
		@JsonProperty("idleReason") IdleReason idleReason
	) {
		if (activeTrackIds == null) {
			this.activeTrackIds = Collections.emptyList();
		} else {
			this.activeTrackIds = Collections.unmodifiableList(new ArrayList<>(activeTrackIds));
		}
		this.mediaSessionId = mediaSessionId;
		this.playbackRate = playbackRate;
		this.playerState = playerState;
		this.currentItemId = currentItemId;
		this.currentTime = currentTime;
		if (customData == null) {
			this.customData = Collections.emptyMap();
		} else {
			this.customData = Collections.unmodifiableMap(new LinkedHashMap<>(customData));
		}
		this.loadingItemId = loadingItemId;
		if (items == null) {
			this.items = Collections.emptyList();
		} else {
			this.items = Collections.unmodifiableList(new ArrayList<>(items));
		}
		this.preloadedItemId = preloadedItemId;
		this.supportedMediaCommands = supportedMediaCommands;
		this.volume = volume;
		this.media = media;
		this.repeatMode = repeatMode;
		this.idleReason = idleReason;
	}

	@Nonnull
	public List<Integer> getActiveTrackIds() {
		return activeTrackIds;
	}

	public long getMediaSessionId() {
		return mediaSessionId;
	}

	public float getPlaybackRate() {
		return playbackRate;
	}

	public PlayerState getPlayerState() {
		return playerState;
	}

	public Integer getCurrentItemId() {
		return currentItemId;
	}

	public double getCurrentTime() {
		return currentTime;
	}

	@Nonnull
	public Map<String, Object> getCustomData() {
		return customData;
	}

	public Integer getLoadingItemId() {
		return loadingItemId;
	}

	@Nonnull
	public List<Item> getItems() {
		return items;
	}

	public Integer getPreloadedItemId() {
		return preloadedItemId;
	}

	public int getSupportedMediaCommands() {
		return supportedMediaCommands;
	}

	public Volume getVolume() {
		return volume;
	}

	public Media getMedia() {
		return media;
	}

	public RepeatMode getRepeatMode() {
		return repeatMode;
	}

	public IdleReason getIdleReason() {
		return idleReason;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [");
		if (activeTrackIds != null) {
			builder.append("activeTrackIds=").append(activeTrackIds).append(", ");
		}
		builder.append("mediaSessionId=").append(mediaSessionId).append(", playbackRate=").append(playbackRate).append(", ");
		if (playerState != null) {
			builder.append("playerState=").append(playerState).append(", ");
		}
		if (currentItemId != null) {
			builder.append("currentItemId=").append(currentItemId).append(", ");
		}
		builder.append("currentTime=").append(currentTime).append(", ");
		if (customData != null) {
			builder.append("customData=").append(customData).append(", ");
		}
		if (loadingItemId != null) {
			builder.append("loadingItemId=").append(loadingItemId).append(", ");
		}
		if (items != null) {
			builder.append("items=").append(items).append(", ");
		}
		if (preloadedItemId != null) {
			builder.append("preloadedItemId=").append(preloadedItemId).append(", ");
		}
		builder.append("supportedMediaCommands=").append(supportedMediaCommands).append(", ");
		if (volume != null) {
			builder.append("volume=").append(volume).append(", ");
		}
		if (media != null) {
			builder.append("media=").append(media).append(", ");
		}
		if (repeatMode != null) {
			builder.append("repeatMode=").append(repeatMode).append(", ");
		}
		if (idleReason != null) {
			builder.append("idleReason=").append(idleReason);
		}
		builder.append("]");
		return builder.toString();
	}
}
