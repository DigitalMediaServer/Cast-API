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
import javax.annotation.Nullable;
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
	public static final String METADATA_TRACK_NUMBER = "trackNumber"; //TODO: (Nad) Add rest + implement metadataType + document value type
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
	 * The format of a HLS audio segment.
	 */
	public enum HlsSegmentFormat {

		/** AAC Packed audio elementary stream */
		AAC,

		/** AC3 packed audio elementary stream */
		AC3,

		/** MP3 packed audio elementary stream */
		MP3,

		/** MPEG-2 transport stream */
		TS,

		/** AAC packed MPEG-2 transport stream */
		TS_AAC,

		/** E-AC3 packed audio elementary stream */
		E_AC3,

		/** Audio packed in ISO BMFF CMAF Fragmented MP4 */
		FMP4
	}

	/**
	 * The format of a HLS video segment.
	 */
	public enum HlsVideoSegmentFormat {

		/** MPEG-2 Transport Stream. Supports AVC */
		MPEG2_TS,

		/** Video packed in ISO BMFF CMAF Fragmented MP4. Supports AVC and HEVC */
		FMP4
	}

	/**
	 * The media category.
	 */
	public enum MediaCategory {

		/** Media is audio only */
		AUDIO,

		/** Media is video and audio (the default) */
		VIDEO,

		/** Media is a picture */
		IMAGE
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

	private final String contentId; //TODO: (Nad) Add JavaDocs, getters..

	@JsonProperty
	private final String contentType;

	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String contentUrl;

	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final Map<String, Object> customData;

	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final Double duration;

	/** Optional Google Assistant deep link to a media entity */
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String entity;

	/** The format of the HLS audio segment */
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final HlsSegmentFormat hlsSegmentFormat;

	/** The format of the HLS video segment */
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final HlsVideoSegmentFormat hlsVideoSegmentFormat;

	/** The media cateory (audio, video, picture) */
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final MediaCategory mediaCategory;

	/** The media metadata */
	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final Map<String, Object> metadata;

	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final Long startAbsoluteTime;

	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final StreamType streamType;

	/** The style of text track */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final TextTrackStyle textTrackStyle;

	@Nonnull
	@JsonIgnore
	private final List<Track> tracks;

	public Media(String url, String contentType) {
		this(url, contentType, null, null);
	}

	public Media(String url, String contentType, Double duration, StreamType streamType) {
		this(url, contentType, null, null, duration, null, null, null, null, null, null, streamType, null, null);
	}

	public Media(
		@JsonProperty("contentId") String contentId,
		@JsonProperty("contentType") String contentType,
		@JsonProperty("contentUrl") String contentUrl,
		@JsonProperty("customData") Map<String, Object> customData,
		@JsonProperty("duration") Double duration,
		@JsonProperty("entity") String entity,
		@JsonProperty("hlsSegmentFormat") HlsSegmentFormat hlsSegmentFormat,
		@JsonProperty("hlsVideoSegmentFormat") HlsVideoSegmentFormat hlsVideoSegmentFormat,
		@JsonProperty("mediaCategory") MediaCategory mediaCategory,
		@JsonProperty("metadata") Map<String, Object> metadata,
		@JsonProperty("startAbsoluteTime") Long startAbsoluteTime,
		@JsonProperty("streamType") StreamType streamType,
		@JsonProperty("textTrackStyle") TextTrackStyle textTrackStyle,
		@JsonProperty("tracks") List<Track> tracks
	) {
		this.contentId = contentId;
		this.contentType = contentType;
		this.contentUrl = contentUrl;
		this.duration = duration;
		this.entity = entity;
		this.hlsSegmentFormat = hlsSegmentFormat;
		this.hlsVideoSegmentFormat = hlsVideoSegmentFormat;
		this.mediaCategory = mediaCategory;
		if (metadata == null || metadata.isEmpty()) {
			this.metadata = Collections.emptyMap();
		} else {
			this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
		}
		this.startAbsoluteTime = startAbsoluteTime;
		this.streamType = streamType;
		if (customData == null || customData.isEmpty()) {
			this.customData = Collections.emptyMap();
		} else {
			this.customData = Collections.unmodifiableMap(new LinkedHashMap<>(customData));
		}
		this.textTrackStyle = textTrackStyle;
		if (tracks == null || tracks.isEmpty()) {
			this.tracks = Collections.emptyList();
		} else {
			this.tracks = Collections.unmodifiableList(new ArrayList<>(tracks));
		}
	}


	public String getContentId() {
		return contentId;
	}

	public String getContentType() {
		return contentType;
	}

	public String getContentUrl() {
		return contentUrl;
	}

	/**
	 * Two different fields might hold the media URL, either {@code contentId}
	 * or {@code contentUrl}. The "rules" is that {@code contentUrl} is the URL
	 * if it is populated, otherwise {@code contentId} is the URL.
	 * <p>
	 * This method returns the URL for this {@link Media} instance according to
	 * these rules.
	 *
	 * @return The URL for this {@link Media} or {@code null} if none is
	 *         defined.
	 */
	@Nullable
	@JsonIgnore
	public String getUrl() {
		return Util.isBlank(contentUrl) ? contentId : contentUrl;
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
		return ordinal < MetadataType.values().length ? MetadataType.values()[ordinal] : GENERIC; //TODO: (Nad) Look into this (ordinal)
	}

	@Nullable
	public TextTrackStyle getTextTrackStyle() {
		return textTrackStyle;
	}

	@Nonnull
	public List<Track> getTracks() {
		return tracks;
	}

	@Override
	public final int hashCode() { //TODO: (Nad) Regen
		return Arrays.hashCode(new Object[] {this.contentUrl, this.contentType, this.streamType, this.duration});
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
			this.contentUrl == null ? that.contentUrl == null : this.contentUrl.equals(that.contentUrl) &&
			this.contentType == null ? that.contentType == null : this.contentType.equals(that.contentType) &&
			this.streamType == null ? that.streamType == null : this.streamType.equals(that.streamType) &&
			this.duration == null ? that.duration == null : this.duration.equals(that.duration);
	}

	@Override
	public String toString() { //TODO: (Nad) Update with new fields
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [");
		if (metadata != null) {
			builder.append("metadata=").append(metadata).append(", ");
		}
		if (contentUrl != null) {
			builder.append("url=").append(contentUrl).append(", ");
		}
		if (duration != null) {
			builder.append("duration=").append(duration).append(", ");
		}
		if (streamType != null) {
			builder.append("streamType=").append(streamType).append(", ");
		}
		if (contentType != null) {
			builder.append("contentType=").append(contentType).append(", ");
		}
		if (customData != null) {
			builder.append("customData=").append(customData).append(", ");
		}
		if (textTrackStyle != null) {
			builder.append("textTrackStyle=").append(textTrackStyle).append(", ");
		}
		if (tracks != null) {
			builder.append("tracks=").append(tracks);
		}
		builder.append("]");
		return builder.toString();
	}
}
