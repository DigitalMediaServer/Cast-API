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
package org.digitalmediaserver.cast.message.request;

import org.digitalmediaserver.cast.message.entity.Volume;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A request to set the volume level or the mute state of the receiver.
 */
public class SetVolume extends StandardRequest {

	/** The {@link Volume} instance */
	@JsonProperty
	protected final Volume volume;

	/**
	 * Creates a new request using the specified parameters.
	 *
	 * @param volume the new volume of the cast device. At least one of
	 *            level or muted must be set.
	 */
	public SetVolume(Volume volume) {
		this.volume = volume;
	}

	/**
	 * @return The new volume of the cast device. At least one of level or
	 *         muted must be set.
	 */
	public Volume getVolume() {
		return volume;
	}
}
