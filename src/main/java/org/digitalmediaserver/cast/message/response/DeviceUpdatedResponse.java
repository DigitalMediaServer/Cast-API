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
package org.digitalmediaserver.cast.message.response;

import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.cast.event.CastEvent.CastEventType;
import org.digitalmediaserver.cast.message.entity.Device;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Received when volume is changed.
 */
@Immutable
public class DeviceUpdatedResponse extends StandardResponse {

	/** The {@link Device} */
	@JsonProperty
	protected final Device device;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param requestId the request ID.
	 * @param device the {@link Device}.
	 */
	public DeviceUpdatedResponse(
		@JsonProperty("requestId") long requestId,
		@JsonProperty("device") Device device
	) {
		super(requestId);
		this.device = device;
	}

	/**
	 * @return The {@link Device}.
	 */
	public Device getDevice() {
		return device;
	}

	@JsonIgnore
	@Override
	public CastEventType getEventType() {
		return CastEventType.DEVICE_UPDATED;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [").append("requestId=").append(getRequestId());
		if (device != null) {
			builder.append(", device=").append(device);
		}
		builder.append("]");
		return builder.toString();
	}
}
