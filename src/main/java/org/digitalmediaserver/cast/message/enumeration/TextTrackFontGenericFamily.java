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


/**
 * Generic font family to be used if font not defined in text track.
 */
public enum TextTrackFontGenericFamily {

	/** Uses Sans Serif font */
	SANS_SERIF,

	/** Uses Sans Monospaced font */
	MONOSPACED_SANS_SERIF,

	/** Uses Serif font */
	SERIF,

	/** Uses Monospaced Serif font */
	MONOSPACED_SERIF,

	/** Uses Short Stack font */
	CASUAL,

	/** Uses Cursive font */
	CURSIVE,

	/** Uses Small Capitals font */
	SMALL_CAPITALS
}
