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
import javax.annotation.Nullable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A request to stop and unload a media referenced by a specific media
 * session ID.
 * <p>
 * <b>Note</b> This should be a {@link MediaRequest}, but since that would
 * also make it a {@link StandardMessage} which maps {@code type} using
 * Jackson subtypes, it isn't. The reason is that another implementation,
 * {@link Stop}, is already mapped to "{@code STOP}" which is the same
 * {@code type} as this request uses. The differences between the two is the
 * namespace, but that isn't captured by the Jackson subtype logic, which is
 * why this implementation is only a {@link Request} that "manually
 * implements" the remaining fields required for a {@link MediaRequest}.
 */
public class StopMedia implements Request {

	/** The media session ID */
	@JsonProperty
	protected final int mediaSessionId;

	/** the request ID */
	@JsonProperty
	protected long requestId;

	/** The request type */
	@JsonProperty
	protected final String type = "STOP";

	/** Custom data for the receiver application */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	protected final Map<String, Object> customData;

	/**
	 * Creates a new request using the specified parameters.
	 *
	 * @param mediaSessionId the media session ID for which the stop request
	 *            applies.
	 * @param customData the custom data for the receiver application.
	 */
	public StopMedia(int mediaSessionId, @Nullable Map<String, Object> customData) {
		this.mediaSessionId = mediaSessionId;
		this.customData = customData;
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
}
