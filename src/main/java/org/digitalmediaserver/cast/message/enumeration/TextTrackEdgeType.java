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

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Defines the text track edge type.
 */
public enum TextTrackEdgeType {

	/** No edge is displayed around text */
	@JsonAlias("none")
	NONE,

	/** Solid outline is displayed around text */
	@JsonAlias("outline")
	OUTLINE,

	/** A fading shadow is casted around text */
	@JsonAlias("drop_shadow")
	DROP_SHADOW,

	/** Text is embossed on background */
	@JsonAlias("raised")
	RAISED,

	/** Text is debossed on background */
	@JsonAlias("depressed")
	DEPRESSED
}
