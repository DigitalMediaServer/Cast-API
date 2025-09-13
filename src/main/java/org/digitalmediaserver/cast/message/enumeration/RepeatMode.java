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
 * The behavior of the queue when all items have been played.
 *
 * @see <a href=
 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.repeatMode">
 *      https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.repeatMode</a>
 */
public enum RepeatMode {

	/** When the queue is completed the media session is terminated */
	REPEAT_OFF,

	/**
	 * All the items in the queue will be played indefinitely, when the last
	 * item is played it will play the first item again
	 */
	REPEAT_ALL,

	/** The current item will be played repeatedly */
	REPEAT_SINGLE,

	/**
	 * All the items in the queue will be played indefinitely, when the last
	 * item is played it will play the first item again (the list will be
	 * shuffled by the receiver first)
	 */
	REPEAT_ALL_AND_SHUFFLE;

	/**
	 * Parses the specified string and returns the corresponding
	 * {@link RepeatMode}, or {@code null} if no match could be found.
	 *
	 * @param repeatMode the string to parse.
	 * @return The resulting {@link RepeatMode} or {@code null}.
	 */
	@Nullable
	@JsonCreator
	public static RepeatMode typeOf(String repeatMode) {
		if (Util.isBlank(repeatMode)) {
			return null;
		}
		String typeString = repeatMode.toUpperCase(Locale.ROOT);
		for (RepeatMode type : values()) {
			if (typeString.equals(type.name())) {
				return type;
			}
		}
		return null;
	}
}
