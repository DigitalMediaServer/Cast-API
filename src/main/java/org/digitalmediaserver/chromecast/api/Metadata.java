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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * A utility class for working with "cast metadata" with defined constants and
 * methods for converting dates.
 *
 * @author Nadahar
 */
public class Metadata {

	private static final Logger LOGGER = LoggerFactory.getLogger(Metadata.class);

	/** An array of the supported time zone patterns */
	protected static final String[] TIME_ZONE_PATTERNS = new String[] {"Z", "+hh", "+hhmm", "+hh:mm"};

	/** The format pattern to use when converting to string */
	protected static final String DATE_TIME_PATTERN = "yyyyMMdd'T'HHmmssZ";

	/** The key used for {@link MetadataType} */
	public static final String METADATA_TYPE = "metadataType";

	/** The key used for metadata images */
	public static final String IMAGES = "images";

	/**
	 * Utility method to extract {@link Image}s from metadata stored as a
	 * {@link Map}.
	 *
	 * @param metadata the metadata to extract from.
	 * @return The resulting {@link List} of {@link Image}s.
	 */
	@Nonnull
	public static List<Image> extractImages(@Nullable Map<String, Object> metadata) {
		List<Image> result = new ArrayList<>();
		if (metadata == null || metadata.isEmpty()) {
			return result;
		}
		Object object = metadata.get(IMAGES);
		if (!(object instanceof List)) {
			return result;
		}
		List<?> untypedList = (List<?>) object;
		if (untypedList.isEmpty()) {
			return result;
		}
		if (!(untypedList.get(0) instanceof Map)) {
			return result;
		}
		@SuppressWarnings("unchecked")
		List<Map<?, ?>> list = (List<Map<?, ?>>) untypedList;
		String url;
		Integer height, width;
		for (Map<?, ?> image : list) {
			object = image.get("url");
			if (!(object instanceof String)) {
				continue;
			}
			url = (String) object;
			object = image.get("height");
			height = object instanceof Integer ?
				(Integer) object :
				object instanceof Number ?
					Integer.valueOf(((Number) object).intValue()) :
					null;
			object = image.get("width");
			width = object instanceof Integer ?
				(Integer) object :
				object instanceof Number ?
					Integer.valueOf(((Number) object).intValue()) :
					null;
			result.add(new Image(url, height, width));
		}
		return result;
	}

	/**
	 * Utility method to "set" (insert or replace) the {@link Image}s of
	 * metadata stored as a {@link Map}.
	 *
	 * @param metadata to metadata to alter.
	 * @param images the {@link Image}(s) to set.
	 * @return {@code true} if the metadata was altered, {@code false} if it
	 *         wasn't.
	 */
	public static boolean setImages(@Nullable Map<String, Object> metadata, Image... images) {
		if (metadata == null) {
			return false;
		}
		if (images == null || images.length == 0) {
			return metadata.remove(IMAGES) != null;
		}
		return setImages(metadata, Arrays.asList(images));
	}

	/**
	 * Utility method to "set" (insert or replace) the {@link Image}s of
	 * metadata stored as a {@link Map}.
	 *
	 * @param metadata to metadata to alter.
	 * @param images the {@link Collection} of {@link Image}s to set.
	 * @return {@code true} if the metadata was altered, {@code false} if it
	 *         wasn't.
	 */
	public static boolean setImages(@Nullable Map<String, Object> metadata, @Nullable Collection<Image> images) {
		if (metadata == null) {
			return false;
		}
		if (images == null || images.isEmpty()) {
			return metadata.remove(IMAGES) != null;
		}
		List<Map<String, Object>> imagesList = new ArrayList<>();
		String url;
		Integer intValue;
		Map<String, Object> imageEntry;
		for (Image image : images) {
			if (Util.isBlank(url = image.getUrl())) {
				continue;
			}
			imageEntry = new LinkedHashMap<>(3);
			imageEntry.put("url", url);
			if ((intValue = image.getHeight()) != null) {
				imageEntry.put("height", intValue);
			}
			if ((intValue = image.getWidth()) != null) {
				imageEntry.put("width", intValue);
			}
			imagesList.add(imageEntry);
		}
		if (imagesList.isEmpty()) {
			return metadata.remove(IMAGES) != null;
		}
		metadata.put(IMAGES, imagesList);
		return true;
	}

	/**
	 * The standard metadata keys defined for "Generic" media.
	 *
	 * @author Nadahar
	 */
	public static class Generic {

		/**
		 * <b>Date</b> Release date. The value is the date and/or time at which
		 * the media was released, in ISO-8601 format. For example, this could
		 * be the date that a movie or music album was released.
		 */
		public static final String RELEASE_DATE = "releaseDate";

		/**
		 * <b>String</b> Title. The title of the media. For example, this could
		 * be the title of a song, movie, or TV show episode. This value is
		 * suitable for display purposes.
		 */
		public static final String TITLE = "title";

		/**
		 * <b>String</b> Subtitle. The subtitle of the media. This value is
		 * suitable for display purposes.
		 */
		public static final String SUBTITLE = "subtitle";

		/**
		 * <b>String</b> Artist. The name of the artist who created the media.
		 * For example, this could be the name of a musician, performer, or
		 * photographer. This value is suitable for display purposes.
		 */
		public static final String ARTIST = "artist";

		/**
		 * <b>Integer</b> Queue item ID. The ID of the queue item that includes
		 * the section start time.
		 */
		public static final String QUEUE_ITEM_ID = "queueItemId";

		/**
		 * <b>Long (time in milliseconds)</b> Section duration. The section
		 * duration in milliseconds.
		 */
		public static final String SECTION_DURATION = "sectionDuration";

		/**
		 * <b>Long (time in milliseconds)</b> Section start absolute time. For
		 * live content, this field can be used to specify the absolute section
		 * start time. The value is in Epoch time in milliseconds.
		 */
		public static final String SECTION_START_ABSOLUTE_TIME = "sectionStartAbsoluteTime";

		/**
		 * <b>Long (time in milliseconds)</b> Section start time in the
		 * container. Provides section start offset within the full container.
		 * For example audiobook chapter offset within the whole book.
		 */
		public static final String SECTION_START_TIME_IN_CONTAINER = "sectionStartTimeInContainer";

		/**
		 * <b>Long (time in milliseconds)</b> Section start time in media item.
		 * Offset of the section start time from the start of the media item (as
		 * specified by {@link #QUEUE_ITEM_ID}) in milliseconds.
		 */
		public static final String SECTION_START_TIME_IN_MEDIA = "sectionStartTimeInMedia";

		/**
		 * Not to be instantiated.
		 */
		private Generic() {
		}
	}

	/**
	 * The standard metadata keys defined for "Movie" media.
	 *
	 * @author Nadahar
	 */
	public static class Movie {

		/**
		 * <b>Date</b> Release date. The value is the date and/or time at which
		 * the media was released, in ISO-8601 format. For example, this could
		 * be the date that a movie or music album was released.
		 */
		public static final String RELEASE_DATE = "releaseDate";

		/**
		 * <b>String</b> Title. The title of the media. For example, this could
		 * be the title of a song, movie, or TV show episode. This value is
		 * suitable for display purposes.
		 */
		public static final String TITLE = "title";

		/**
		 * <b>String</b> Subtitle. The subtitle of the media. This value is
		 * suitable for display purposes.
		 */
		public static final String SUBTITLE = "subtitle";

		/**
		 * <b>String</b> Studio. The name of a recording studio that produced a
		 * piece of media. For example, this could be the name of a movie studio
		 * or music label. This value is suitable for display purposes.
		 */
		public static final String STUDIO = "studio";

		/**
		 * <b>Integer</b> Queue item ID. The ID of the queue item that includes
		 * the section start time.
		 */
		public static final String QUEUE_ITEM_ID = "queueItemId";

		/**
		 * <b>Long (time in milliseconds)</b> Section duration. The section
		 * duration in milliseconds.
		 */
		public static final String SECTION_DURATION = "sectionDuration";

		/**
		 * <b>Long (time in milliseconds)</b> Section start absolute time. For
		 * live content, this field can be used to specify the absolute section
		 * start time. The value is in Epoch time in milliseconds.
		 */
		public static final String SECTION_START_ABSOLUTE_TIME = "sectionStartAbsoluteTime";

		/**
		 * <b>Long (time in milliseconds)</b> Section start time in the
		 * container. Provides section start offset within the full container.
		 * For example audiobook chapter offset within the whole book.
		 */
		public static final String SECTION_START_TIME_IN_CONTAINER = "sectionStartTimeInContainer";

		/**
		 * <b>Long (time in milliseconds)</b> Section start time in media item.
		 * Offset of the section start time from the start of the media item (as
		 * specified by {@link #QUEUE_ITEM_ID}) in milliseconds.
		 */
		public static final String SECTION_START_TIME_IN_MEDIA = "sectionStartTimeInMedia";

		/**
		 * Not to be instantiated.
		 */
		private Movie() {
		}
	}

	/**
	 * The standard metadata keys defined for "TV show" media.
	 *
	 * @author Nadahar
	 */
	public static class TvShow {

		/**
		 * <b>Date</b> Release date. The value is the date and/or time at which
		 * the media was released, in ISO-8601 format. For example, this could
		 * be the date that a movie or music album was released.
		 */
		public static final String RELEASE_DATE = "releaseDate";

		/**
		 * <b>Date</b> Broadcast date. The value is the date and/or time at
		 * which the media was first broadcast, in ISO-8601 format. For example,
		 * this could be the date that a TV show episode was first aired.
		 */
		public static final String BROADCAST_DATE = "originalAirdate";

		/**
		 * <b>String</b> Title. The title of the media. For example, this could
		 * be the title of a song, movie, or TV show episode. This value is
		 * suitable for display purposes.
		 */
		public static final String TITLE = "title";

		/**
		 * <b>Integer</b> Season number. The season number that a TV show
		 * episode belongs to. Typically season numbers are counted starting
		 * from 1, however this value may be 0 if it is a "pilot" episode that
		 * predates the official start of a TV series.
		 */
		public static final String SEASON_NUMBER = "season";

		/**
		 * <b>Integer</b> Episode number. The number of an episode in a given
		 * season of a TV show. Typically episode numbers are counted starting
		 * from 1, however this value may be 0 if it is a "pilot" episode that
		 * is not considered to be an official episode of the first season.
		 */
		public static final String EPISODE_NUMBER = "episode";

		/**
		 * <b>String</b> Series title. The name of a series. For example, this
		 * could be the name of a TV show or series of related music albums.
		 * This value is suitable for display purposes.
		 */
		public static final String SERIES_TITLE = "seriesTitle";

		/**
		 * <b>Integer</b> Queue item ID. The ID of the queue item that includes
		 * the section start time.
		 */
		public static final String QUEUE_ITEM_ID = "queueItemId";

		/**
		 * <b>Long (time in milliseconds)</b> Section duration. The section
		 * duration in milliseconds.
		 */
		public static final String SECTION_DURATION = "sectionDuration";

		/**
		 * <b>Long (time in milliseconds)</b> Section start absolute time. For
		 * live content, this field can be used to specify the absolute section
		 * start time. The value is in Epoch time in milliseconds.
		 */
		public static final String SECTION_START_ABSOLUTE_TIME = "sectionStartAbsoluteTime";

		/**
		 * <b>Long (time in milliseconds)</b> Section start time in the
		 * container. Provides section start offset within the full container.
		 * For example audiobook chapter offset within the whole book.
		 */
		public static final String SECTION_START_TIME_IN_CONTAINER = "sectionStartTimeInContainer";

		/**
		 * <b>Long (time in milliseconds)</b> Section start time in media item.
		 * Offset of the section start time from the start of the media item (as
		 * specified by {@link #QUEUE_ITEM_ID}) in milliseconds.
		 */
		public static final String SECTION_START_TIME_IN_MEDIA = "sectionStartTimeInMedia";

		/**
		 * Not to be instantiated.
		 */
		private TvShow() {
		}
	}

	/**
	 * The standard metadata keys defined for "Music track" media.
	 *
	 * @author Nadahar
	 */
	public static class MusicTrack {

		/**
		 * <b>Date</b> Release date. The value is the date and/or time at which
		 * the media was released, in ISO-8601 format. For example, this could
		 * be the date that a movie or music album was released.
		 */
		public static final String RELEASE_DATE = "releaseDate";

		/**
		 * <b>String</b> Title. The title of the media. For example, this could
		 * be the title of a song, movie, or TV show episode. This value is
		 * suitable for display purposes.
		 */
		public static final String TITLE = "title";

		/**
		 * <b>String</b> Artist. The name of the artist who created the media.
		 * For example, this could be the name of a musician, performer, or
		 * photographer. This value is suitable for display purposes.
		 */
		public static final String ARTIST = "artist";

		/**
		 * <b>String</b> Album artist. The name of the artist who produced an
		 * album. For example, in compilation albums such as DJ mixes, the album
		 * artist is not necessarily the same as the artist(s) of the individual
		 * songs on the album. This value is suitable for display purposes.
		 */
		public static final String ALBUM_ARTIST = "albumArtist";

		/**
		 * <b>String</b> Album title. The title of the album that a music track
		 * belongs to. This value is suitable for display purposes.
		 */
		public static final String ALBUM_TITLE = "albumName";

		/**
		 * <b>String</b> Composer. The name of the composer of a music track.
		 * This value is suitable for display purposes.
		 */
		public static final String COMPOSER = "composer";

		/**
		 * <b>Integer</b> Disc number. The disc number (counting from 1) that a
		 * music track belongs to in a multi-disc album.
		 */
		public static final String DISC_NUMBER = "discNumber";

		/**
		 * <b>Integer</b> Track number. The track number of a music track on an
		 * album disc. Typically track numbers are counted starting from 1,
		 * however this value may be 0 if it is a "hidden track" at the
		 * beginning of an album.
		 */
		public static final String TRACK_NUMBER = "trackNumber";

		/**
		 * <b>Integer</b> Queue item ID. The ID of the queue item that includes
		 * the section start time.
		 */
		public static final String QUEUE_ITEM_ID = "queueItemId";

		/**
		 * <b>Long (time in milliseconds)</b> Section duration. The section
		 * duration in milliseconds.
		 */
		public static final String SECTION_DURATION = "sectionDuration";

		/**
		 * <b>Long (time in milliseconds)</b> Section start absolute time. For
		 * live content, this field can be used to specify the absolute section
		 * start time. The value is in Epoch time in milliseconds.
		 */
		public static final String SECTION_START_ABSOLUTE_TIME = "sectionStartAbsoluteTime";

		/**
		 * <b>Long (time in milliseconds)</b> Section start time in the
		 * container. Provides section start offset within the full container.
		 * For example audiobook chapter offset within the whole book.
		 */
		public static final String SECTION_START_TIME_IN_CONTAINER = "sectionStartTimeInContainer";

		/**
		 * <b>Long (time in milliseconds)</b> Section start time in media item.
		 * Offset of the section start time from the start of the media item (as
		 * specified by {@link #QUEUE_ITEM_ID}) in milliseconds.
		 */
		public static final String SECTION_START_TIME_IN_MEDIA = "sectionStartTimeInMedia";

		/**
		 * Not to be instantiated.
		 */
		private MusicTrack() {
		}
	}

	/**
	 * The standard metadata keys defined for "Photo" media.
	 *
	 * @author Nadahar
	 */
	public static class Photo {

		/**
		 * <b>Date</b> Creation date. The value is the date and/or time at which
		 * the media was created, in ISO-8601 format. For example, this could be
		 * the date and time at which a photograph was taken or a piece of music
		 * was recorded.
		 */
		public static final String CREATION_DATE = "creationDateTime";

		/**
		 * <b>String</b> Title. The title of the media. For example, this could
		 * be the title of a song, movie, or TV show episode. This value is
		 * suitable for display purposes.
		 */
		public static final String TITLE = "title";

		/**
		 * <b>String</b> Artist. The name of the artist who created the media.
		 * For example, this could be the name of a musician, performer, or
		 * photographer. This value is suitable for display purposes.
		 */
		public static final String ARTIST = "artist";

		/**
		 * <b>Integer</b> Width. The width of a piece of media, in pixels. This
		 * would typically be used for providing the dimensions of a photograph.
		 */
		public static final String WIDTH = "width";

		/**
		 * <b>Integer</b> Height. The height of a piece of media, in pixels.
		 * This would typically be used for providing the dimensions of a
		 * photograph.
		 */
		public static final String HEIGHT = "height";

		/**
		 * <b>String</b> Location name. The name of a location where a piece of
		 * media was created. For example, this could be the location of a
		 * photograph or the principal filming location of a movie. This value
		 * is suitable for display purposes.
		 */
		public static final String LOCATION_NAME = "location";

		/**
		 * <b>Double</b> Location latitude. The latitude component of the
		 * geographical location where a piece of media was created. For
		 * example, this could be the location of a photograph or the principal
		 * filming location of a movie.
		 */
		public static final String LOCATION_LATITUDE = "latitude";

		/**
		 * <b>Double</b> Location longitude. The longitude component of the
		 * geographical location where a piece of media was created. For
		 * example, this could be the location of a photograph or the principal
		 * filming location of a movie.
		 */
		public static final String LOCATION_LONGITUDE = "longitude";

		/**
		 * <b>Integer</b> Queue item ID. The ID of the queue item that includes
		 * the section start time.
		 */
		public static final String QUEUE_ITEM_ID = "queueItemId";

		/**
		 * <b>Long (time in milliseconds)</b> Section duration. The section
		 * duration in milliseconds.
		 */
		public static final String SECTION_DURATION = "sectionDuration";

		/**
		 * <b>Long (time in milliseconds)</b> Section start absolute time. For
		 * live content, this field can be used to specify the absolute section
		 * start time. The value is in Epoch time in milliseconds.
		 */
		public static final String SECTION_START_ABSOLUTE_TIME = "sectionStartAbsoluteTime";

		/**
		 * <b>Long (time in milliseconds)</b> Section start time in the
		 * container. Provides section start offset within the full container.
		 * For example audiobook chapter offset within the whole book.
		 */
		public static final String SECTION_START_TIME_IN_CONTAINER = "sectionStartTimeInContainer";

		/**
		 * <b>Long (time in milliseconds)</b> Section start time in media item.
		 * Offset of the section start time from the start of the media item (as
		 * specified by {@link #QUEUE_ITEM_ID}) in milliseconds.
		 */
		public static final String SECTION_START_TIME_IN_MEDIA = "sectionStartTimeInMedia";

		/**
		 * Not to be instantiated.
		 */
		private Photo() {
		}
	}

	/**
	 * The standard metadata keys defined for "Audiobook chapter" media.
	 *
	 * @author Nadahar
	 */
	public static class AudiobookChapter {

		/**
		 * <b>String</b> Title. The title of the media. For example, this could
		 * be the title of a song, movie, or TV show episode. This value is
		 * suitable for display purposes.
		 */
		public static final String TITLE = "title";

		/**
		 * <b>String</b> Subtitle. The subtitle of the media. This value is
		 * suitable for display purposes.
		 */
		public static final String SUBTITLE = "subtitle";

		/**
		 * <b>String</b> Audiobook title. The title of the audiobook.
		 */
		public static final String BOOK_TITLE = "bookTitle";

		/**
		 * <b>String</b> Chapter title. The title of the chapter of the
		 * audiobook.
		 */
		public static final String CHAPTER_TITLE = "chapterTitle";

		/**
		 * <b>Integer</b> Chapter number. The chapter number of the audiobook.
		 */
		public static final String CHAPTER_NUMBER = "chapterNumber";

		/**
		 * <b>Integer</b> Queue item ID. The ID of the queue item that includes
		 * the section start time.
		 */
		public static final String QUEUE_ITEM_ID = "queueItemId";

		/**
		 * <b>Long (time in milliseconds)</b> Section duration. The section
		 * duration in milliseconds.
		 */
		public static final String SECTION_DURATION = "sectionDuration";

		/**
		 * <b>Long (time in milliseconds)</b> Section start absolute time. For
		 * live content, this field can be used to specify the absolute section
		 * start time. The value is in Epoch time in milliseconds.
		 */
		public static final String SECTION_START_ABSOLUTE_TIME = "sectionStartAbsoluteTime";

		/**
		 * <b>Long (time in milliseconds)</b> Section start time in the
		 * container. Provides section start offset within the full container.
		 * For example audiobook chapter offset within the whole book.
		 */
		public static final String SECTION_START_TIME_IN_CONTAINER = "sectionStartTimeInContainer";

		/**
		 * <b>Long (time in milliseconds)</b> Section start time in media item.
		 * Offset of the section start time from the start of the media item (as
		 * specified by {@link #QUEUE_ITEM_ID}) in milliseconds.
		 */
		public static final String SECTION_START_TIME_IN_MEDIA = "sectionStartTimeInMedia";

		/**
		 * Not to be instantiated.
		 */
		private AudiobookChapter() {
		}
	}

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

	/**
	 * Not to be instantiated.
	 */
	private Metadata() {
	}

	/**
	 * Converts a {@link Calendar} instance to a "metadata date" formatted
	 * string.
	 *
	 * @param calendar the {@link Calendar} instance to convert.
	 * @return The resulting string or {@code null} if {@code calendar} is
	 *         {@code null}.
	 */
	@Nullable
	public static String dateToString(Calendar calendar) {
		if (calendar == null) {
			LOGGER.debug("Calendar object cannot be null");
			return null;
		}
		String pattern = DATE_TIME_PATTERN;
		if (calendar.get(11) == 0 && calendar.get(12) == 0 && calendar.get(13) == 0) {
			pattern = "yyyyMMdd";
		}

		SimpleDateFormat format = new SimpleDateFormat(pattern);
		format.setTimeZone(calendar.getTimeZone());
		String result = format.format(calendar.getTime());
		if (result.endsWith("+0000")) {
			result = result.replace("+0000", TIME_ZONE_PATTERNS[0]);
		}

		return result;
	}

	/**
	 * Attempts to parse a date and time string formatted as a "metadata date"
	 * to a {@link Calendar} instance.
	 *
	 * @param dateTimeString the date and time string to parse.
	 * @return The resulting {@link Calendar} or {@code null}.
	 */
	@Nullable
	public static Calendar parseDate(String dateTimeString) {
		if (Util.isBlank(dateTimeString)) {
			LOGGER.debug("dateTimeString is empty or null");
			return null;
		}
		String dateString = extractDate(dateTimeString);
		if (Util.isBlank(dateString)) {
			LOGGER.debug("Invalid date format");
			return null;
		}
		String timeString = extractTime(dateTimeString);
		String pattern = "yyyyMMdd";
		if (!Util.isBlank(timeString)) {
			dateString = new StringBuilder(1 + dateString.length() + timeString.length())
				.append(dateString)
				.append("T")
				.append(timeString).toString();
			if (timeString.length() == 6) {
				pattern = "yyyyMMdd'T'HHmmss";
			} else {
				pattern = DATE_TIME_PATTERN;
			}
		}

		Calendar result = GregorianCalendar.getInstance();
		Date tmpDate;
		try {
			tmpDate = new SimpleDateFormat(pattern).parse(dateString);
		} catch (ParseException e) {
			LOGGER.debug("Error parsing string: {}", e.getMessage());
			LOGGER.trace("", e);
			return null;
		}

		result.setTime(tmpDate);
		return result;
	}

	/**
	 * Extracts the "date part" of the date and time string.
	 *
	 * @param dateTimeString the date and time string to extract from.
	 * @return The "date part" string or {@code null}.
	 */
	@Nullable
	protected static String extractDate(String dateTimeString) {
		if (Util.isBlank(dateTimeString)) {
			LOGGER.debug("dateTimeString is empty or null");
			return null;
		}
		try {
			return dateTimeString.substring(0, 8);
		} catch (IndexOutOfBoundsException e) {
			LOGGER.info("Error extracting the date: {}", e.getMessage());
			LOGGER.trace("", e);
			return null;
		}
	}

	/**
	 * Extracts the "time part" of the date and time string.
	 *
	 * @param dateTimeString the date and time string to extract from.
	 * @return The "time part" string or {@code null}.
	 */
	@Nullable
	protected static String extractTime(@Nullable String dateTimeString) {
		if (Util.isBlank(dateTimeString)) {
			LOGGER.debug("dateTimeString is empty or null");
			return null;
		}
		int delimiter = dateTimeString.indexOf(84);
		if (delimiter != 8) {
			LOGGER.debug("T delimeter is not found");
			return null;
		} else {
			String timeString;
			try {
				timeString = dateTimeString.substring(delimiter + 1);
			} catch (IndexOutOfBoundsException e) {
				LOGGER.debug("Error extracting the time substring: {}", e.getMessage());
				LOGGER.trace("", e);
				return null;
			}

			if (timeString.length() == 6) {
				return timeString;
			} else {
				switch (timeString.charAt(6)) {
					case '+':
					case '-':
						int len = timeString.length();
						if (len == 6 + TIME_ZONE_PATTERNS[1].length()) {
							return timeString.concat("00");
						}
						if (len == 6 + TIME_ZONE_PATTERNS[2].length()) {
							return timeString;
						}
						if (len == 6 + TIME_ZONE_PATTERNS[3].length()) {
							return timeString.replaceAll("([\\+\\-]\\d\\d):(\\d\\d)", "$1$2");
						}
					case 'Z':
						if (timeString.length() == TIME_ZONE_PATTERNS[0].length() + 6) {
							return timeString.substring(0, timeString.length() - 1).concat("+0000");
						}
						return null;
					default:
						return null;
				}
			}
		}
	}
}
