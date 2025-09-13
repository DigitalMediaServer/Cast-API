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
 * The reason for the player to be in {@link PlayerState#IDLE} state.
 *
 * @see <a href=
 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.IdleReason">
 *      https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.IdleReason</a>
 */
public enum IdleReason {

	/**
	 * A sender requested to stop playback using the {@code STOP} command
	 */
	CANCELLED,

	/**
	 * A sender requested playing a different media using the {@code LOAD}
	 * command
	 */
	INTERRUPTED,

	/** The media playback completed */
	FINISHED,

	/**
	 * The media was interrupted due to an error, this could happen if, for
	 * example, the player could not download media due to networking errors
	 */
	ERROR,

	/**
	 * <b>Non-API</b>, not supported by Google devices. Pandora is known to
	 * use 'COMPLETED' when the application times out.
	 */
	COMPLETED;

	/**
	 * Parses the specified string and returns the corresponding
	 * {@link IdleReason}, or {@code null} if no match could be found.
	 *
	 * @param idleReason the string to parse.
	 * @return The resulting {@link IdleReason} or {@code null}.
	 */
	@Nullable
	@JsonCreator
	public static IdleReason typeOf(String idleReason) {
		if (Util.isBlank(idleReason)) {
			return null;
		}
		String typeString = idleReason.toUpperCase(Locale.ROOT);
		for (IdleReason type : values()) {
			if (typeString.equals(type.name())) {
				return type;
			}
		}
		return null;
	}
}
