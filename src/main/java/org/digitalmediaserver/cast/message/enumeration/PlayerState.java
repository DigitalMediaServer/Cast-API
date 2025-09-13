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
package org.digitalmediaserver.cast.message.enumeration;

import java.util.Locale;
import javax.annotation.Nullable;
import org.digitalmediaserver.cast.util.Util;
import com.fasterxml.jackson.annotation.JsonCreator;


/**
 * Represents the player state.
 *
 * @see <a href=
 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.PlayerState">
 *      https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.PlayerState</a>
 * @see <a href=
 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.ExtendedPlayerState">
 *      https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.ExtendedPlayerState</a>
 */
public enum PlayerState {

	/** The player is in IDLE state */
	IDLE,

	/** The player is in PLAYING state */
	PLAYING,

	/** The player is in PAUSED state */
	PAUSED,

	/** The player is in BUFFERING state */
	BUFFERING;

	/**
	 * Parses the specified string and returns the corresponding
	 * {@link PlayerState}, or {@code null} if no match could be found.
	 *
	 * @param playerState the string to parse.
	 * @return The resulting {@link PlayerState} or {@code null}.
	 */
	@Nullable
	@JsonCreator
	public static PlayerState typeOf(String playerState) {
		if (Util.isBlank(playerState)) {
			return null;
		}
		String typeString = playerState.toUpperCase(Locale.ROOT);
		for (PlayerState state : values()) {
			if (typeString.equals(state.name())) {
				return state;
			}
		}
		return null;
	}
}
