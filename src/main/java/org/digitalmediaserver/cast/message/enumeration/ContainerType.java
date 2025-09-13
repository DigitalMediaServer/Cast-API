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
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * Possible types of container metadata.
 */
public enum ContainerType {

	/**
	 * Generic template suitable for most media types. Used by
	 * {@link ContainerMetadata}.
	 */
	GENERIC_CONTAINER(0),

	/**
	 * Metadata for an audiobook. Used by
	 * {@code AudiobookContainerMetadata}.
	 */
	AUDIOBOOK_CONTAINER(1);

	private int value;

	private ContainerType(int value) {
		this.value = value;
	}

	/**
	 * @return The integer value of this container type.
	 */
	@JsonValue
	public int getValue() {
		return value;
	}

	/**
	 * Returns the {@link ContainerType} that corresponds to the specified
	 * integer value, or {@code null} if nothing corresponds.
	 *
	 * @param value the integer value whose corresponding
	 *            {@link ContainerType} to find.
	 * @return The {@link ContainerType} or {@code null}.
	 */
	@JsonCreator
	@Nullable
	public static ContainerType typeOf(int value) {
		switch (value) {
			case 0:
				return GENERIC_CONTAINER;
			case 1:
				return AUDIOBOOK_CONTAINER;
			default:
				return null;
		}
	}
}
