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

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;


/**
 * The event sent when a {@code CLOSE} message that doesn't belong to any known
 * {@link Session} is received. {@code CLOSE} messages have no payload.
 */
@Immutable
public class CloseMessageEvent {

	/** The source ID from the message */
	@Nullable
	protected final String sourceId;

	/** The destination ID from the message */
	@Nullable
	protected final String destinationId;

	/** The namespace from the message */
	@Nullable
	protected final String namespace;

	/**
	 * Creates a new instance.
	 *
	 * @param sourceId the source ID from the message.
	 * @param destinationId the destination ID from the message.
	 * @param namespace the namespace from the message.
	 */
	public CloseMessageEvent(
		@Nullable String sourceId,
		@Nullable String destinationId,
		@Nullable String namespace
	) {
		this.sourceId = sourceId;
		this.destinationId = destinationId;
		this.namespace = namespace;
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
	 * @return The the message namespace.
	 */
	@Nullable
	public String getNamespace() {
		return namespace;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CloseMessageEvent [");
		if (sourceId != null) {
			builder.append("sourceId=").append(sourceId).append(", ");
		}
		if (destinationId != null) {
			builder.append("destinationId=").append(destinationId).append(", ");
		}
		if (namespace != null) {
			builder.append("namespace=").append(namespace);
		}
		builder.append("]");
		return builder.toString();
	}
}
