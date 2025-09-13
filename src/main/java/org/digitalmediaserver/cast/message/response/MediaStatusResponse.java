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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.cast.event.CastEvent.CastEventType;
import org.digitalmediaserver.cast.message.entity.MediaStatus;
import org.digitalmediaserver.cast.util.MediaStatusResponseDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


/**
 * Response to "MediaStatus" request.
 */
@Immutable
@JsonDeserialize(using = MediaStatusResponseDeserializer.class)
public class MediaStatusResponse extends StandardResponse {

	/** The {@link List} of {@link MediaStatus}es */
	@JsonProperty
	protected final List<MediaStatus> statuses;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param requestId the request ID.
	 * @param statuses the {@link MediaStatus}es.
	 */
	public MediaStatusResponse(
		@JsonProperty("requestId") long requestId,
		@JsonProperty("status") MediaStatus... statuses
	) {
		super(requestId);
		if (statuses == null || statuses.length == 0) {
			this.statuses = Collections.emptyList();
		} else {
			this.statuses = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(statuses)));
		}
	}

	/**
	 * @return The {@link List} of {@link MediaStatus}es.
	 */
	@Nonnull
	@JsonProperty("status")
	public List<MediaStatus> getStatuses() {
		return statuses;
	}

	@JsonIgnore
	@Override
	public CastEventType getEventType() {
		return CastEventType.MEDIA_STATUS;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [").append("requestId=").append(getRequestId());
		if (statuses != null) {
			builder.append(", statuses=").append(statuses);
		}
		builder.append("]");
		return builder.toString();
	}
}
