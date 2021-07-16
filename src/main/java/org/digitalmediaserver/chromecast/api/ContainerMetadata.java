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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * Common container metadata used as part of QueueData.
 *
 * @author Nadahar
 */
@Immutable
public class ContainerMetadata {

	/**
	 * Container duration in seconds. For example an audiobook playback time.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Double containerDuration;

	/**
	 * Container images. For example a live TV channel logo, audiobook cover,
	 * album cover art, etc.
	 */
	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	protected final List<Image> containerImages;

	/** The type of container object */
	@Nonnull
	@JsonProperty
	protected final ContainerType containerType;

	/**
	 * The title of the container, for example an audiobook title, a TV channel
	 * name, etc.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final String title;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param containerDuration the container duration in seconds.
	 * @param containerImages the container images. For example a live TV
	 *            channel logo, audiobook cover, album cover art, etc.
	 * @param containerType the type of container object.
	 * @param title the title of the container, for example an audiobook title,
	 *            a TV channel name, etc.
	 */
	public ContainerMetadata(
		@JsonProperty("containerDuration") @Nullable Double containerDuration,
		@JsonProperty("containerImages") @Nullable List<Image> containerImages,
		@JsonProperty("containerType") @Nonnull ContainerType containerType,
		@JsonProperty("title") @Nullable String title
	) {
		Util.requireNotNull(containerType, "containerType");
		this.containerDuration = containerDuration;
		if (containerImages == null || containerImages.isEmpty()) {
			this.containerImages = Collections.emptyList();
		} else {
			this.containerImages = Collections.unmodifiableList(new ArrayList<>(containerImages));
		}
		this.containerType = containerType;
		this.title = title;
	}

	/**
	 * @return The container duration in seconds.
	 */
	@Nullable
	public Double getContainerDuration() {
		return containerDuration;
	}

	/**
	 * @return The container images. For example a live TV channel logo,
	 *         audiobook cover, album cover art, etc.
	 */
	@Nonnull
	public List<Image> getContainerImages() {
		return containerImages;
	}

	/**
	 * @return The type of container object.
	 */
	@Nonnull
	public ContainerType getContainerType() {
		return containerType;
	}

	/**
	 * @return The title of the container, for example an audiobook title, a TV
	 *         channel name, etc.
	 */
	@Nonnull
	public String getTitle() {
		return title;
	}


	@Override
	public int hashCode() {
		return Objects.hash(containerDuration, containerImages, containerType, title);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ContainerMetadata)) {
			return false;
		}
		ContainerMetadata other = (ContainerMetadata) obj;
		return
			Objects.equals(containerDuration, other.containerDuration) &&
			Objects.equals(containerImages, other.containerImages) &&
			containerType == other.containerType &&
			Objects.equals(title, other.title);
	}


	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		if (containerDuration != null) {
			builder.append("containerDuration=").append(containerDuration).append(", ");
		}
		builder.append("containerImages=").append(containerImages).append(", ");
		builder.append("containerType=").append(containerType);
		if (title != null) {
			builder.append(", ").append("title=").append(title);
		}
		builder.append("]");
		return builder.toString();
	}


	/**
	 * Possible types of container metadata.
	 */
	public enum ContainerType {

		/**
		 * Generic template suitable for most media types. Used by
		 * {@link ContainerMetadata}.
		 */
		GENERIC_CONTAINER(0),

		/**
		 * Metadata for an audiobook. Used by
		 * {@code AudiobookContainerMetadata}.
		 */
		AUDIOBOOK_CONTAINER(1);

		private int value;

		private ContainerType(int value) {
			this.value = value;
		}

		/**
		 * @return The integer value of this container type.
		 */
		@JsonValue
		public int getValue() {
			return value;
		}

		/**
		 * Returns the {@link ContainerType} that corresponds to the specified
		 * integer value, or {@code null} if nothing corresponds.
		 *
		 * @param value the integer value whose corresponding
		 *            {@link ContainerType} to find.
		 * @return The {@link ContainerType} or {@code null}.
		 */
		@JsonCreator
		@Nullable
		public static ContainerType typeOf(int value) {
			switch (value) {
				case 0:
					return GENERIC_CONTAINER;
				case 1:
					return AUDIOBOOK_CONTAINER;
				default:
					return null;
			}
		}
	}
}
