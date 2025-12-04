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
package org.digitalmediaserver.cast.util;

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
import org.digitalmediaserver.cast.message.entity.Image;
import org.digitalmediaserver.cast.message.entity.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A utility class for working with "cast metadata" with methods for e.g.
 * handling images or converting dates.
 *
 * @author Nadahar
 */
public class MetadataUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataUtil.class);

	/** An array of the supported time zone patterns */
	protected static final String[] TIME_ZONE_PATTERNS = new String[] {"Z", "+hh", "+hhmm", "+hh:mm"};

	/** The format pattern to use when converting to string */
	protected static final String DATE_TIME_PATTERN = "yyyyMMdd'T'HHmmssZ";

	/**
	 * Not to be instantiated.
	 */
	private MetadataUtil() {
	}

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
		Object object = metadata.get(Metadata.IMAGES);
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
			return metadata.remove(Metadata.IMAGES) != null;
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
			return metadata.remove(Metadata.IMAGES) != null;
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
			return metadata.remove(Metadata.IMAGES) != null;
		}
		metadata.put(Metadata.IMAGES, imagesList);
		return true;
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
