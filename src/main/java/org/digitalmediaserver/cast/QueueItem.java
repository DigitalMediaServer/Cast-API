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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Media item.
 */
@Immutable
public class QueueItem {

	protected final boolean autoplay;

	@Nonnull
	protected final Map<String, Object> customData;
	protected final Media media;
	protected final long itemId;

	public QueueItem(
		@JsonProperty("autoplay") boolean autoplay,
		@JsonProperty("customData") Map<String, Object> customData,
		@JsonProperty("itemId") long itemId,
		@JsonProperty("media") Media media
	) {
		this.autoplay = autoplay;
		if (customData == null || customData.isEmpty()) {
			this.customData = Collections.emptyMap();
		} else {
			this.customData = Collections.unmodifiableMap(new LinkedHashMap<>(customData));
		}
		this.itemId = itemId;
		this.media = media;
	}

	public boolean isAutoplay() {
		return autoplay;
	}

	@Nonnull
	public Map<String, Object> getCustomData() {
		return customData;
	}

	public long getItemId() {
		return itemId;
	}

	public Media getMedia() {
		return media;
	}

	@Override
	public final int hashCode() {
		return Arrays.hashCode(new Object[] {this.autoplay, this.customData, this.itemId, this.media});
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof QueueItem)) {
			return false;
		}
		final QueueItem that = (QueueItem) obj;
		return
			autoplay == that.autoplay &&
			customData == null ?
				that.customData == null :
				customData.equals(that.customData) &&
			itemId == that.itemId &&
			media == null ?
				that.media == null :
				this.media.equals(that.media);
	}

	@Override
	public final String toString() {
		return String.format("Item{id: %s, media: %s}", this.itemId, this.media);
	}
}
