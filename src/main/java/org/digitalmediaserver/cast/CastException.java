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
package org.digitalmediaserver.cast;

import java.io.IOException;
import javax.annotation.Nullable;


/**
 * Generic error, which may happen during interaction with a cast device.
 * Contains some descriptive message.
 */
public class CastException extends IOException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance with the specified message.
	 *
	 * @param message the message to use.
	 */
	public CastException(@Nullable String message) {
		super(message);
	}

	/**
	 * Creates a new instance with the specified message and cause.
	 *
	 * @param message the message to use.
	 * @param cause the cause to use.
	 */
	public CastException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}

	public static class LoadCancelledCastException extends CastException {

		private static final long serialVersionUID = 1L;

		@Nullable
		private final Integer itemId;

		public LoadCancelledCastException(String message, @Nullable Integer itemId) {
			super(message);
			this.itemId = itemId;
		}

		@Nullable
		public Integer getItemId() {
			return itemId;
		}
	}

	public static class LoadFailedCastException extends CastException {

		private static final long serialVersionUID = 1L;

		public LoadFailedCastException(String message) {
			super(message);
		}
	}

	public static class InvalidCastException extends CastException {

		private static final long serialVersionUID = 1L;

		public InvalidCastException(String message) {
			super(message);
		}
	}

	/**
	 * A specialized {@link CastException} used when a {@code LAUNCH_ERROR}
	 * response is received.
	 */
	public static class LaunchErrorCastException extends CastException {

		private static final long serialVersionUID = 1L;

		/**
		 * Creates a new instance using the specified message.
		 *
		 * @param message the message to use.
		 */
		public LaunchErrorCastException(String message) {
			super(message);
		}
	}
}
