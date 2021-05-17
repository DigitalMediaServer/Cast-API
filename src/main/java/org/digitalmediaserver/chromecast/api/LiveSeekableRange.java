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
 * Provides the live seekable range with start and end time in seconds.
 *
 * @author Nadahar
 */
@Immutable
public class LiveSeekableRange {

	/** The start time in seconds */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Double start;

	/** The end time in seconds */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Double end;

	/**
	 * A boolean value indicates whether a live stream is ended. If it is done,
	 * the end of live seekable range should stop updating.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Boolean isLiveDone;

	/**
	 * A boolean value indicates whether the live seekable range is a moving
	 * window. If false, it will be either a expanding range or a fixed range
	 * meaning live has ended.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Boolean isMovingWindow;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param start the start time in seconds.
	 * @param end the end time in seconds.
	 * @param isLiveDone {@code true} if the live stream has ended,
	 *            {@code false} otherwise.
	 * @param isMovingWindow {@code true} if the live seekable range is a moving
	 *            window, {@code false} if it's not.
	 */
	public LiveSeekableRange(
		@JsonProperty("start") Double start,
		@JsonProperty("end") Double end,
		@JsonProperty("isLiveDone") Boolean isLiveDone,
		@JsonProperty("isMovingWindow") Boolean isMovingWindow
	) {
		this.start = start;
		this.end = end;
		this.isLiveDone = isLiveDone;
		this.isMovingWindow = isMovingWindow;
	}

	/**
	 * @return The start time in seconds.
	 */
	@Nullable
	public Double getStart() {
		return start;
	}

	/**
	 * @return The end time in seconds.
	 */
	@Nullable
	public Double getEnd() {
		return end;
	}

	/**
	 * A boolean value indicates whether a live stream is ended. If it is done,
	 * the end of live seekable range should stop updating.
	 *
	 * @return {@code true} if the live stream has ended, {@code false}
	 *         otherwise.
	 */
	@Nullable
	public Boolean getIsLiveDone() {
		return isLiveDone;
	}

	/**
	 * A boolean value indicates whether the live seekable range is a moving
	 * window. If false, it will be either a expanding range or a fixed range
	 * meaning live has ended.
	 *
	 * @return {@code true} if the live seekable range is a moving window,
	 *         {@code false} if it's not.
	 */
	@Nullable
	public Boolean getIsMovingWindow() {
		return isMovingWindow;
	}

	@Override
	public int hashCode() {
		return Objects.hash(end, isLiveDone, isMovingWindow, start);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof LiveSeekableRange)) {
			return false;
		}
		LiveSeekableRange other = (LiveSeekableRange) obj;
		return
			Objects.equals(end, other.end) &&
			Objects.equals(isLiveDone, other.isLiveDone) &&
			Objects.equals(isMovingWindow, other.isMovingWindow) &&
			Objects.equals(start, other.start);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		if (start != null) {
			builder.append("start=").append(start).append(", ");
		}
		if (end != null) {
			builder.append("end=").append(end).append(", ");
		}
		if (isLiveDone != null) {
			builder.append("isLiveDone=").append(isLiveDone).append(", ");
		}
		if (isMovingWindow != null) {
			builder.append("isMovingWindow=").append(isMovingWindow);
		}
		builder.append("]");
		return builder.toString();
	}
}
