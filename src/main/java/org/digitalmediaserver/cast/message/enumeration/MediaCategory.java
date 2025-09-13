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
 * The media category.
 */
public enum MediaCategory {

	/** Media is audio only */
	AUDIO,

	/** Media is video and audio (the default) */
	VIDEO,

	/** Media is a picture */
	IMAGE;

	/**
	 * Parses the specified string and returns the corresponding
	 * {@link MediaCategory}, or {@code null} if no match could be found.
	 *
	 * @param mediaCategory the string to parse.
	 * @return The resulting {@link MediaCategory} or {@code null}.
	 */
	@Nullable
	@JsonCreator
	public static MediaCategory typeOf(String mediaCategory) {
		if (Util.isBlank(mediaCategory)) {
			return null;
		}
		String typeString = mediaCategory.toUpperCase(Locale.ROOT);
		for (MediaCategory type : values()) {
			if (typeString.equals(type.name())) {
				return type;
			}
		}
		return null;
	}
}
