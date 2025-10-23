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
package org.digitalmediaserver.cast;

import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nonnull;


/**
 * This represents the (fundamental) capabilities of a cast device.
 *
 * @author Nadahar
 */
public enum CastDeviceCapability {

	/** The cast device has no known capabilities */
	NONE(0),

	/** The cast device can play video media */
	VIDEO_OUT(1 << 0),

	/** The cast device can provide video media */
	VIDEO_IN(1 << 1),

	/** The cast device can play audio media */
	AUDIO_OUT(1 << 2),

	/** The cast device can provide audio media */
	AUDIO_IN(1 << 3),

	/** The cast device supports or is in (?) development mode */
	DEV_MODE(1 << 4),

	/** The device is a multizone group */
	MULTIZONE_GROUP(1 << 5),

	/** The device is a dynamic group */
	DYNAMIC_GROUP(1 << 6),

	/** The device is a multichannel group */
	MULTI_CHANNEL_GROUP(1 << 7),

	/** The device is a multichannel member */
	MULTI_CHANNEL_MEMBER(1 << 8),

	/** The device has master or fixed volume mode capability */
	MASTER_OR_FIXED_VOLUME(1 << 11),

	/** The device has attenuation or fixed volume mode capability */
	ATTENUATION_OR_FIXED_VOLUME(1 << 12),

	/** The device can be part of a dynamic group */
	DYNAMIC_GROUPING_SUPPORTED(1 << 16);

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
