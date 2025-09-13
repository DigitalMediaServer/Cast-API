/*
 * Copyright 2018 Vitaly Litvak (vitavaque@gmail.com)
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
package org.digitalmediaserver.cast.message.entity;

import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.cast.CastDeviceCapability;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Device descriptor.
 */
@Immutable
public class Device {

	/** The device name */
	protected final String name;

	/** The encoded device capabilities */
	protected final int capabilities;

	/** The device ID */
	protected final String deviceId;

	/** The {@link Volume} instance */
	protected final Volume volume;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param name the device name.
	 * @param capabilities the encoded device capabilities.
	 * @param deviceId the device ID.
	 * @param volume the {@link Volume} instance.
	 */
	public Device(
		@JsonProperty("name") String name,
		@JsonProperty("capabilities") int capabilities,
		@JsonProperty("deviceId") String deviceId,
		@JsonProperty("volume") Volume volume
	) {
		this.name = name;
		this.capabilities = capabilities;
		this.deviceId = deviceId;
		this.volume = volume;
	}

	/**
	 * @return The name of the device.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return The (encoded) device capabilities. Use
	 *         {@link CastDeviceCapability#getCastDeviceCapabilities(int)} to
	 *         decode.
	 */
	public int getCapabilities() {
		return capabilities;
	}

	/**
	 * @return The device ID.
	 */
	public String getDeviceId() {
		return deviceId;
	}

	/**
	 * @return The {@link Volume} instance.
	 */
	public Volume getVolume() {
		return volume;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [");
		if (name != null) {
			builder.append("name=").append(name).append(", ");
		}
		builder.append("capabilities=").append(capabilities).append(", ");
		if (deviceId != null) {
			builder.append("deviceId=").append(deviceId).append(", ");
		}
		if (volume != null) {
			builder.append("volume=").append(volume);
		}
		builder.append("]");
		return builder.toString();
	}
}
