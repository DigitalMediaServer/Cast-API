/*
 * Copyright (C) 2025 Digital Media Server developers.
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
 * This represents the type of cast device.
 *
 * @author Nadahar
 */
public enum DeviceType {

	/** Generic Cast device */
	GENERIC,

	/** Cast-enabled TV */
	TV,

	/** Cast-enabled speaker or other audio device */
	SPEAKER,

	/** Speaker group */
	SPEAKER_GROUP,

	/** The "Nearby Devices" pseudo-device, which represents any nearby unpaired guest-mode devices */
	NEARBY_UNPAIRED;

	/**
	 * Parses the specified string and returns the corresponding
	 * {@link DeviceType}, or {@code null} if no match could be found.
	 *
	 * @param deviceType the string to parse.
	 * @return The resulting {@link DeviceType} or {@code null}.
	 */
	@Nullable
	@JsonCreator
	public static DeviceType typeOf(String deviceType) {
		if (Util.isBlank(deviceType)) {
			return null;
		}
		String typeString = deviceType.toUpperCase(Locale.ROOT).replace(' ', '_');
		for (DeviceType type : values()) {
			if (typeString.equals(type.name())) {
				return type;
			}
		}
		return null;
	}
}
