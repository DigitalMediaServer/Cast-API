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
import org.digitalmediaserver.cast.protobuf.CastChannel.CastMessage.ProtocolVersion;
import com.google.protobuf.ByteString;


/**
 * An {@link ImmutableCastMessage} implementation for binary payloads.
 *
 * @author Nadahar
 */
public class ImmutableBinaryCastMessage extends ImmutableCastMessage {

	/** The message payload */
	@Nonnull
	protected final ByteString payload;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param destinationId the destination ID.
	 * @param namespace the namespace.
	 * @param payload the binary payload.
	 * @param protocolVersion the protocol version.
	 * @param sourceId the source ID.
	 * @throws IllegalArgumentException If any of the parameters are
	 *             {@code null}.
	 */
	public ImmutableBinaryCastMessage(
		@Nonnull String destinationId,
		@Nonnull String namespace,
		@Nonnull ByteString payload,
		@Nonnull ProtocolVersion protocolVersion,
		@Nonnull String sourceId
	) {
		super(destinationId, namespace, protocolVersion, sourceId);
		requireNotNull(payload, "payload");
		this.payload = payload;
	}

	/**
	 * @return The binary message payload.
	 */
	@Nonnull
	public ByteString getPayload() {
		return payload;
	}
}
