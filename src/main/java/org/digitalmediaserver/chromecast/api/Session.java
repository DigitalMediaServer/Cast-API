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
package org.digitalmediaserver.chromecast.api;

import java.io.IOException;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.digitalmediaserver.chromecast.api.Media.MediaBuilder;


/**
 * This class represents a "session" or "virtual connection" with a "remote
 * application" running on a cast device.
 * <p>
 * When a connection is established with a cast device, communication is only
 * possible with the "device itself", offering only a very limited interaction.
 * To make the cast device "do anything", an application must be running (which
 * can be launched with a command to the device), and a "connection within the
 * connection" must be made to communicate with the specific remote application.
 * A {@link Session} encapsulates this "inner connection".
 *
 * @author Nadahar
 */
@ThreadSafe
public class Session {

	/** The sender ID used by this {@link Session} */
	@Nonnull
	protected final String senderId;

	/** The session ID used by this {@link Session} */
	@Nonnull
	protected final String id;

	/** The destination ID used by this {@link Session} */
	@Nonnull
	protected final String destinationId;

	/** The {@link Channel} used by this {@link Session} */
	@Nonnull
	protected final Channel channel;

	/** The synchronization object used to protect {@code listener} */
	@Nonnull
	protected final Object listenerLock = new Object();

	/** The listener to invoke if the session is closed by the remote peer */
	@Nullable
	@GuardedBy("listenerLock")
	protected ClosedByPeerListener listener;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param senderId the sender ID to use.
	 * @param sessionId the session ID to use.
	 * @param destinationId the destination ID to use.
	 * @param channel the {@link Channel} to use.
	 */
	public Session(
		@Nonnull String senderId,
		@Nonnull String sessionId,
		@Nonnull String destinationId,
		@Nonnull Channel channel
	) {
		Util.requireNotBlank(senderId, "senderId");
		Util.requireNotBlank(sessionId, "sessionId");
		Util.requireNotBlank(destinationId, "destinationId");
		Util.requireNotNull(channel, "channel");
		this.senderId = senderId;
		this.id = sessionId;
		this.destinationId = destinationId;
		this.channel = channel;
	}

	/**
	 * @return The sender ID used by this {@link Session}.
	 */
	@Nonnull
	public String getSenderId() {
		return senderId;
	}

	/**
	 * @return The session ID used by this {@link Session}.
	 */
	@Nonnull
	public String getId() {
		return id;
	}

	/**
	 * @return The destination ID used by this {@link Session}.
	 */
	@Nonnull
	public String getDestinationId() {
		return destinationId;
	}

	/**
	 * @return {@code true} if this {@link Session} is closed, {@code false}
	 *         otherwise.
	 */
	public boolean isClosed() {
		return channel.isSessionClosed(this);
	}

	/**
	 * Closes this {@link Session}.
	 *
	 * @return {@code true} if this {@link Session} was closed, {@code false} if
	 *         it already was.
	 * @throws IOException If an error occurs during the operation.
	 */
	public boolean close() throws IOException {
		return channel.closeSession(this);
	}

	/**
	 * @return The listener that is invoked if the session is closed by the
	 *         remote peer, if any.
	 */
	@Nullable
	public ClosedByPeerListener getClosedByPeerListener() {
		synchronized (listenerLock) {
			return listener;
		}
	}

	/**
	 * Sets the listener that is to be invoked if the session is closed by the
	 * remote peer.
	 *
	 * @param listener The {@link ClosedByPeerListener} implementation to
	 *            invoke.
	 */
	public void setClosedByPeerListener(@Nullable ClosedByPeerListener listener) {
		synchronized (listenerLock) {
			this.listener = listener;
		}
	}

	/**
	 * Asks the remote application to load the specified {@link Media} using the
	 * specified parameters. This can only succeed if the remote application
	 * supports the "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param mediaBuilder the {@link MediaBuilder} to use to create the
	 *            {@link Media} to load.
	 * @param autoplay {@code true} to ask the remote application to start
	 *            playback as soon as the {@link Media} has been loaded,
	 *            {@code false} to ask it to transition to a paused state after
	 *            loading.
	 * @param currentTime the position in seconds where playback are to be
	 *            started in the loaded {@link Media}.
	 * @param synchronous {@code true} to make this call blocking until a
	 *            response is received or times out, {@code false} to make it
	 *            return immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IOException If an error occurs during the operation.
	 */
	@Nullable
	public MediaStatus load(
		MediaBuilder mediaBuilder,
		boolean autoplay,
		double currentTime,
		boolean synchronous
	) throws IOException {
		return channel.load(
			senderId,
			destinationId,
			id,
			mediaBuilder == null ? null : mediaBuilder.build(),
			autoplay,
			currentTime,
			synchronous,
			null
		);
	}

	/**
	 * Asks the remote application to load the specified {@link Media} using the
	 * specified parameters. This can only succeed if the remote application
	 * supports the "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param media the {@link Media} to load.
	 * @param autoplay {@code true} to ask the remote application to start
	 *            playback as soon as the {@link Media} has been loaded,
	 *            {@code false} to ask it to transition to a paused state after
	 *            loading.
	 * @param currentTime the position in seconds where playback are to be
	 *            started in the loaded {@link Media}.
	 * @param synchronous {@code true} to make this call blocking until a
	 *            response is received or times out, {@code false} to make it
	 *            return immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IOException If an error occurs during the operation.
	 */
	@Nullable
	public MediaStatus load(
		Media media,
		boolean autoplay,
		double currentTime,
		boolean synchronous
	) throws IOException {
		return channel.load(senderId, destinationId, id, media, autoplay, currentTime, synchronous, null);
	}

	/**
	 * Asks the remote application to load the specified {@link Media} using the
	 * specified parameters. This can only succeed if the remote application
	 * supports the "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param media the {@link Media} to load.
	 * @param autoplay {@code true} to ask the remote application to start
	 *            playback as soon as the {@link Media} has been loaded,
	 *            {@code false} to ask it to transition to a paused state after
	 *            loading.
	 * @param currentTime the position in seconds where playback are to be
	 *            started in the loaded {@link Media}.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @param customData the custom application data to send to the remote
	 *            application with the load command.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IOException If an error occurs during the operation.
	 */
	@Nullable
	public MediaStatus load(
		Media media,
		boolean autoplay,
		double currentTime,
		boolean synchronous,
		Map<String, String> customData
	) throws IOException {
		return channel.load(senderId, destinationId, id, media, autoplay, currentTime, synchronous, customData);
	}

	/**
	 * Asks the remote application to start playing the media referenced by the
	 * specified media session ID. This can only succeed if the remote
	 * application supports the "{@code urn:x-cast:com.google.cast.media}"
	 * namespace.
	 *
	 * @param mediaSessionId the media session ID for which the play request
	 *            applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IOException If an error occurs during the operation.
	 */
	@Nullable
	public MediaStatus play(long mediaSessionId, boolean synchronous) throws IOException {
		return channel.play(senderId, destinationId, id, mediaSessionId, synchronous);
	}

	/**
	 * Asks the remote application to pause playback of the media referenced by
	 * the specified media session ID. This can only succeed if the remote
	 * application supports the "{@code urn:x-cast:com.google.cast.media}"
	 * namespace.
	 *
	 * @param mediaSessionId the media session ID for which the pause request
	 *            applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IOException If an error occurs during the operation.
	 */
	@Nullable
	public MediaStatus pause(long mediaSessionId, boolean synchronous) throws IOException {
		return channel.pause(senderId, destinationId, id, mediaSessionId, synchronous);
	}

	/**
	 * Asks the remote application to move the playback position of the media
	 * referenced by the specified media session ID to the specified position.
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param mediaSessionId the media session ID for which the pause request
	 *            applies.
	 * @param currentTime the new playback position in seconds.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IOException If an error occurs during the operation.
	 */
	@Nullable
	public MediaStatus seek(long mediaSessionId, double currentTime, boolean synchronous) throws IOException {
		return channel.seek(senderId, destinationId, id, mediaSessionId, currentTime, synchronous);
	}

	/**
	 * Requests an updated {@link MediaStatus} from the remote application. This
	 * method is always blocking. This can only succeed if the remote
	 * application supports the "{@code urn:x-cast:com.google.cast.media}"
	 * namespace.
	 *
	 * @return The resulting {@link MediaStatus} if a reply is received in time,
	 *         or {@code null} if a timeout occurs.
	 * @throws IOException If an error occurs during the operation.
	 */
	@Nullable
	public MediaStatus getMediaStatus() throws IOException {
		return channel.getMediaStatus(senderId, destinationId);
	}

	/**
	 * Sends the specified {@link Request} with the specified namespace using
	 * this {@link Session}.
	 *
	 * @param <T> the class of the {@link Response} object.
	 * @param namespace the namespace to use.
	 * @param request the {@link Request} to send.
	 * @param responseClass the response class to to block and wait for or
	 *            {@code null} to return immediately.
	 * @return The {@link Response} if the response is received in time, or
	 *         {@code null} if the {@code responseClass} is {@code null} or a
	 *         timeout occurs.
	 * @throws IOException If an error occurs during the operation.
	 */
	public <T extends Response> T sendGenericRequest(
		String namespace,
		Request request,
		Class<T> responseClass
	) throws IOException {
		return channel.sendGenericRequest(senderId, destinationId, namespace, request, responseClass);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		if (senderId != null) {
			builder.append("senderId=").append(senderId).append(", ");
		}
		if (id != null) {
			builder.append("id=").append(id).append(", ");
		}
		if (destinationId != null) {
			builder.append("destinationId=").append(destinationId).append(", ");
		}
		builder.append("closed=").append(isClosed()).append("]");
		return builder.toString();
	}

	/**
	 * An interface defining a callback which is invoked when a {@link Session}
	 * is closed by the remote peer. It is not invoked if the {@link Session} is
	 * closed from "our side".
	 *
	 * @author Nadahar
	 */
	public interface ClosedByPeerListener {

		/**
		 * Called when the specified {@link Session} is closed by the remote
		 * peer.
		 *
		 * @param session the {@link Session} that is being closed.
		 */
		void closed(@Nonnull Session session);
	}
}
