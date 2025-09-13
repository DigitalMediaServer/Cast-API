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
 * Types of media container/queue.
 */
public enum QueueType {

	/** Music album */
	ALBUM,

	/**
	 * Music playlist, such as private playlist, public playlist,
	 * auto-generated playlist, etc.
	 */
	PLAYLIST,

	/** Audiobook */
	AUDIOBOOK,

	/** Traditional radio station */
	RADIO_STATION,

	/** Podcast series */
	PODCAST_SERIES,

	/** TV Series */
	TV_SERIES,

	/** Video playlist */
	VIDEO_PLAYLIST,

	/** Live TV channel */
	LIVE_TV,

	/** Movie */
	MOVIE
}
