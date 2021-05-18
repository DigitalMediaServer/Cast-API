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
package org.digitalmediaserver.cast;

import java.util.Objects;
import javax.annotation.Nullable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * An image that describes a receiver application or media item. This could be
 * an application icon, cover art, or a thumbnail.
 *
 * @author Nadahar
 */
public class Image {

	/** The URL for the image */
	@JsonProperty
	protected final String url;

	/** The height of the image (optional) */
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Integer height;

	/** The width of the image (optional) */
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Integer width;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param url the URL for the image.
	 * @param height the height of the image (optional).
	 * @param width the width of the image (optional).
	 */
	public Image(
		@JsonProperty("url") String url,
		@JsonProperty("height") Integer height,
		@JsonProperty("width") Integer width
	) {
		this.url = url;
		this.height = height;
		this.width = width;
	}

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param url the URL for the image.
	 * @param height the height of the image.
	 * @param width the width of the image.
	 */
	public Image(
		String url,
		int height,
		int width
	) {
		this.url = url;
		this.height = Integer.valueOf(height);
		this.width = Integer.valueOf(width);
	}

	/**
	 * Creates a new instance using the specified URL with unspecified
	 * dimensions.
	 *
	 * @param url the URL for the image.
	 */
	public Image(String url) {
		this.url = url;
		this.height = null;
		this.width = null;
	}

	/**
	 * @return The URL for the image.
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return The optional height of the image or {@code null}.
	 */
	@Nullable
	public Integer getHeight() {
		return height;
	}

	/**
	 * @return The optional width of the image or {@code null}.
	 */
	@Nullable
	public Integer getWidth() {
		return width;
	}

	@Override
	public int hashCode() {
		return Objects.hash(height, url, width);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Image)) {
			return false;
		}
		Image other = (Image) obj;
		return
			Objects.equals(height, other.height) &&
			Objects.equals(url, other.url) &&
			Objects.equals(width, other.width);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		builder.append("url=").append(url);
		if (height != null) {
			builder.append(", ").append("height=").append(height);
		}
		if (width != null) {
			builder.append(", ").append("width=").append(width);
		}
		builder.append("]");
		return builder.toString();
	}
}
