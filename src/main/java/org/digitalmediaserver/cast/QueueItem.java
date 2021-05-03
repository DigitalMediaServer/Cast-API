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
import java.util.Map;

/**
 * Media item.
 */
public class QueueItem {

	public final boolean autoplay;
	public final Map<String, Object> customData;
	public final Media media;
	public final long id;

	public QueueItem(
		@JsonProperty("autoplay") boolean autoplay,
		@JsonProperty("customData") Map<String, Object> customData,
		@JsonProperty("itemId") long id,
		@JsonProperty("media") Media media
	) {
		this.autoplay = autoplay;
		this.customData = customData != null ? Collections.unmodifiableMap(customData) : null;
		this.id = id;
		this.media = media;
	}

	@Override
	public final int hashCode() {
		return Arrays.hashCode(new Object[] {this.autoplay, this.customData, this.id, this.media});
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
			id == that.id &&
			media == null ?
				that.media == null :
				this.media.equals(that.media);
	}

	@Override
	public final String toString() {
		return String.format("Item{id: %s, media: %s}", this.id, this.media);
	}
}
