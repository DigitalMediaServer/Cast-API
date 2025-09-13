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
 * The format of a HLS audio segment.
 */
public enum HlsSegmentFormat {

	/** AAC Packed audio elementary stream */
	AAC,

	/** AC3 packed audio elementary stream */
	AC3,

	/** MP3 packed audio elementary stream */
	MP3,

	/** MPEG-2 transport stream */
	TS,

	/** AAC packed MPEG-2 transport stream */
	TS_AAC,

	/** E-AC3 packed audio elementary stream */
	E_AC3,

	/** Audio packed in ISO BMFF CMAF Fragmented MP4 */
	FMP4;

	/**
	 * Parses the specified string and returns the corresponding
	 * {@link HlsSegmentFormat}, or {@code null} if no match could be found.
	 *
	 * @param hlsSegmentFormat the string to parse.
	 * @return The resulting {@link HlsSegmentFormat} or {@code null}.
	 */
	@Nullable
	@JsonCreator
	public static HlsSegmentFormat typeOf(String hlsSegmentFormat) {
		if (Util.isBlank(hlsSegmentFormat)) {
			return null;
		}
		String typeString = hlsSegmentFormat.toUpperCase(Locale.ROOT);
		for (HlsSegmentFormat type : values()) {
			if (typeString.equals(type.name())) {
				return type;
			}
		}
		return null;
	}
}
