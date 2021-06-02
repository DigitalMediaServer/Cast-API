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

import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nonnull;


//TODO: (Nad) JavaDocs
public enum CastDeviceCapability {
	NONE(0),
	VIDEO_OUT(1 << 0),
	VIDEO_IN(1 << 1),
	AUDIO_OUT(1 << 2),
	AUDIO_IN(1 << 3),
	DEV_MODE(1 << 4),
	MULTIZONE_GROUP(1 << 5);

	private final int mask;

	private CastDeviceCapability(int mask) {
		this.mask = mask;
	}

	/**
	 * @return The mask value of this capability.
	 */
	public int getMask() {
		return mask;
	}

	/**
	 * Checks whether this capability's bit is active in the specified value.
	 *
	 * @param value the value to check.
	 * @return {@code true} if this capability is contained in the specified
	 *         value, false otherwise.
	 */
	public boolean isIn(int value) {
		return (value & mask) == mask;
	}

	/**
	 * Generates a {@link Set} of {@code CastDeviceCapabilities} that
	 * corresponds to the specified "{@code ca}" value from the device's
	 * {@code DNS-SD} record.
	 *
	 * @param value the "{@code ca}" value.
	 * @return The corresponding {@link Set} of capabilities.
	 */
	@Nonnull
	public static EnumSet<CastDeviceCapability> getCastDeviceCapabilities(int value) {
		if (value <= 0) {
			return EnumSet.of(NONE);
		}
		EnumSet<CastDeviceCapability> result = EnumSet.noneOf(CastDeviceCapability.class);
		for (CastDeviceCapability capability : values()) {
			if (capability == NONE) {
				continue;
			}
			if ((capability.mask & value) == capability.mask) {
				result.add(capability);
			}
		}
		return result;
	}
}
