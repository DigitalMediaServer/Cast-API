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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.cast.message.enumeration.QueueType;
import org.digitalmediaserver.cast.message.enumeration.RepeatMode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Queue data as part of the LOAD request.
 *
 * @author Nadahar
 */
@Immutable
public class QueueData {

	/** Metadata to describe the queue content, and optionally media sections */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final ContainerMetadata containerMetadata;

	/** Description of the queue */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final String description;

	/** Optional Queue entity ID, provide Google Assistant deep link */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final String entity;

	/** ID of the queue */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final String id;

	/**
	 * List of queue items. It is sorted (first element will be played first).
	 */
	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	protected final List<QueueItem> items;

	/** Name of the queue */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final String name;

	/** Queue type, e.g. album, playlist, radio station, tv series, etc. */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final QueueType queueType;

	/** Continuous playback behavior of the queue */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final RepeatMode repeatMode;

	/** Indicate if the queue is shuffled */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Boolean shuffle;

	/**
	 * The index of the item in the queue that should be used to start playback
	 * first
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Integer startIndex;

	/**
	 * Seconds (since the beginning of content) to start playback of the first
	 * item
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Double startTime;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param containerMetadata the metadata to describe the queue content.
	 * @param description the description of the queue.
	 * @param entity the optional queue entity ID, provide Google Assistant deep
	 *            link.
	 * @param id the ID of the queue.
	 * @param items the {@link List} of {@link QueueItem}s. It is sorted (first
	 *            element will be played first).
	 * @param name the name of the queue.
	 * @param queueType the queue type, e.g. album, playlist, radio station, tv
	 *            series, etc.
	 * @param repeatMode the continuous playback behavior of the queue.
	 * @param shuffle {@code true} if the queue is shuffled, {@code false}
	 *            otherwise.
	 * @param startIndex the index of the item in the queue that should be used
	 *            to start playback first.
	 * @param startTime the seconds (since the beginning of content) to start
	 *            playback of the first item.
	 */
	public QueueData(
		@JsonProperty("containerMetadata") ContainerMetadata containerMetadata,
		@JsonProperty("description") String description,
		@JsonProperty("entity") String entity,
		@JsonProperty("id") String id,
		@JsonProperty("items") List<QueueItem> items,
		@JsonProperty("name") String name,
		@JsonProperty("queueType") QueueType queueType,
		@JsonProperty("repeatMode") RepeatMode repeatMode,
		@JsonProperty("shuffle") Boolean shuffle,
		@JsonProperty("startIndex") Integer startIndex,
		@JsonProperty("startTime") Double startTime
	) {
		this.containerMetadata = containerMetadata;
		this.description = description;
		this.entity = entity;
		this.id = id;
		if (items == null || items.isEmpty()) {
			this.items = Collections.emptyList();
		} else {
			this.items = Collections.unmodifiableList(new ArrayList<>(items));
		}
		this.name = name;
		this.queueType = queueType;
		this.repeatMode = repeatMode;
		this.shuffle = shuffle;
		this.startIndex = startIndex;
		this.startTime = startTime;
	}

	/**
	 * @return The metadata to describe the queue content.
	 */
	@Nullable
	public ContainerMetadata getContainerMetadata() {
		return containerMetadata;
	}

	/**
	 * @return The description of the queue.
	 */
	@Nullable
	public String getDescription() {
		return description;
	}

	/**
	 * @return The optional queue entity ID, provide Google Assistant deep link.
	 */
	@Nullable
	public String getEntity() {
		return entity;
	}

	/**
	 * @return The ID of the queue.
	 */
	@Nullable
	public String getId() {
		return id;
	}

	/**
	 * @return The {@link List} of {@link QueueItem}s. It is sorted (first
	 *         element will be played first).
	 */
	@Nonnull
	public List<QueueItem> getItems() {
		return items;
	}

	/**
	 * @return The name of the queue.
	 */
	@Nullable
	public String getName() {
		return name;
	}

	/**
	 * @return The queue type, e.g. album, playlist, radio station, tv series,
	 *         etc.
	 */
	@Nullable
	public QueueType getQueueType() {
		return queueType;
	}

	/**
	 * @return The continuous playback behavior of the queue.
	 */
	@Nullable
	public RepeatMode getRepeatMode() {
		return repeatMode;
	}

	/**
	 * @return {@code true} if the queue is shuffled, {@code false} otherwise.
	 */
	@Nullable
	public Boolean getShuffle() {
		return shuffle;
	}

	/**
	 * @return The index of the item in the queue that should be used to start
	 *         playback first.
	 */
	@Nullable
	public Integer getStartIndex() {
		return startIndex;
	}

	/**
	 * @return The seconds (since the beginning of content) to start playback of
	 *         the first item.
	 */
	@Nullable
	public Double getStartTime() {
		return startTime;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			containerMetadata, description, entity, id, items, name, queueType, repeatMode, shuffle, startIndex, startTime
		);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof QueueData)) {
			return false;
		}
		QueueData other = (QueueData) obj;
		return
			Objects.equals(containerMetadata, other.containerMetadata) &&
			Objects.equals(description, other.description) &&
			Objects.equals(entity, other.entity) &&
			Objects.equals(id, other.id) &&
			Objects.equals(items, other.items) &&
			Objects.equals(name, other.name) &&
			queueType == other.queueType &&
			repeatMode == other.repeatMode &&
			Objects.equals(shuffle, other.shuffle) &&
			Objects.equals(startIndex, other.startIndex) &&
			Objects.equals(startTime, other.startTime);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		if (containerMetadata != null) {
			builder.append("containerMetadata=").append(containerMetadata).append(", ");
		}
		if (description != null) {
			builder.append("description=").append(description).append(", ");
		}
		if (entity != null) {
			builder.append("entity=").append(entity).append(", ");
		}
		if (id != null) {
			builder.append("id=").append(id).append(", ");
		}
		if (!items.isEmpty()) {
			builder.append("items=").append(items).append(", ");
		}
		if (name != null) {
			builder.append("name=").append(name).append(", ");
		}
		if (queueType != null) {
			builder.append("queueType=").append(queueType).append(", ");
		}
		if (repeatMode != null) {
			builder.append("repeatMode=").append(repeatMode).append(", ");
		}
		if (shuffle != null) {
			builder.append("shuffle=").append(shuffle).append(", ");
		}
		if (startIndex != null) {
			builder.append("startIndex=").append(startIndex).append(", ");
		}
		if (startTime != null) {
			builder.append("startTime=").append(startTime);
		}
		builder.append("]");
		return builder.toString();
	}
}
