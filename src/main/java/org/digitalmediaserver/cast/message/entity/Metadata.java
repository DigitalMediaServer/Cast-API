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
package org.digitalmediaserver.cast.message.entity;


/**
 * A collection of constants for "cast metadata".
 *
 * @author Nadahar
 */
public class Metadata {

	/** The key used for {@link MetadataType} */
	public static final String METADATA_TYPE = "metadataType";

	/** The key used for metadata images */
	public static final String IMAGES = "images";

	/**
	 * Not to be instantiated.
	 */
	private Metadata() {
	}

	/**
	 * The standard metadata keys defined for "Generic" media.
	 *
	 * @author Nadahar
	 */
	public static class Generic extends Metadata {

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
	public static class Movie extends Metadata {

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
	public static class TvShow extends Metadata {

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
	public static class MusicTrack extends Metadata {

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
	public static class Photo extends Metadata {

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
	public static class AudiobookChapter extends Metadata {

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
}
