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
package org.digitalmediaserver.chromecast.api;

import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.digitalmediaserver.chromecast.api.StandardResponse.ErrorResponse;


/**
 * Generic error, which may happen during interaction with ChromeCast device.
 * Contains some descriptive message.
 */
public class ChromeCastException extends IOException { //TODO: (Nad) Rename

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance with the specified message.
	 *
	 * @param message the message to use.
	 */
	public ChromeCastException(@Nullable String message) {
		super(message);
	}

	/**
	 * Creates a new instance with the specified message and cause.
	 *
	 * @param message the message to use.
	 * @param cause the cause to use.
	 */
	public ChromeCastException(@Nullable String message, @Nullable Throwable cause) {
		super(message, cause);
	}

	/**
	 * A specialized {@link ChromeCastException} used to deliver an
	 * {@link ErrorResponse} if one is returned from the cast device.
	 */
	public static class ErrorResponseChromeCastException extends ChromeCastException {

		private static final long serialVersionUID = 1L;

		@Nonnull
		private final ErrorResponse errorResponse;

		/**
		 * Creates a new instance with the specified message and
		 * {@link ErrorResponse}.
		 *
		 * @param message the message to use.
		 * @param errorResponse the {@link ErrorResponse} to deliver.
		 */
		public ErrorResponseChromeCastException(@Nullable String message, @Nonnull ErrorResponse errorResponse) {
			super(message);
			Util.requireNotNull(errorResponse, "errorResponse");
			this.errorResponse = errorResponse;
		}

		/**
		 * @return The {@link ErrorResponse} that caused this
		 *         {@link ErrorResponseChromeCastException}.
		 */
		@Nonnull
		public ErrorResponse getErrorResponse() {
			return errorResponse;
		}
	}

	/**
	 * A specialized {@link ChromeCastException} used to deliver a
	 * {@link StandardResponse} if another type than what was expected is
	 * returned from the cast device.
	 */
	public static class UntypedChromeCastException extends ChromeCastException {

		private static final long serialVersionUID = 1L;

		@Nonnull
		private final StandardResponse untypedResponse;

		/**
		 * Creates a new instance with the specified message and untyped
		 * {@link StandardResponse}.
		 *
		 * @param message the message to use.
		 * @param untypedResponse the {@link StandardResponse} to deliver.
		 */
		public UntypedChromeCastException(@Nullable String message, @Nonnull StandardResponse untypedResponse) {
			super(message);
			Util.requireNotNull(untypedResponse, "untypedResponse");
			this.untypedResponse = untypedResponse;
		}

		/**
		 * @return The untyped {@link StandardResponse} that caused this
		 *         {@link UnprocessedChromeCastException}.
		 */
		@Nonnull
		public StandardResponse getUntypedResponse() {
			return untypedResponse;
		}
	}

	/**
	 * A specialized {@link ChromeCastException} used to deliver a responses
	 * that could not be deserialized.
	 */
	public static class UnprocessedChromeCastException extends ChromeCastException {

		private static final long serialVersionUID = 1L;

		@Nullable
		private final String unprocessedResponse;

		/**
		 * Creates a new instance with the specified message and unprocessed
		 * {@code JSON} string.
		 *
		 * @param message the message to use.
		 * @param unprocessedResponse the unprocessed {@code JSON} to deliver.
		 */
		public UnprocessedChromeCastException(String message, @Nullable String unprocessedResponse) {
			super(message);
			this.unprocessedResponse = unprocessedResponse;
		}

		/**
		 * @return The unprocessed {@code JSON} response.
		 */
		@Nullable
		public String getUnprocessedResponse() {
			return unprocessedResponse;
		}
	}

	/**
	 * A specialized {@link ChromeCastException} used what a
	 * {@code LAUNCH_ERROR} response is received.
	 */
	public static class LaunchErrorCastException extends ChromeCastException {

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
