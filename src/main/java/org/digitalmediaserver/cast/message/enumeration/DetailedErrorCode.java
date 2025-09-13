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
package org.digitalmediaserver.cast.message.enumeration;

import javax.annotation.Nullable;
import com.fasterxml.jackson.annotation.JsonCreator;


/**
 * Represents detailed error codes.
 */
public enum DetailedErrorCode {

	/**
	 * Returned when the HTMLMediaElement throws an error, but the specified
	 * error isn't recognized
	 */
	MEDIA_UNKNOWN(100),

	/**
	 * Returned when the fetching process for the media resource was aborted
	 * by the user agent at the user's request
	 */
	MEDIA_ABORTED(101),

	/**
	 * Returned when an error occurred while decoding the media resource,
	 * after the resource was established to be usable
	 */
	MEDIA_DECODE(102),

	/**
	 * Returned when a network error caused the user agent to stop fetching
	 * the media resource, after the resource was established to be usable
	 */
	MEDIA_NETWORK(103),

	/**
	 * Returned when the media resource indicated by the src attribute was
	 * not suitable
	 */
	MEDIA_SRC_NOT_SUPPORTED(104),

	/** Returned when a source buffer cannot be added to the MediaSource */
	SOURCE_BUFFER_FAILURE(110),

	/** Returned when there is an unknown error with media keys */
	MEDIAKEYS_UNKNOWN(200),

	/** Returned when there is a media keys failure due to a network issue */
	MEDIAKEYS_NETWORK(201),

	/** Returned when a MediaKeySession object cannot be created */
	MEDIAKEYS_UNSUPPORTED(202),

	/** Returned when crypto failed */
	MEDIAKEYS_WEBCRYPTO(203),

	/** Returned when there was an unknown network issue */
	NETWORK_UNKNOWN(300),

	/** Returned when a segment fails to download */
	SEGMENT_NETWORK(301),

	/** Returned when an HLS master playlist fails to download */
	HLS_NETWORK_MASTER_PLAYLIST(311),

	/** Returned when an HLS playlist fails to download */
	HLS_NETWORK_PLAYLIST(312),

	/** Returned when an HLS key fails to download */
	HLS_NETWORK_NO_KEY_RESPONSE(313),

	/** Returned when a request for an HLS key fails before it is sent */
	HLS_NETWORK_KEY_LOAD(314),

	/** Returned when an HLS segment is invalid */
	HLS_NETWORK_INVALID_SEGMENT(315),

	/** Returned when an HLS segment fails to parse */
	HLS_SEGMENT_PARSING(316),

	/**
	 * Returned when an unknown network error occurs while handling a DASH
	 * stream
	 */
	DASH_NETWORK(321),

	/** Returned when a DASH stream is missing an init */
	DASH_NO_INIT(322),

	/**
	 * Returned when an unknown network error occurs while handling a Smooth
	 * stream
	 */
	SMOOTH_NETWORK(331),

	/** Returned when a Smooth stream is missing media data */
	SMOOTH_NO_MEDIA_DATA(332),

	/** Returned when an unknown error occurs while parsing a manifest */
	MANIFEST_UNKNOWN(400),

	/**
	 * Returned when an error occurs while parsing an HLS master manifest
	 */
	HLS_MANIFEST_MASTER(411),

	/** Returned when an error occurs while parsing an HLS playlist */
	HLS_MANIFEST_PLAYLIST(412),

	/**
	 * Returned when an unknown error occurs while parsing a DASH manifest
	 */
	DASH_MANIFEST_UNKNOWN(420),

	/** Returned when a DASH manifest is missing periods */
	DASH_MANIFEST_NO_PERIODS(421),

	/** Returned when a DASH manifest is missing a MimeType */
	DASH_MANIFEST_NO_MIMETYPE(422),

	/** Returned when a DASH manifest contains invalid segment info */
	DASH_INVALID_SEGMENT_INFO(423),

	/** Returned when an error occurs while parsing a Smooth manifest */
	SMOOTH_MANIFEST(431),

	/** Returned when an unknown segment error occurs */
	SEGMENT_UNKNOWN(500),

	/** An unknown error occurred with a text stream */
	TEXT_UNKNOWN(600),

	/**
	 * Returned when an error occurs outside of the framework (e.g., if an
	 * event handler throws an error)
	 */
	APP(900),

	/** Returned when break clip load interceptor fails */
	BREAK_CLIP_LOADING_ERROR(901),

	/** Returned when break seek interceptor fails */
	BREAK_SEEK_INTERCEPTOR_ERROR(902),

	/** Returned when an image fails to load */
	IMAGE_ERROR(903),

	/** A load was interrupted by an unload, or by another load */
	LOAD_INTERRUPTED(904),

	/** A load command failed */
	LOAD_FAILED(905),

	/** An error message was sent to the sender */
	MEDIA_ERROR_MESSAGE(906),

	/** Returned when an unknown error occurs */
	GENERIC(999);

	private final int code;

	private DetailedErrorCode(int code) {
		this.code = code;
	}

	/**
	 * @return The numerical code of this {@link DetailedErrorCode}.
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Returns the {@link DetailedErrorCode} that corresponds to the
	 * specified integer code, or {@code null} if nothing corresponds.
	 *
	 * @param code the integer code whose corresponding
	 *            {@link DetailedErrorCode} to find.
	 * @return The {@link DetailedErrorCode} or {@code null}.
	 */
	@JsonCreator
	@Nullable
	public static DetailedErrorCode typeOf(int code) {
		for (DetailedErrorCode errorCode : values()) {
			if (errorCode.code == code) {
				return errorCode;
			}
		}
		return null;
	}
}
