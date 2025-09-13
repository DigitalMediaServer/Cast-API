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
 * The format of a HLS video segment.
 */
public enum HlsVideoSegmentFormat {

	/** MPEG-2 Transport Stream. Supports AVC */
	MPEG2_TS,

	/** Video packed in ISO BMFF CMAF Fragmented MP4. Supports AVC and HEVC */
	FMP4;

	/**
	 * Parses the specified string and returns the corresponding
	 * {@link HlsVideoSegmentFormat}, or {@code null} if no match could be
	 * found.
	 *
	 * @param hlsVideoSegmentFormat the string to parse.
	 * @return The resulting {@link HlsVideoSegmentFormat} or {@code null}.
	 */
	@Nullable
	@JsonCreator
	public static HlsVideoSegmentFormat typeOf(String hlsVideoSegmentFormat) {
		if (Util.isBlank(hlsVideoSegmentFormat)) {
			return null;
		}
		String typeString = hlsVideoSegmentFormat.toUpperCase(Locale.ROOT);
		for (HlsVideoSegmentFormat type : values()) {
			if (typeString.equals(type.name())) {
				return type;
			}
		}
		return null;
	}
}
