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
import org.digitalmediaserver.cast.message.enumeration.ResumeState;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * A request to change current playback position of a media referenced by a
 * specific media session ID.
 */
public class Seek extends MediaRequest {

	/** The new playback position in seconds */
	@JsonProperty
	protected final double currentTime;

	/** Custom data for the receiver application */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	protected final Map<String, Object> customData;

	/**
	 * The desired media player state after the seek is complete. If
	 * {@code null}, it will retain the state it had before seeking
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final ResumeState resumeState;

	/**
	 * Creates a new request to move the playback position of the media
	 * referenced by the specified media session ID to the specified
	 * position.
	 *
	 * @param mediaSessionId the media session ID for which the seek request
	 *            applies.
	 * @param sessionId the session ID to use.
	 * @param currentTime the new playback position in seconds.
	 * @param resumeState the desired media player state after the seek is
	 *            complete. If {@code null}, it will retain the state it had
	 *            before seeking.
	 */
	public Seek(
		int mediaSessionId,
		@Nonnull String sessionId,
		double currentTime,
		@Nullable ResumeState resumeState
	) {
		super(mediaSessionId, sessionId);
		this.currentTime = currentTime;
		this.customData = null;
		this.resumeState = resumeState;
	}

	/**
	 * Creates a new request to move the playback position of the media
	 * referenced by the specified media session ID to the specified
	 * position.
	 *
	 * @param mediaSessionId the media session ID for which the seek request
	 *            applies.
	 * @param sessionId the session ID to use.
	 * @param currentTime the new playback position in seconds.
	 * @param customData the custom data for the receiver application.
	 * @param resumeState the desired media player state after the seek is
	 *            complete. If {@code null}, it will retain the state it had
	 *            before seeking.
	 */
	public Seek(
		int mediaSessionId,
		@Nonnull String sessionId,
		double currentTime,
		@Nullable Map<String, Object> customData,
		@Nullable ResumeState resumeState
	) {
		super(mediaSessionId, sessionId);
		this.currentTime = currentTime;
		this.customData = customData;
		this.resumeState = resumeState;
	}

	/**
	 * @return The new playback position in seconds.
	 */
	public double getCurrentTime() {
		return currentTime;
	}

	/**
	 * @return The custom data for the receiver application.
	 */
	@Nullable
	public Map<String, Object> getCustomData() {
		return customData;
	}

	/**
	 * @return The desired media player state after the seek is complete. If
	 *         {@code null}, it will retain the state it had before seeking.
	 */
	@Nullable
	public ResumeState getResumeState() {
		return resumeState;
	}
}
