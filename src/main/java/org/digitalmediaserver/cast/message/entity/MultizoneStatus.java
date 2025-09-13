/*
 * Copyright 2019 Vitaly Litvak (vitavaque@gmail.com)
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Status received in case there a multiple ChomeCast devices in several "zones"
 * (multi-zone setup).
 */
@Immutable
public class MultizoneStatus {

	/** The {@link List} of {@link Device}s */
	@Nonnull
	protected final List<Device> devices;

	/** {@code true} if the zone is multi-channel, {@code false} if it isn't */
	protected final boolean isMultichannel;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param devices the array of {@link Device}s.
	 * @param isMultichannel {@code true} if the zone is multi-channel,
	 *            {@code false} if it isn't.
	 */
	public MultizoneStatus(
		@JsonProperty("devices") Device[] devices,
		@JsonProperty("isMultichannel") boolean isMultichannel
	) {
		if (devices == null || devices.length == 0) {
			this.devices = Collections.emptyList();
		} else {
			this.devices = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(devices)));
		}
		this.isMultichannel = isMultichannel;
	}

	/**
	 * @return The {@link Device}s of the zone.
	 */
	@Nonnull
	public List<Device> getDevices() {
		return devices;
	}

	/**
	 * @return {@code true} if the zone is multi-channel, {@code false} if it
	 *         isn't.
	 */
	@JsonProperty("isMultichannel")
	public boolean isMultichannel() {
		return isMultichannel;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [");
		if (devices != null) {
			builder.append("devices=").append(devices).append(", ");
		}
		builder.append("isMultichannel=").append(isMultichannel).append("]");
		return builder.toString();
	}
}
