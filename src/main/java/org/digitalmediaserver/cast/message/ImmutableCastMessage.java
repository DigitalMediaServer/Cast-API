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
package org.digitalmediaserver.cast.message;

import static org.digitalmediaserver.cast.util.Util.requireNotNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.cast.protobuf.CastChannel.CastMessage;
import org.digitalmediaserver.cast.protobuf.CastChannel.CastMessage.PayloadType;
import org.digitalmediaserver.cast.protobuf.CastChannel.CastMessage.ProtocolVersion;


/**
 * An immutable implementation of the essential data of a {@link CastMessage},
 * which allows incoming messages be passed around to multiple threads without
 * risking mutation.
 *
 * @author Nadahar
 */
@Immutable
public abstract class ImmutableCastMessage {

	/**
	 * The destination ID.
	 * <p>
	 * The source and destination IDs identify the origin and destination of a
	 * message. They are used to route messages between endpoints that share a
	 * device-to-device channel.
	 * <p>
	 * For messages between applications:
	 * <ul>
	 * <li>The sender application id is a unique identifier generated on behalf
	 * of the sender application.</li>
	 * <li>The receiver id is always the the session id for the
	 * application.</li>
	 * </ul>
	 * For messages to or from the sender or receiver platform, the special IDs
	 * '{@code sender-0}' and '{@code receiver-0}' can be used.
	 * <p>
	 * For messages intended for all endpoints using a given channel, the
	 * wildcard destination_id '{@code *}' can be used.
	 */
	@Nonnull
	protected final String destinationId;

	/**
	 * The namespace, the core multiplexing key. All messages are sent on a
	 * namespace and endpoints sharing a channel listen on one or more
	 * namespaces. The namespace defines the protocol and semantics of the
	 * message.
	 */
	@Nonnull
	protected final String namespace;

	/** The protocol version */
	@Nonnull
	protected final ProtocolVersion protocolVersion;

	/**
	 * The source ID.
	 * <p>
	 * The source and destination IDs identify the origin and destination of a
	 * message. They are used to route messages between endpoints that share a
	 * device-to-device channel.
	 * <p>
	 * For messages between applications:
	 * <ul>
	 * <li>The sender application id is a unique identifier generated on behalf
	 * of the sender application.</li>
	 * <li>The receiver id is always the the session id for the
	 * application.</li>
	 * </ul>
	 * For messages to or from the sender or receiver platform, the special IDs
	 * '{@code sender-0}' and '{@code receiver-0}' can be used.
	 * <p>
	 * For messages intended for all endpoints using a given channel, the
	 * wildcard destination_id '{@code *}' can be used.
	 */
	@Nonnull
	protected final String sourceId;

	/**
	 * Abstract constructor.
	 *
	 * @param destinationId the destination ID.
	 * @param namespace the namespace.
	 * @param protocolVersion the protocol version.
	 * @param sourceId the source ID.
	 * @throws IllegalArgumentException If any of the parameters are {@code null}.
	 */
	protected ImmutableCastMessage(
		@Nonnull String destinationId,
		@Nonnull String namespace,
		@Nonnull ProtocolVersion protocolVersion,
		@Nonnull String sourceId
	) {
		requireNotNull(destinationId, "destinationId");
		requireNotNull(namespace, "namespace");
		requireNotNull(protocolVersion, "protocolVersion");
		requireNotNull(sourceId, "sourceId");
		this.destinationId = destinationId;
		this.namespace = namespace;
		this.protocolVersion = protocolVersion;
		this.sourceId = sourceId;
	}

	/**
	 * The source and destination IDs identify the origin and destination of a
	 * message. They are used to route messages between endpoints that share a
	 * device-to-device channel.
	 * <p>
	 * For messages between applications:
	 * <ul>
	 * <li>The sender application id is a unique identifier generated on behalf
	 * of the sender application.</li>
	 * <li>The receiver id is always the the session id for the
	 * application.</li>
	 * </ul>
	 * For messages to or from the sender or receiver platform, the special IDs
	 * '{@code sender-0}' and '{@code receiver-0}' can be used.
	 * <p>
	 * For messages intended for all endpoints using a given channel, the
	 * wildcard destination_id '{@code *}' can be used.
	 *
	 * @return The destination ID.
	 */
	@Nonnull
	public String getDestinationId() {
		return destinationId;
	}

	/**
	 * @return The namespace, the core multiplexing key. All messages are sent
	 *         on a namespace and endpoints sharing a channel listen on one or
	 *         more namespaces. The namespace defines the protocol and semantics
	 *         of the message.
	 */
	@Nonnull
	public String getNamespace() {
		return namespace;
	}

	/**
	 * @return The protocol version.
	 */
	@Nonnull
	public ProtocolVersion getProtocolVersion() {
		return protocolVersion;
	}

	/**
	 * The source and destination IDs identify the origin and destination of a
	 * message. They are used to route messages between endpoints that share a
	 * device-to-device channel.
	 * <p>
	 * For messages between applications:
	 * <ul>
	 * <li>The sender application id is a unique identifier generated on behalf
	 * of the sender application.</li>
	 * <li>The receiver id is always the the session id for the
	 * application.</li>
	 * </ul>
	 * For messages to or from the sender or receiver platform, the special IDs
	 * '{@code sender-0}' and '{@code receiver-0}' can be used.
	 * <p>
	 * For messages intended for all endpoints using a given channel, the
	 * wildcard destination_id '{@code *}' can be used.
	 *
	 * @return The source ID.
	 */
	@Nonnull
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Creates either an {@link ImmutableStringCastMessage} or an
	 * {@link ImmutableBinaryCastMessage} depending on the payload type, which
	 * holds the data from the specified {@link CastMessage}.
	 *
	 * @param message the {@link CastMessage} to get the data from.
	 * @return The new {@link ImmutableCastMessage} instance.
	 * @throws IllegalArgumentException If any of the required fields of
	 *             {@code message} is {@code null}.
	 */
	@Nonnull
	public static ImmutableCastMessage create(@Nonnull CastMessage message) {
		requireNotNull(message, "message");
		if (message.getPayloadType() == PayloadType.STRING) {
			return new ImmutableStringCastMessage(
				message.getDestinationId(),
				message.getNamespace(),
				message.getPayloadUtf8(),
				message.getProtocolVersion(),
				message.getSourceId()
			);
		}
		return new ImmutableBinaryCastMessage(
			message.getDestinationId(),
			message.getNamespace(),
			message.getPayloadBinary(),
			message.getProtocolVersion(),
			message.getSourceId()
		);
	}
}
