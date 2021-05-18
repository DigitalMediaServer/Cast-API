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
package org.digitalmediaserver.chromecast.api;

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

	@Nonnull
	private final List<Device> devices;
	private final boolean isMultichannel;

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

	@Nonnull
	public List<Device> getDevices() {
		return devices;
	}

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
