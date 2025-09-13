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
 * Represents the stream types.
 *
 * @see <a href=
 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.StreamType">
 *      https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.StreamType</a>
 */
public enum StreamType {

	/** VOD and DVR content */
	BUFFERED,

	/** Live linear stream content */
	LIVE,

	/** Not specified/Other */
	NONE;

	/**
	 * Parses the specified string and returns the corresponding
	 * {@link StreamType}, or {@code null} if no match could be found.
	 *
	 * @param streamType the string to parse.
	 * @return The resulting {@link StreamType} or {@code null}.
	 */
	@Nullable
	@JsonCreator
	public static StreamType typeOf(String streamType) {
		if (Util.isBlank(streamType)) {
			return null;
		}
		String typeString = streamType.toUpperCase(Locale.ROOT);
		for (StreamType type : values()) {
			if (typeString.equals(type.name())) {
				return type;
			}
		}
		return null;
	}
}
