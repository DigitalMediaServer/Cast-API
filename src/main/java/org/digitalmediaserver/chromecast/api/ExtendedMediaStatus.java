/*
 * Copyright (C) 2021 Digital Media Server developers.
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

import static org.digitalmediaserver.chromecast.api.Util.requireNotNull;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Extended media status information.
 *
 * @author Nadahar
 */
@Immutable
public class ExtendedMediaStatus {

	/** The extended player state */
	@Nonnull
	@JsonProperty
	private final ExtendedPlayerState playerState;

	/** The {@link Media} instance */
	@Nullable
	@JsonProperty
	private final Media media;

	/** The media session ID */
	@Nullable
	@JsonProperty
	private final Integer mediaSessionId;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param playerState the extended player state.
	 * @param media the {@link Media} instance.
	 * @param mediaSessionId the media session ID.
	 */
	public ExtendedMediaStatus(
		@JsonProperty("playerState") @Nonnull ExtendedPlayerState playerState,
		@JsonProperty("media") @Nullable Media media,
		@JsonProperty("mediaSessionId") @Nullable Integer mediaSessionId
	) {
		requireNotNull(playerState, "playerState");
		this.playerState = playerState;
		this.media = media;
		this.mediaSessionId = mediaSessionId;
	}

	/**
	 * @return The extended player state.
	 */
	@Nonnull
	public ExtendedPlayerState getPlayerState() {
		return playerState;
	}

	/**
	 * @return The {@link Media} instance.
	 */
	@Nullable
	public Media getMedia() {
		return media;
	}

	/**
	 * @return The media session ID.
	 */
	@Nullable
	public Integer getMediaSessionId() {
		return mediaSessionId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(media, mediaSessionId, playerState);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ExtendedMediaStatus)) {
			return false;
		}
		ExtendedMediaStatus other = (ExtendedMediaStatus) obj;
		return
			Objects.equals(media, other.media) &&
			Objects.equals(mediaSessionId, other.mediaSessionId) &&
			playerState == other.playerState;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		builder.append("playerState=").append(playerState);
		if (media != null) {
			builder.append(", ").append("media=").append(media);
		}
		if (mediaSessionId != null) {
			builder.append(", ").append("mediaSessionId=").append(mediaSessionId);
		}
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Extended player state information.
	 */
	public enum ExtendedPlayerState {

		/** The player is in LOADING state */
		LOADING
	}
}
