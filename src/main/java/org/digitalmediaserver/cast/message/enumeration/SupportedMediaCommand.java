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
package org.digitalmediaserver.cast.message.enumeration;

import java.util.EnumSet;
import javax.annotation.Nonnull;


/**
 * Represents the {@code kSupportedMediaCommandXxx} constants, and offers a
 * static method to convert a given combined value into a set of
 * {@link SupportedMediaCommand}s.
 */
public enum SupportedMediaCommand {

	/** {@code kSupportedMediaCommandPause} */
	PAUSE(1 << 0),

	/** {@code kSupportedMediaCommandSeek} */
	SEEK(1 << 1),

	/** {@code kSupportedMediaCommandStreamVolume} */
	STREAM_VOLUME(1 << 2),

	/** {@code kSupportedMediaCommandStreamMute} */
	STREAM_MUTE(1 << 3),

	/** {@code kSupportedMediaCommandSkipForward} */
	SKIP_FORWARD(1 << 4),

	/** {@code kSupportedMediaCommandSkipBackward} */
	SKIP_BACKWARD(1 << 5),

	/** {@code kSupportedMediaCommandQueueNext} */
	QUEUE_NEXT(1 << 6),

	/** {@code kSupportedMediaCommandQueuePrev} */
	QUEUE_PREV(1 << 7),

	/** {@code kSupportedMediaCommandQueueShuffle} */
	QUEUE_SHUFFLE(1 << 8),

	/** {@code kSupportedMediaCommandSkipAd} */
	SKIP_AD(1 << 9),

	/** {@code kSupportedMediaCommandQueueRepeatAll} */
	QUEUE_REPEAT_ALL(1 << 10),

	/** {@code kSupportedMediaCommandQueueRepeatOne} */
	QUEUE_REPEAT_ONE(1 << 11),

	/** {@code kSupportedMediaCommandEditTracks} */
	EDIT_TRACKS(1 << 12),

	/** {@code kSupportedMediaCommandPlaybackRate} */
	PLAYBACK_RATE(1 << 13),

	/** {@code kSupportedMediaCommandLike} */
	LIKE(1 << 14),

	/** {@code kSupportedMediaCommandDislike} */
	DISLIKE(1 << 15),

	/** {@code kSupportedMediaCommandFollow} */
	FOLLOW(1 << 16),

	/** {@code kSupportedMediaCommandUnfollow} */
	UNFOLLOW(1 << 17),

	/** {@code kSupportedMediaCommandStreamTransfer} */
	STREAM_TRANSFER(1 << 18);

	private int code;

	private SupportedMediaCommand(int code) {
		this.code = code;
	}

	/**
	 * @return The integer value assigned to this {@link SupportedMediaCommand}.
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Parses a combined integer value into its individual components and
	 * returns a set of corresponding {@link SupportedMediaCommand}.
	 *
	 * @param value the combined value.
	 * @return The {@link EnumSet} of {@link SupportedMediaCommand}s.
	 */
	@Nonnull
	public static EnumSet<SupportedMediaCommand> parseCommands(int value) {
		EnumSet<SupportedMediaCommand> result = EnumSet.noneOf(SupportedMediaCommand.class);
		for (SupportedMediaCommand command : values()) {
			if ((value & command.code) != 0) {
				result.add(command);
			}
		}
		return result;
	}
}
