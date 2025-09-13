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

import java.util.Locale;
import javax.annotation.Nullable;
import org.digitalmediaserver.cast.util.Util;
import com.fasterxml.jackson.annotation.JsonCreator;


/**
 * Represents media error message reasons.
 */
public enum ErrorReason {

	/** Returned when the command is not valid or not implemented */
	INVALID_COMMAND,

	/**
	 * Returned when the params are not valid or a non optional param is
	 * missing
	 */
	INVALID_PARAMS,

	/** Returned when the media session does not exist */
	INVALID_MEDIA_SESSION_ID,

	/** Returned when cannot skip more items due to reaching skip limit */
	SKIP_LIMIT_REACHED,

	/** Returned when the request is not supported by the application */
	NOT_SUPPORTED,

	/** Returned when the requested language is not supported */
	LANGUAGE_NOT_SUPPORTED,

	/**
	 * Returned when skip is not possible due to going back beyond the first
	 * item or forward beyond the last item in the queue
	 */
	END_OF_QUEUE,

	/**
	 * Returned when the request ID is not unique (the receiver is
	 * processing a request with the same ID)
	 */
	DUPLICATE_REQUEST_ID,

	/**
	 * Returned when the request cannot be completed because a video-capable
	 * device is required
	 */
	VIDEO_DEVICE_REQUIRED,

	/**
	 * Returned when premium account is required for the request to succeed
	 */
	PREMIUM_ACCOUNT_REQUIRED,

	/** Returned when the application state is invalid to fulfill the request */
	APP_ERROR,

	/**
	 * Returned when a request cannot be performed because authentication
	 * has expired, e.g. user changed password or the token was revoked
	 */
	AUTHENTICATION_EXPIRED,

	/** Returned when too many concurrent streams are detected */
	CONCURRENT_STREAM_LIMIT,

	/** Returned when the content is blocked due to parental controls */
	PARENTAL_CONTROL_RESTRICTED,

	/** Returned when the content is blocked due to filter */
	CONTENT_FILTERED,

	/**
	 * Returned when the content is blocked due to being regionally
	 * unavailable
	 */
	NOT_AVAILABLE_IN_REGION,

	/** Returned when the requested content is already playing */
	CONTENT_ALREADY_PLAYING,

	/** Returned when the request is not valid */
	INVALID_REQUEST,

	/** Returned when the load request encounter intermittent issue */
	GENERIC_LOAD_ERROR;

	/**
	 * Parses the specified string and returns the corresponding
	 * {@link ErrorReason}, or {@code null} if no match could be found.
	 *
	 * @param errorReason the string to parse.
	 * @return The resulting {@link ErrorReason} or {@code null}.
	 */
	@Nullable
	@JsonCreator
	public static ErrorReason typeOf(String errorReason) {
		if (Util.isBlank(errorReason)) {
			return null;
		}
		String typeString = errorReason.toUpperCase(Locale.ROOT);
		for (ErrorReason type : values()) {
			if (typeString.equals(type.name())) {
				return type;
			}
		}
		return null;
	}
}
