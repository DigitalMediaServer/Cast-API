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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Contains utility methods.
 */
final class Util {

	private Util() {
	}

	/**
	 * Converts specified byte array in Big Endian to int.
	 */
	public static int intFromB32Bytes(byte[] payload) {
		return payload[0] << 24 | (payload[1] & 0xFF) << 16 | (payload[2] & 0xFF) << 8 | (payload[3] & 0xFF);
	}

	/**
	 * Converts specified int to byte array in Big Endian.
	 */
	public static byte[] intToB32Bytes(int value) {
		return new byte[] {(byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value};
	}

	/**
	 * Writes the specified {@code int} as a 32-bit big endian integer to the
	 * specified {@link OutputStream}.
	 *
	 * @param value the {@code int} value to write.
	 * @param os the {@link OutputStream} to write to.
	 * @return The number of bytes written.
	 * @throws IOException If an I/O error occurs during the operation.
	 */
	public static int writeB32Int(int value, @Nonnull OutputStream os) throws IOException {
		os.write(value >> 24);
		os.write(value >> 16);
		os.write(value >> 8);
		os.write(value);
		return 4;
	}

	/**
	 * Reads a 32-bit big endian integer from the specified {@link InputStream}.
	 *
	 * @param is the {@link InputStream} to read from.
	 * @return The resulting {@code int} value.
	 * @throws IOException If fewer than 4 bytes are available from {@code is}
	 *             or if some other I/O error occurs during the operation.
	 */
	public static int readB32Int(@Nonnull InputStream is) throws IOException {
		int result = 0;
		int read;
		for (int i = 24; i >= 0; i -= 8) {
			read = is.read();
			if (read < 0) {
				throw new IOException("Incorrect message length (" + (3 - i / 8) + " bytes)");
			}
			result |= (read & 0xff) << i;
		}
		return result;
	}

	public static String getMediaTitle(String url) { //TODO: (Nad) Look into
		try {
			URL urlObj = new URL(url);
			String mediaTitle;
			String path = urlObj.getPath();
			int lastIndexOfSlash = path.lastIndexOf('/');
			if (lastIndexOfSlash >= 0 && lastIndexOfSlash + 1 < url.length()) {
				mediaTitle = path.substring(lastIndexOfSlash + 1);
				int lastIndexOfDot = mediaTitle.lastIndexOf('.');
				if (lastIndexOfDot > 0) {
					mediaTitle = mediaTitle.substring(0, lastIndexOfDot);
				}
			} else {
				mediaTitle = path;
			}
			return mediaTitle.isEmpty() ? url : mediaTitle;
		} catch (MalformedURLException mfu) {
			return url;
		}
	}

	/**
	 * Evaluates if the specified character sequence is {@code null}, empty or
	 * only consists of whitespace.
	 *
	 * @param cs the {@link CharSequence} to evaluate.
	 * @return true if {@code cs} is {@code null}, empty or only consists of
	 *         whitespace, {@code false} otherwise.
	 */
	public static boolean isBlank(@Nullable CharSequence cs) {
		if (cs == null) {
			return true;
		}
		int strLen = cs.length();
		if (strLen == 0) {
			return true;
		}
		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(cs.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Asserts that the specified {@link Object} is non-{@code null} by throwing
	 * an {@link IllegalArgumentException} if it is.
	 *
	 * @param object the {@link Object} to assert that isn't {@code null}.
	 * @param objectName the identifier name to be used in the thrown
	 *            {@link IllegalArgumentException}.
	 *
	 * @throws IllegalArgumentException If {@code object} is {@code null}.
	 * @throws AssertionError If {@code objectName} is {@code null}.
	 */
	public static void requireNotNull(@Nullable Object object, @Nonnull String objectName) {
		if (objectName == null) {
			throw new AssertionError("Invalid use of requireNotNull, objectName must be specified");
		}
		if (object == null) {
			throw new IllegalArgumentException(objectName + " cannot be null");
		}
	}

	/**
	 * Asserts that the specified {@link CharSequence} isn't blank by throwing
	 * an {@link IllegalArgumentException} if it is.
	 *
	 * @param charSequence the {@link CharSequence} to assert that isn't blank.
	 * @param charSequenceName the identifier name to be used in the thrown
	 *            {@link IllegalArgumentException}.
	 *
	 * @throws IllegalArgumentException If {@code charSequence} is {@code null}
	 *             or blank.
	 * @throws AssertionError If {@code charSequenceName} is {@code null}.
	 */
	public static void requireNotBlank(@Nullable CharSequence charSequence, @Nonnull String charSequenceName) {
		if (charSequenceName == null) {
			throw new AssertionError("Invalid use of requireNotBlank, charSequenceName must be specified");
		}
		if (charSequence == null || isBlank(charSequence)) {
			throw new IllegalArgumentException(charSequenceName + " cannot be null or blank");
		}
	}
}
