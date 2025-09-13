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
 * Represents media error message types.
 */
public enum ErrorType {

	/** Returned when the player state is invalid to fulfill the request */
	INVALID_PLAYER_STATE,

	/** Returned when the LOAD request failed */
	LOAD_FAILED,

	/**
	 * Returned when the LOAD request is cancelled by a second incoming LOAD
	 * request
	 */
	LOAD_CANCELLED,

	/** Returned when the request is not valid */
	INVALID_REQUEST,

	/** Generic error, for any other error case */
	ERROR;

	/**
	 * Parses the specified string and returns the corresponding
	 * {@link ErrorType}, or {@code null} if no match could be found.
	 *
	 * @param errorType the string to parse.
	 * @return The resulting {@link ErrorType} or {@code null}.
	 */
	@Nullable
	@JsonCreator
	public static ErrorType typeOf(String errorType) {
		if (Util.isBlank(errorType)) {
			return null;
		}
		String typeString = errorType.toUpperCase(Locale.ROOT);
		for (ErrorType type : values()) {
			if (typeString.equals(type.name())) {
				return type;
			}
		}
		return null;
	}
}
