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
package org.digitalmediaserver.cast;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.cast.CastChannel.CastMessage.PayloadType;
import com.google.protobuf.ByteString;


/**
 * A custom event sent by a receiver application.
 */
@Immutable
public class CustomMessageEvent {

	/** The source ID from the message */
	@Nullable
	protected final String sourceId;

	/** The destination ID from the message */
	@Nullable
	protected final String destinationId;

	/** The namespace from the message */
	@Nullable
	protected final String namespace;

	/** The string payload from the message */
	@Nullable
	protected final String stringPayload;

	/** The binary payload from the message */
	@Nullable
	protected final ByteString binaryPayload;

	/** The {@link PayloadType} */
	@Nonnull
	protected final PayloadType payloadType;

	/**
	 * Creates a new {@code STRING} instance.
	 *
	 * @param sourceId the source ID from the message.
	 * @param destinationId the destination ID from the message.
	 * @param namespace the namespace from the message.
	 * @param stringPayload the string payload from the message.
	 */
	public CustomMessageEvent(
		@Nullable String sourceId,
		@Nullable String destinationId,
		@Nullable String namespace,
		@Nullable String stringPayload
	) {
		this.sourceId = sourceId;
		this.destinationId = destinationId;
		this.namespace = namespace;
		this.stringPayload = stringPayload;
		this.binaryPayload = null;
		this.payloadType = PayloadType.STRING;
	}

	/**
	 * Creates a new {@code BINARY} instance.
	 *
	 * @param sourceId the source ID from the message.
	 * @param destinationId the destination ID from the message.
	 * @param namespace the namespace from the message.
	 * @param binaryPayload the binary payload from the message.
	 */
	public CustomMessageEvent(
		@Nullable String sourceId,
		@Nullable String destinationId,
		@Nullable String namespace,
		@Nullable ByteString binaryPayload
	) {
		this.sourceId = sourceId;
		this.destinationId = destinationId;
		this.namespace = namespace;
		this.stringPayload = null;
		this.binaryPayload = binaryPayload;
		this.payloadType = PayloadType.BINARY;
	}

	/**
	 * @return The message source ID.
	 */
	@Nullable
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * @return The message destination ID.
	 */
	@Nullable
	public String getDestinationId() {
		return destinationId;
	}

	/**
	 * @return The message namespace.
	 */
	@Nullable
	public String getNamespace() {
		return namespace;
	}

	/**
	 * @return The {@link String} payload if the source message is of type
	 *         {@code STRING}. If the message is {@code BINARY}, always
	 *         {@code null}.
	 */
	@Nullable
	public String getStringPayload() {
		return stringPayload;
	}

	/**
	 * @return The {@link ByteString} payload if the source message is of type
	 *         {@code BINARY}. If the message is {@code STRING}, always
	 *         {@code null}.
	 */
	@Nullable
	public ByteString getBinaryPayload() {
		return binaryPayload;
	}

	/**
	 * @return The type of payload in the source message.
	 */
	@Nonnull
	public PayloadType getPayloadType() {
		return payloadType;
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append(" [").append("namespace: ").append(namespace);
		if (payloadType == PayloadType.STRING) {
			sb.append(", string payload: ").append(stringPayload);
		} else {
			sb.append(", binary payload: ").append(binaryPayload);
		}
		sb.append(']');
		return sb.toString();
	}
}
