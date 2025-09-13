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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.cast.event.CastEvent.CastEventType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Response to "AppAvailability" request.
 */
@Immutable
public class AppAvailabilityResponse extends StandardResponse {

	/** The {@link Map} of application ID and availability information */
	@Nonnull
	@JsonProperty
	protected final Map<String, String> availability;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param requestId the request ID.
	 * @param availability the {@link Map} containing the availability information.
	 */
	public AppAvailabilityResponse(
		@JsonProperty("requestId") long requestId,
		@JsonProperty("availability") Map<String, String> availability
	) {
		super(requestId);
		if (availability == null || availability.isEmpty()) {
			this.availability = Collections.emptyMap();
		} else {
			this.availability = Collections.unmodifiableMap(new LinkedHashMap<>(availability));
		}
	}

	/**
	 * @return The {@link Map} containing the availability information.
	 */
	@Nonnull
	public Map<String, String> getAvailability() {
		return availability;
	}

	@JsonIgnore
	@Override
	public CastEventType getEventType() {
		return CastEventType.APPLICATION_AVAILABILITY;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [").append("requestId=").append(getRequestId());
		if (availability != null) {
			builder.append(", availability=").append(availability);
		}
		builder.append("]");
		return builder.toString();
	}
}
