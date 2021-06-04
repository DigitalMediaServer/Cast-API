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
package org.digitalmediaserver.chromecast.api;

import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the volume of a media session stream.
 *
 * @author Nadahar
 */
@Immutable
public class MediaVolume {

	/** Value from 0 to 1 that represents the current stream volume level */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final Double level;

	/** Whether the stream is muted */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final Boolean muted;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param level the value between 0 and 1 that represents the current stream
	 *            volume level.
	 * @param muted whether the stream is muted.
	 */
	public MediaVolume(
		@JsonProperty("level") Double level,
		@JsonProperty("muted") Boolean muted
	) {
		this.level = level;
		this.muted = muted;
	}

	/**
	 * @return The value between 0 and 1 that represents the current stream
	 *         volume level.
	 */
	@Nullable
	public Double getLevel() {
		return level;
	}

	/**
	 * @return Whether the stream is muted.
	 */
	@Nullable
	public Boolean getMuted() {
		return muted;
	}

	@Override
	public int hashCode() {
		return Objects.hash(level, muted);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MediaVolume)) {
			return false;
		}
		MediaVolume other = (MediaVolume) obj;
		return Objects.equals(level, other.level) && Objects.equals(muted, other.muted);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		if (level != null) {
			builder.append("level=").append(level).append(", ");
		}
		if (muted != null) {
			builder.append("muted=").append(muted);
		}
		builder.append("]");
		return builder.toString();
	}
}
