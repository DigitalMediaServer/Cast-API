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
package org.digitalmediaserver.cast;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;


/**
 * Queue item information.
 * <p>
 * Application developers may need to create a {@link QueueItem} to insert a
 * queue element. In this case they should not provide an {@code itemId} (as the
 * actual itemId will be assigned when the item is inserted in the queue). This
 * prevents ID collisions with items added from a sender application.
 */
@Immutable
public class QueueItem {

	/**
	 * {@link List} of {@link Track} {@code trackIds} that are active. If the
	 * list is not provided, the default tracks will be active.
	 */
	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	protected final List<Integer> activeTrackIds;

	/**
	 * If the autoplay parameter is not specified or is {@code true}, the media
	 * player will begin playing the element in the queue when the item becomes
	 * the currentItem.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Boolean autoplay;

	/** The application can define any extra queue item information needed */
	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	protected final Map<String, Object> customData;

	/**
	 * Unique identifier of the item in the queue. The attribute is optional
	 * because for LOAD or INSERT should not be provided (as it will be assigned
	 * by the receiver when an item is first created/inserted).
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Integer itemId;

	/** Metadata (including contentId) of the playlist element */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Media media;

	/** Used to track original order of an item in the queue to undo shuffle */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Integer orderId;

	/**
	 * This parameter is a hint for the receiver to preload this media item
	 * before it is played. It allows for a smooth transition between items
	 * played from the queue. The time is expressed in seconds, relative to the
	 * beginning of this item playback (usually the end of the previous item
	 * playback). Only positive values are valid. For example, if the value is
	 * 10 seconds, this item will be preloaded 10 seconds before the previous
	 * item has finished. The receiver will try to honor this value but will not
	 * guarantee it, for example if the value is larger than the previous item
	 * duration the receiver may just preload this item shortly after the
	 * previous item has started playing (there will never be two items being
	 * preloaded in parallel). Also, if an item is inserted in the queue just
	 * after the currentItem and the time to preload is higher than the time
	 * left on the currentItem, the preload will just happen as soon as
	 * possible.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Double preloadTime;

	/**
	 * Seconds since beginning of content. If the content is live content, and
	 * startTime is not specified, the stream will start at the live position.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Double startTime;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param activeTrackIds the {@link List} of {@link Track} {@code trackIds}
	 *            that are active. If the list is not provided, the default
	 *            tracks will be active.
	 * @param autoplay the autoplay parameter, which if is not specified or is
	 *            {@code true}, the media player will begin playing the element
	 *            in the queue when the item becomes the currentItem.
	 * @param media the {@link Media} instance of the playlist element.
	 * @param preloadTime the preload time in seconds.
	 * @param startTime the number of seconds since beginning of the content. If
	 *            the content is live content, and {@code startTime} is not
	 *            specified, the stream will start at the live position.
	 */
	public QueueItem(
		@Nullable List<Integer> activeTrackIds,
		@Nullable Boolean autoplay,
		@Nullable Media media,
		@Nullable Double preloadTime,
		@Nullable Double startTime
	) {
		this(activeTrackIds, autoplay, null, null, media, null, preloadTime, startTime);
	}

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param activeTrackIds the {@link List} of {@link Track} {@code trackIds}
	 *            that are active. If the list is not provided, the default
	 *            tracks will be active.
	 * @param autoplay the autoplay parameter, which if is not specified or is
	 *            {@code true}, the media player will begin playing the element
	 *            in the queue when the item becomes the currentItem.
	 * @param customData the extra queue item information.
	 * @param itemId the item ID, which is a unique identifier of the item in
	 *            the queue. The attribute is optional because for LOAD or
	 *            INSERT should not be provided (as it will be assigned by the
	 *            receiver when an item is first created/inserted).
	 * @param media the {@link Media} instance of the playlist element.
	 * @param orderId the order ID, which is used to track the original order of
	 *            an item in the queue to undo shuffle.
	 * @param preloadTime the preload time in seconds.
	 * @param startTime the number of seconds since beginning of the content. If
	 *            the content is live content, and {@code startTime} is not
	 *            specified, the stream will start at the live position.
	 */
	public QueueItem(
		@JsonProperty("activeTrackIds") @Nullable List<Integer> activeTrackIds,
		@JsonProperty("autoplay") @Nullable Boolean autoplay,
		@JsonProperty("customData") @Nullable Map<String, Object> customData,
		@JsonProperty("itemId") @Nullable Integer itemId,
		@JsonProperty("media") @Nullable Media media,
		@JsonProperty("orderId") @Nullable Integer orderId,
		@JsonProperty("preloadTime") @Nullable Double preloadTime,
		@JsonProperty("startTime") @Nullable Double startTime
	) {
		if (activeTrackIds == null || activeTrackIds.isEmpty()) {
			this.activeTrackIds = Collections.emptyList();
		} else {
			this.activeTrackIds = Collections.unmodifiableList(new ArrayList<>(activeTrackIds));
		}
		this.autoplay = autoplay;
		if (customData == null || customData.isEmpty()) {
			this.customData = Collections.emptyMap();
		} else {
			this.customData = Collections.unmodifiableMap(new LinkedHashMap<>(customData));
		}
		this.itemId = itemId;
		this.media = media;
		this.orderId = orderId;
		this.preloadTime = preloadTime;
		this.startTime = startTime;
	}

	/**
	 * @return The {@link List} of {@link Track} {@code trackIds} that are
	 *         active. If the list is not provided, the default tracks will be
	 *         active.
	 */
	@Nonnull
	public List<Integer> getActiveTrackIds() {
		return activeTrackIds;
	}

	/**
	 * If the autoplay parameter is not specified or is {@code true}, the media
	 * player will begin playing the element in the queue when the item becomes
	 * the currentItem.
	 *
	 * @return The autoplay status.
	 */
	@Nullable
	public Boolean getAutoplay() {
		return autoplay;
	}

	/**
	 * The application can define any extra queue item information needed.
	 *
	 * @return The extra queue item information.
	 */
	@Nonnull
	public Map<String, Object> getCustomData() {
		return customData;
	}

	/**
	 * The item ID is a unique identifier of the item in the queue. The
	 * attribute is optional because for LOAD or INSERT should not be provided
	 * (as it will be assigned by the receiver when an item is first
	 * created/inserted).
	 *
	 * @return The item ID.
	 */
	@Nullable
	public Integer getItemId() {
		return itemId;
	}

	/**
	 * @return The {@link Media} instance of the playlist element.
	 */
	@Nullable
	public Media getMedia() {
		return media;
	}

	/**
	 * @return The order ID, which is used to track the original order of an
	 *         item in the queue to undo shuffle.
	 */
	@Nullable
	public Integer getOrderId() {
		return orderId;
	}

	/**
	 * This parameter is a hint for the receiver to preload this media item
	 * before it is played. It allows for a smooth transition between items
	 * played from the queue. The time is expressed in seconds, relative to the
	 * beginning of this item playback (usually the end of the previous item
	 * playback). Only positive values are valid. For example, if the value is
	 * 10 seconds, this item will be preloaded 10 seconds before the previous
	 * item has finished. The receiver will try to honor this value but will not
	 * guarantee it, for example if the value is larger than the previous item
	 * duration the receiver may just preload this item shortly after the
	 * previous item has started playing (there will never be two items being
	 * preloaded in parallel). Also, if an item is inserted in the queue just
	 * after the currentItem and the time to preload is higher than the time
	 * left on the currentItem, the preload will just happen as soon as
	 * possible.
	 *
	 * @return The preload time in seconds.
	 */
	@Nullable
	public Double getPreloadTime() {
		return preloadTime;
	}

	/**
	 * @return The number of seconds since beginning of the content. If the
	 *         content is live content, and {@code startTime} is not specified,
	 *         the stream will start at the live position.
	 */
	@Nullable
	public Double getStartTime() {
		return startTime;
	}

	@Override
	public int hashCode() {
		return Objects.hash(activeTrackIds, autoplay, customData, itemId, media, orderId, preloadTime, startTime);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof QueueItem)) {
			return false;
		}
		QueueItem other = (QueueItem) obj;
		return
			Objects.equals(activeTrackIds, other.activeTrackIds) &&
			Objects.equals(autoplay, other.autoplay) &&
			Objects.equals(customData, other.customData) &&
			Objects.equals(itemId, other.itemId) &&
			Objects.equals(media, other.media) &&
			Objects.equals(orderId, other.orderId) &&
			Objects.equals(preloadTime, other.preloadTime) &&
			Objects.equals(startTime, other.startTime);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		if (activeTrackIds != null) {
			builder.append("activeTrackIds=").append(activeTrackIds).append(", ");
		}
		if (autoplay != null) {
			builder.append("autoplay=").append(autoplay).append(", ");
		}
		if (customData != null) {
			builder.append("customData=").append(customData).append(", ");
		}
		if (itemId != null) {
			builder.append("itemId=").append(itemId).append(", ");
		}
		if (media != null) {
			builder.append("media=").append(media).append(", ");
		}
		if (orderId != null) {
			builder.append("orderId=").append(orderId).append(", ");
		}
		if (preloadTime != null) {
			builder.append("preloadTime=").append(preloadTime).append(", ");
		}
		if (startTime != null) {
			builder.append("startTime=").append(startTime);
		}
		builder.append("]");
		return builder.toString();
	}
}
