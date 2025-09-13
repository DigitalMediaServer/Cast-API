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
package org.digitalmediaserver.cast.message.entity;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.cast.message.enumeration.HdrType;
import org.digitalmediaserver.cast.util.Util;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Video information such as video resolution and High Dynamic Range (HDR).
 *
 * @author Nadahar
 */
@Immutable
public class VideoInformation {

	/** HDR type */
	@Nonnull
	@JsonProperty
	protected final HdrType hdrType;

	/** Video height */
	@JsonProperty
	protected int height;

	/** Video width */
	@JsonProperty
	protected int width;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param hdrType the HDR type.
	 * @param height the video height.
	 * @param width the video width.
	 */
	public VideoInformation(
		@JsonProperty("hdrType") HdrType hdrType,
		@JsonProperty("height") int height,
		@JsonProperty("width") int width
	) {
		Util.requireNotNull(hdrType, "hdrType");
		this.hdrType = hdrType;
		this.height = height;
		this.width = width;
	}

	/**
	 * @return The HDR type.
	 */
	@Nonnull
	public HdrType getHdrType() {
		return hdrType;
	}

	/**
	 * @return The video height.
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * @return The video width.
	 */
	public int getWidth() {
		return width;
	}

	@Override
	public int hashCode() {
		return Objects.hash(hdrType, height, width);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof VideoInformation)) {
			return false;
		}
		VideoInformation other = (VideoInformation) obj;
		return hdrType == other.hdrType && height == other.height && width == other.width;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [")
			.append("hdrType=").append(hdrType).append(", ")
			.append("height=").append(height)
			.append(", width=").append(width).append("]");
		return builder.toString();
	}
}
