/*
 * Copyright 2014 Vitaly Litvak (vitavaque@gmail.com)
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
package org.digitalmediaserver.cast.message.request;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.digitalmediaserver.cast.message.entity.MediaVolume;
import org.digitalmediaserver.cast.util.Util;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A request to set the stream volume of a media referenced by a specific
 * media session ID.
 * <p>
 * <b>Note</b> This should be a {@link MediaRequest}, but since that would
 * also make it a {@link StandardMessage} which maps {@code type} using
 * Jackson subtypes, it isn't. The reason is that another implementation,
 * {@link SetVolume}, is already mapped to "{@code SET_VOLUME}" which is the
 * same {@code type} as this request uses. The differences between the two
 * is the namespace, but that isn't captured by the Jackson subtype logic,
 * which is why this implementation is only a {@link Request} that "manually
 * implements" the remaining fields required for a {@link MediaRequest}.
 */
public class VolumeRequest implements Request {

	/** The media session ID */
	@JsonProperty
	protected final int mediaSessionId;

	/** The session ID */
	@JsonProperty
	protected final String sessionId;

	/** the request ID */
	@JsonProperty
	protected long requestId;

	/** The request type */
	@JsonProperty
	protected final String type = "SET_VOLUME";

	/**
	 * The new volume of the stream. At least one of level or muted must be
	 * set.
	 */
	@Nonnull
	@JsonProperty
	protected final MediaVolume volume;

	/** Custom data for the receiver application */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	protected final Map<String, Object> customData;

	/**
	 * Creates a new request using the specified parameters.
	 *
	 * @param sessionId the session ID to use.
	 * @param mediaSessionId the media session ID for which the seek request
	 *            applies.
	 * @param volume the new volume of the stream. At least one of level or
	 *            muted must be set.
	 * @param customData the custom data for the receiver application.
	 * @throws IllegalArgumentException If {@code sessionId} or
	 *             {@code volume} is {@code null}.
	 */
	public VolumeRequest(
		String sessionId,
		int mediaSessionId,
		@Nonnull MediaVolume volume,
		@Nullable Map<String, Object> customData
	) {
		Util.requireNotBlank(sessionId, "sessionId");
		Util.requireNotNull(volume, "volume");
		this.sessionId = sessionId;
		this.mediaSessionId = mediaSessionId;
		this.volume = volume;
		this.customData = customData;
	}

	/**
	 * @return The new volume of the stream. At least one of level or muted
	 *         must be set.
	 */
	@Nonnull
	public MediaVolume getVolume() {
		return volume;
	}

	/**
	 * @return The custom data for the receiver application.
	 */
	@Nullable
	public Map<String, Object> getCustomData() {
		return customData;
	}

	@Override
	public void setRequestId(long requestId) {
		this.requestId = requestId;
	}

	@Override
	public long getRequestId() {
		return requestId;
	}

	/**
	 * @return The media session ID.
	 */
	public int getMediaSessionId() {
		return mediaSessionId;
	}

	/**
	 * @return the session ID.
	 */
	public String getSessionId() {
		return sessionId;
	}
}
