/*
 * Copyright (C) 2025 Digital Media Server developers.
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
package org.digitalmediaserver.cast.message.enumeration;

import javax.annotation.Nullable;
import org.digitalmediaserver.cast.message.entity.Metadata;
import org.digitalmediaserver.cast.message.entity.Metadata.MusicTrack;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Describes the type of the data found inside the {@code metadata}. You can
 * access the type with the key {@link Metadata#METADATA_TYPE}.
 *
 * You can access known metadata types using the constants like for example
 * {@link MusicTrack#ALBUM_TITLE}.
 *
 * @see <a href=
 *      "https://developers.google.com/cast/docs/reference/ios/interface_g_c_k_media_metadata">
 *      https://developers.google.com/cast/docs/reference/ios/interface_g_c_k_media_metadata</a>
 * @see <a href=
 *      "https://developers.google.com/android/reference/com/google/android/gms/cast/MediaMetadata">
 *      https://developers.google.com/android/reference/com/google/android/gms/cast/MediaMetadata</a>
 */
public enum MetadataType {

	/**
	 * Generic template suitable for most media types. Used by
	 * {@code GenericMediaMetadata}.
	 */
	GENERIC(0),

	/** A full length movie. Used by {@code MovieMediaMetadata}. */
	MOVIE(1),

	/** An episode of a TV series. Used by {@code TvShowMediaMetadata}. */
	TV_SHOW(2),

	/** A music track. Used by {@code MusicTrackMediaMetadata}. */
	MUSIC_TRACK(3),

	/** A photo. Used by {@code PhotoMediaMetadata}. */
	PHOTO(4),

	/** An audiobook chapter. Used by {@code AudiobookChapterMediaMetadata}. */
	AUDIOBOOK_CHAPTER(5);

	private int code;

	private MetadataType(int code) {
		this.code = code;
	}

	/**
	 * @return The numerical code representing this {@link MetadataType}.
	 */
	@JsonValue
	public int getCode() {
		return code;
	}

	/**
	 * Returns the {@link MetadataType} that corresponds to the specified
	 * integer value, or {@code null} if nothing corresponds.
	 *
	 * @param code the integer value whose corresponding
	 *            {@link MetadataType} to find.
	 * @return The {@link MetadataType} or {@code null}.
	 */
	@Nullable
	@JsonCreator
	public static MetadataType typeOf(int code) {
		for (MetadataType type : values()) {
			if (type.code == code) {
				return type;
			}
		}
		return null;
	}
}
