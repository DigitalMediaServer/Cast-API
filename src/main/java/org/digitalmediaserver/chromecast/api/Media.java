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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.chromecast.api.Metadata.MetadataType;

/**
 * Represents the media information.
 *
 * @see <a href=
 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.MediaInformation">
 *      https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.MediaInformation</a>
 */
@Immutable
public class Media {

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
	 * Represents the stream types.
	 *
	 * @see <a href=
	 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.StreamType">
	 *      https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.StreamType</a>
	 */
	public enum StreamType {

		/** VOD and DVR content */
		BUFFERED,

		/** Live linear stream content */
		LIVE,

		/** Not specified/Other */
		NONE;

		/**
		 * Parses the specified string and returns the corresponding
		 * {@link StreamType}, or {@code null} if no match could be found.
		 *
		 * @param streamType the string to parse.
		 * @return The resulting {@link StreamType} or {@code null}.
		 */
		@Nullable
		@JsonCreator
		public static StreamType typeOf(String streamType) {
			if (Util.isBlank(streamType)) {
				return null;
			}
			String typeString = streamType.toUpperCase(Locale.ROOT);
			for (StreamType type : values()) {
				if (typeString.equals(type.name())) {
					return type;
				}
			}
			return null;
		}
	}

	/** Typically the URL of the media */
	@Nonnull
	@JsonProperty
	private final String contentId;

	/** The content MIME-type */
	@Nonnull
	@JsonProperty
	private final String contentType;

	/**
	 * Optional media URL, to allow using {@code contentId} for real ID. If
	 * {@code contentUrl} is provided, it will be used as media the URL,
	 * otherwise {@code contentId} will be used as the media URL.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String contentUrl;

	/** Application-specific media information */
	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final Map<String, Object> customData;

	/** The media duration */
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
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final Map<String, Object> metadata;

	/**
	 * Provides absolute time (Epoch Unix time in seconds) for live streams. For
	 * live event it would be the time the event started, otherwise it will be
	 * start of the seekable range when the streaming started.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final Long startAbsoluteTime;

	/** The stream type (required) */
	@Nonnull
	@JsonProperty
	private final StreamType streamType;

	/** The style of text track */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final TextTrackStyle textTrackStyle;

	/** The media tracks */
	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final List<Track> tracks;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param contentId the content ID, typically the URL of the media
	 *            (required).
	 * @param contentType the content MIME-type.
	 * @param contentUrl the optional media URL, to allow using
	 *            {@code contentId} for real ID. If {@code contentUrl} is
	 *            provided, it will be used as media the URL, otherwise
	 *            {@code contentId} will be used as the media URL.
	 * @param customData the application-specific media information.
	 * @param duration the media duration.
	 * @param entity the optional Google Assistant deep link to a media entity.
	 * @param hlsSegmentFormat the format of the HLS audio segment.
	 * @param hlsVideoSegmentFormat the format of the HLS video segment.
	 * @param mediaCategory the media category.
	 * @param metadata the media metadata.
	 * @param startAbsoluteTime the absolute time (Epoch Unix time in seconds)
	 *            for live streams. For live event it would be the time the
	 *            event started, otherwise it will be start of the seekable
	 *            range when the streaming started.
	 * @param streamType the stream type (required).
	 * @param textTrackStyle the style of text track.
	 * @param tracks the media {@link Track}s.
	 */
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

	/**
	 * @return The content ID, typically the URL of the media.
	 */
	@Nonnull
	public String getContentId() {
		return contentId;
	}

	/**
	 * @return The content MIME-type.
	 */
	@Nonnull
	public String getContentType() {
		return contentType;
	}

	/**
	 * @return The optional media URL, to allow using {@code contentId} for real
	 *         ID. If {@code contentUrl} is provided, it will be used as media
	 *         the URL, otherwise {@code contentId} will be used as the media
	 *         URL.
	 */
	@Nullable
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

	/**
	 * @return The application-specific media information.
	 */
	@Nonnull
	public Map<String, Object> getCustomData() {
		return customData;
	}

	/**
	 * @return The media duration.
	 */
	@Nullable
	public Double getDuration() {
		return duration;
	}

	/**
	 * @return The optional Google Assistant deep link to a media entity.
	 */
	@Nullable
	public String getEntity() {
		return entity;
	}

	/**
	 * @return The format of the HLS audio segment.
	 */
	@Nullable
	public HlsSegmentFormat getHlsSegmentFormat() {
		return hlsSegmentFormat;
	}

	/**
	 * @return The format of the HLS video segment.
	 */
	@Nullable
	public HlsVideoSegmentFormat getHlsVideoSegmentFormat() {
		return hlsVideoSegmentFormat;
	}

	/**
	 * @return The media category (audio, video, picture).
	 */
	@Nullable
	public MediaCategory getMediaCategory() {
		return mediaCategory;
	}

	/**
	 * @return The media metadata.
	 */
	@Nonnull
	public Map<String, Object> getMetadata() {
		return metadata;
	}

	/**
	 * @return The {@link List} of zero or more {@link Image}s from the media
	 *         metadata.
	 */
	@JsonIgnore
	@Nonnull
	public List<Image> getImages() {
		return Metadata.extractImages(metadata);
	}

	/**
	 * @return The absolute time (Epoch Unix time in seconds) for live streams.
	 *         For live event it would be the time the event started, otherwise
	 *         it will be start of the seekable range when the streaming
	 *         started.
	 */
	@Nullable
	public Long getStartAbsoluteTime() {
		return startAbsoluteTime;
	}

	/**
	 * @return The stream type.
	 */
	@Nonnull
	public StreamType getStreamType() {
		return streamType;
	}

	/**
	 * @return The style of text track.
	 */
	@Nullable
	public TextTrackStyle getTextTrackStyle() {
		return textTrackStyle;
	}

	/**
	 *
	 * @return The media {@link Track}s.
	 */
	@Nonnull
	public List<Track> getTracks() {
		return tracks;
	}

	/**
	 * @return The type defined by the key {@link #METADATA_TYPE}.
	 */
	@JsonIgnore
	public MetadataType getMetadataType() {
		if (!metadata.containsKey(Metadata.METADATA_TYPE)) {
			return MetadataType.GENERIC;
		}

		Integer ordinal = (Integer) metadata.get(Metadata.METADATA_TYPE);
		return ordinal < MetadataType.values().length ? MetadataType.values()[ordinal] : MetadataType.GENERIC; //TODO: (Nad) Look into this (ordinal)
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			contentId, contentType, contentUrl, customData, duration, entity, hlsSegmentFormat,
			hlsVideoSegmentFormat, mediaCategory, metadata, startAbsoluteTime, streamType, textTrackStyle, tracks
		);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Media)) {
			return false;
		}
		Media other = (Media) obj;
		return Objects.equals(contentId, other.contentId) && Objects.equals(contentType, other.contentType) &&
			Objects.equals(contentUrl, other.contentUrl) && Objects.equals(customData, other.customData) &&
			Objects.equals(duration, other.duration) && Objects.equals(entity, other.entity) &&
			hlsSegmentFormat == other.hlsSegmentFormat && hlsVideoSegmentFormat == other.hlsVideoSegmentFormat &&
			mediaCategory == other.mediaCategory && Objects.equals(metadata, other.metadata) &&
			Objects.equals(startAbsoluteTime, other.startAbsoluteTime) && streamType == other.streamType &&
			Objects.equals(textTrackStyle, other.textTrackStyle) && Objects.equals(tracks, other.tracks);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		if (contentId != null) {
			builder.append("contentId=").append(contentId).append(", ");
		}
		if (contentType != null) {
			builder.append("contentType=").append(contentType).append(", ");
		}
		if (contentUrl != null) {
			builder.append("contentUrl=").append(contentUrl).append(", ");
		}
		if (customData != null) {
			builder.append("customData=").append(customData).append(", ");
		}
		if (duration != null) {
			builder.append("duration=").append(duration).append(", ");
		}
		if (entity != null) {
			builder.append("entity=").append(entity).append(", ");
		}
		if (hlsSegmentFormat != null) {
			builder.append("hlsSegmentFormat=").append(hlsSegmentFormat).append(", ");
		}
		if (hlsVideoSegmentFormat != null) {
			builder.append("hlsVideoSegmentFormat=").append(hlsVideoSegmentFormat).append(", ");
		}
		if (mediaCategory != null) {
			builder.append("mediaCategory=").append(mediaCategory).append(", ");
		}
		if (metadata != null) {
			builder.append("metadata=").append(metadata).append(", ");
		}
		if (startAbsoluteTime != null) {
			builder.append("startAbsoluteTime=").append(startAbsoluteTime).append(", ");
		}
		if (streamType != null) {
			builder.append("streamType=").append(streamType).append(", ");
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

	/**
	 * Creates a new {@link MediaBuilder} using the specified parameters.
	 *
	 * @param contentId the content ID, typically the URL of the media.
	 * @param contentType the content MIME-type.
	 * @param streamType the stream type.
	 * @return The new {@link MediaBuilder}.
	 */
	public static MediaBuilder builder(
		@Nonnull String contentId,
		@Nonnull String contentType,
		@Nonnull StreamType streamType
	) {
		return new MediaBuilder(contentId, contentType, streamType);
	}

	/**
	 * A builder class for the {@link Media} class.
	 *
	 * @author Nadahar
	 */
	public static class MediaBuilder {

		/** Typically the URL of the media */
		protected final String contentId;

		/** The content MIME-type */
		protected final String contentType;

		/**
		 * Optional media URL, to allow using {@code contentId} for real ID. If
		 * {@code contentUrl} is provided, it will be used as media the URL,
		 * otherwise {@code contentId} will be used as the media URL.
		 */
		protected String contentUrl;

		/** Application-specific media information */
		protected Map<String, Object> customData;

		/** The media duration */
		protected Double duration;

		/** Optional Google Assistant deep link to a media entity */
		protected String entity;

		/** The format of the HLS audio segment */
		protected HlsSegmentFormat hlsSegmentFormat;

		/** The format of the HLS video segment */
		protected HlsVideoSegmentFormat hlsVideoSegmentFormat;

		/** The media category (audio, video, picture) */
		protected MediaCategory mediaCategory;

		/** The media metadata */
		protected Map<String, Object> metadata;

		/**
		 * Provides absolute time (Epoch Unix time in seconds) for live streams. For
		 * live event it would be the time the event started, otherwise it will be
		 * start of the seekable range when the streaming started.
		 */
		protected Long startAbsoluteTime;

		/** The stream type (required) */
		protected final StreamType streamType;

		/** The style of text track */
		protected TextTrackStyle textTrackStyle;

		/** The media tracks */
		protected List<Track> tracks;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param contentId the content ID, typically the URL of the media.
		 * @param contentType the content MIME-type.
		 * @param streamType the stream type.
		 */
		public MediaBuilder(@Nonnull String contentId, @Nonnull String contentType, @Nonnull StreamType streamType) {
			Util.requireNotBlank(contentId, "contentId");
			Util.requireNotBlank(contentType, "contentType");
			Util.requireNotNull(streamType, "streamType");
			this.contentId = contentId;
			this.contentType = contentType;
			this.streamType = streamType;
		}

		/**
		 * @return The content ID, typically the URL of the media.
		 */
		public String contentId() {
			return contentId;
		}

		/**
		 * @return The content MIME-type.
		 */
		public String contentType() {
			return contentType;
		}

		/**
		 * @return The optional media URL, to allow using {@code contentId} for
		 *         real ID. If {@code contentUrl} is provided, it will be used
		 *         as media the URL, otherwise {@code contentId} will be used as
		 *         the media URL.
		 */
		public String contentUrl() {
			return contentUrl;
		}

		/**
		 * @param contentUrl the optional media URL, to allow using
		 *            {@code contentId} for real ID. If {@code contentUrl} is
		 *            provided, it will be used as media the URL, otherwise
		 *            {@code contentId} will be used as the media URL.
		 * @return This {@link MediaBuilder}.
		 */
		public MediaBuilder contentUrl(String contentUrl) {
			this.contentUrl = contentUrl;
			return this;
		}

		/**
		 * @return The application-specific media information.
		 */
		public Map<String, Object> customData() {
			return customData;
		}

		/**
		 * @param customData the application-specific media information.
		 * @return This {@link MediaBuilder}.
		 */
		public MediaBuilder customData(Map<String, Object> customData) {
			this.customData = customData;
			return this;
		}

		/**
		 * @return The media duration.
		 */
		public Double duration() {
			return duration;
		}

		/**
		 * @param duration the media duration.
		 * @return This {@link MediaBuilder}.
		 */
		public MediaBuilder duration(Double duration) {
			this.duration = duration;
			return this;
		}

		/**
		 * @return The optional Google Assistant deep link to a media entity.
		 */
		public String entity() {
			return entity;
		}

		/**
		 * @param entity the optional Google Assistant deep link to a media
		 *            entity.
		 * @return This {@link MediaBuilder}.
		 */
		public MediaBuilder entity(String entity) {
			this.entity = entity;
			return this;
		}

		/**
		 * @return The format of the HLS audio segment.
		 */
		public HlsSegmentFormat hlsSegmentFormat() {
			return hlsSegmentFormat;
		}

		/**
		 * @param hlsSegmentFormat the format of the HLS audio segment.
		 * @return This {@link MediaBuilder}.
		 */
		public MediaBuilder hlsSegmentFormat(HlsSegmentFormat hlsSegmentFormat) {
			this.hlsSegmentFormat = hlsSegmentFormat;
			return this;
		}

		/**
		 * @return The format of the HLS video segment.
		 */
		public HlsVideoSegmentFormat hlsVideoSegmentFormat() {
			return hlsVideoSegmentFormat;
		}

		/**
		 * @param hlsVideoSegmentFormat the format of the HLS video segment.
		 * @return This {@link MediaBuilder}.
		 */
		public MediaBuilder hlsVideoSegmentFormat(HlsVideoSegmentFormat hlsVideoSegmentFormat) {
			this.hlsVideoSegmentFormat = hlsVideoSegmentFormat;
			return this;
		}

		/**
		 * @return The media category.
		 */
		public MediaCategory mediaCategory() {
			return mediaCategory;
		}

		/**
		 * @param mediaCategory the media category.
		 * @return This {@link MediaBuilder}.
		 */
		public MediaBuilder mediaCategory(MediaCategory mediaCategory) {
			this.mediaCategory = mediaCategory;
			return this;
		}

		/**
		 * @return The media metadata.
		 */
		public Map<String, Object> metadata() {
			return metadata;
		}

		/**
		 * @param metadata the media metadata.
		 * @return This {@link MediaBuilder}.
		 */
		public MediaBuilder metadata(Map<String, Object> metadata) {
			this.metadata = metadata;
			return this;
		}

		/**
		 * @return The absolute time (Epoch Unix time in seconds) for live
		 *         streams. For live event it would be the time the event
		 *         started, otherwise it will be start of the seekable range
		 *         when the streaming started.
		 */
		public Long startAbsoluteTime() {
			return startAbsoluteTime;
		}

		/**
		 * @param startAbsoluteTime the absolute time (Epoch Unix time in
		 *            seconds) for live streams. For live event it would be the
		 *            time the event started, otherwise it will be start of the
		 *            seekable range when the streaming started.
		 * @return This {@link MediaBuilder}.
		 */
		public MediaBuilder startAbsoluteTime(Long startAbsoluteTime) {
			this.startAbsoluteTime = startAbsoluteTime;
			return this;
		}

		/**
		 * @return The stream type.
		 */
		public StreamType streamType() {
			return streamType;
		}

		/**
		 * @return The style of text track.
		 */
		public TextTrackStyle textTrackStyle() {
			return textTrackStyle;
		}

		/**
		 * @param textTrackStyle the style of text track.
		 * @return This {@link MediaBuilder}.
		 */
		public MediaBuilder textTrackStyle(TextTrackStyle textTrackStyle) {
			this.textTrackStyle = textTrackStyle;
			return this;
		}

		/**
		 * @return The media {@link Track}s.
		 */
		public List<Track> tracks() {
			return tracks;
		}

		/**
		 * @param tracks the media {@link Track}s.
		 * @return This {@link MediaBuilder}.
		 */
		public MediaBuilder tracks(List<Track> tracks) {
			this.tracks = tracks;
			return this;
		}

		/**
		 * @return A new {@link Media} instance with the content from this
		 *         {@link MediaBuilder}.
		 */
		@Nonnull
		public Media build() {
			return new Media(
				contentId,
				contentType,
				contentUrl,
				customData,
				duration,
				entity,
				hlsSegmentFormat,
				hlsVideoSegmentFormat,
				mediaCategory,
				metadata,
				startAbsoluteTime,
				streamType,
				textTrackStyle,
				tracks
			);
		}
	}
}
