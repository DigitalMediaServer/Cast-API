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
package org.digitalmediaserver.chromecast.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import static org.digitalmediaserver.chromecast.api.Media.MetadataType.GENERIC;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Media streamed on ChromeCast device.
 *
 * @see <a href=
 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.MediaInformation">
 *      https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.MediaInformation</a>
 */
@Immutable
public class Media {

	public static final String METADATA_TYPE = "metadataType";
	public static final String METADATA_ALBUM_ARTIST = "albumArtist";
	public static final String METADATA_ALBUM_NAME = "albumName";
	public static final String METADATA_ARTIST = "artist";
	public static final String METADATA_BROADCAST_DATE = "broadcastDate";
	public static final String METADATA_COMPOSER = "composer";
	public static final String METADATA_CREATION_DATE = "creationDate";
	public static final String METADATA_DISC_NUMBER = "discNumber";
	public static final String METADATA_EPISODE_NUMBER = "episodeNumber";
	public static final String METADATA_HEIGHT = "height";
	public static final String METADATA_IMAGES = "images";
	public static final String METADATA_LOCATION_NAME = "locationName";
	public static final String METADATA_LOCATION_LATITUDE = "locationLatitude";
	public static final String METADATA_LOCATION_LONGITUDE = "locationLongitude";
	public static final String METADATA_RELEASE_DATE = "releaseDate";
	public static final String METADATA_SEASON_NUMBER = "seasonNumber";
	public static final String METADATA_SERIES_TITLE = "seriesTitle";
	public static final String METADATA_STUDIO = "studio";
	public static final String METADATA_SUBTITLE = "subtitle";
	public static final String METADATA_TITLE = "title";
	public static final String METADATA_TRACK_NUMBER = "trackNumber";
	public static final String METADATA_WIDTH = "width";

	/**
	 * Type of the data found inside {@link #metadata}. You can access the type
	 * with the key {@link #METADATA_TYPE}.
	 *
	 * You can access known metadata types using the constants in {@link Media},
	 * such as {@link #METADATA_ALBUM_NAME}.
	 *
	 * @see <a href=
	 *      "https://developers.google.com/cast/docs/reference/ios/interface_g_c_k_media_metadata">
	 *      https://developers.google.com/cast/docs/reference/ios/interface_g_c_k_media_metadata</a>
	 * @see <a href=
	 *      "https://developers.google.com/android/reference/com/google/android/gms/cast/MediaMetadata">
	 *      https://developers.google.com/android/reference/com/google/android/gms/cast/MediaMetadata</a>
	 */
	public enum MetadataType {
		GENERIC, MOVIE, TV_SHOW, MUSIC_TRACK, PHOTO
	}

	/**
	 * <p>
	 * Stream type.
	 * </p>
	 *
	 * <p>
	 * Some receivers use upper-case (like Pandora), some use lower-case (like
	 * Google Audio), duplicate elements to support both.
	 * </p>
	 *
	 * @see <a href=
	 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.StreamType">
	 *      https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.StreamType</a>
	 */
	public enum StreamType {
		BUFFERED, buffered, LIVE, live, NONE, none
	}

	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Map<String, Object> metadata;

	@JsonProperty("contentId")
	protected final String url;

	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Double duration;

	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final StreamType streamType;

	@JsonProperty
	protected final String contentType;

	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Map<String, Object> customData;

	@Nonnull
	@JsonIgnore
	protected final Map<String, Object> textTrackStyle;

	@Nonnull
	@JsonIgnore
	protected final List<Track> tracks;

	public Media(String url, String contentType) {
		this(url, contentType, null, null);
	}

	public Media(String url, String contentType, Double duration, StreamType streamType) {
		this(url, contentType, duration, streamType, null, null, null, null);
	}

	public Media(
		@JsonProperty("contentId") String url,
		@JsonProperty("contentType") String contentType,
		@JsonProperty("duration") Double duration,
		@JsonProperty("streamType") StreamType streamType,
		@JsonProperty("customData") Map<String, Object> customData,
		@JsonProperty("metadata") Map<String, Object> metadata,
		@JsonProperty("textTrackStyle") Map<String, Object> textTrackStyle,
		@JsonProperty("tracks") List<Track> tracks
	) {
		this.url = url;
		this.contentType = contentType;
		this.duration = duration;
		this.streamType = streamType;
		if (customData == null || customData.isEmpty()) {
			this.customData = Collections.emptyMap();
		} else {
			this.customData = Collections.unmodifiableMap(new LinkedHashMap<>(customData));
		}
		if (metadata == null || metadata.isEmpty()) {
			this.metadata = Collections.emptyMap();
		} else {
			this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
		}
		if (textTrackStyle == null || textTrackStyle.isEmpty()) {
			this.textTrackStyle = Collections.emptyMap();
		} else {
			this.textTrackStyle = Collections.unmodifiableMap(new LinkedHashMap<>(textTrackStyle));
		}
		if (tracks == null || tracks.isEmpty()) {
			this.tracks = Collections.emptyList();
		} else {
			this.tracks = Collections.unmodifiableList(new ArrayList<>(tracks));
		}
	}

	public String getUrl() {
		return url;
	}

	public String getContentType() {
		return contentType;
	}

	public Double getDuration() {
		return duration;
	}

	public StreamType getStreamType() {
		return streamType;
	}

	@Nonnull
	public Map<String, Object> getCustomData() {
		return customData;
	}

	@Nonnull
	public Map<String, Object> getMetadata() {
		return metadata;
	}

	/**
	 * @return the type defined by the key {@link #METADATA_TYPE}.
	 */
	@JsonIgnore
	public MetadataType getMetadataType() {
		if (!metadata.containsKey(METADATA_TYPE)) {
			return GENERIC;
		}

		Integer ordinal = (Integer) metadata.get(METADATA_TYPE);
		return ordinal < MetadataType.values().length ? MetadataType.values()[ordinal] : GENERIC;
	}

	@Nonnull
	public Map<String, Object> getTextTrackStyle() {
		return textTrackStyle;
	}

	@Nonnull
	public List<Track> getTracks() {
		return tracks;
	}

	@Override
	public final int hashCode() {
		return Arrays.hashCode(new Object[] {this.url, this.contentType, this.streamType, this.duration});
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Media)) {
			return false;
		}
		final Media that = (Media) obj;
		return
			this.url == null ? that.url == null : this.url.equals(that.url) &&
			this.contentType == null ? that.contentType == null : this.contentType.equals(that.contentType) &&
			this.streamType == null ? that.streamType == null : this.streamType.equals(that.streamType) &&
			this.duration == null ? that.duration == null : this.duration.equals(that.duration);
	}

	@Override
	public final String toString() {
		return String.format("Media{url: %s, contentType: %s, duration: %s}", url, contentType, duration);
	}
}
