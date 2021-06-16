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
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.digitalmediaserver.chromecast.api.Media.MediaBuilder;
import org.digitalmediaserver.chromecast.api.StandardRequest.ResumeState;


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

	/** The listener to invoke if the session is closed */
	@Nullable
	@GuardedBy("listenerLock")
	protected SessionClosedListener listener;

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
	 * @return The listener that is invoked if the session is closed, if any.
	 */
	@Nullable
	public SessionClosedListener getSessionClosedListener() {
		synchronized (listenerLock) {
			return listener;
		}
	}

	/**
	 * Sets the listener that is to be invoked if the session is closed.
	 *
	 * @param listener The {@link SessionClosedListener} implementation to
	 *            invoke.
	 */
	public void setSessionClosedListener(@Nullable SessionClosedListener listener) {
		synchronized (listenerLock) {
			this.listener = listener;
		}
	}

//	/**
//	 * Asks the remote application to load the specified {@link Media} using the
//	 * specified parameters and {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the
//	 * timeout value.
//	 * <p>
//	 * This can only succeed if the remote application supports the
//	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
//	 *
//	 * @param mediaBuilder the {@link MediaBuilder} to use to create the
//	 *            {@link Media} to load.
//	 * @param autoplay {@code true} to ask the remote application to start
//	 *            playback as soon as the {@link Media} has been loaded,
//	 *            {@code false} to ask it to transition to a paused state after
//	 *            loading.
//	 * @param currentTime the position in seconds where playback are to be
//	 *            started in the loaded {@link Media}.
//	 * @param synchronous {@code true} to make this call blocking until a
//	 *            response is received or times out, {@code false} to make it
//	 *            return immediately always returning {@code null}.
//	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
//	 *         {@code true} and a reply is received in time, {@code null} if
//	 *         {@code synchronous} is {@code false}.
//	 * @throws IOException If the response times out or an error occurs during
//	 *             the operation.
//	 */
//	@Nullable
//	public MediaStatus load(
//		MediaBuilder mediaBuilder,
//		boolean autoplay,
//		double currentTime,
//		boolean synchronous
//	) throws IOException {
//		return channel.load(
//			senderId,
//			destinationId,
//			id,
//			mediaBuilder == null ? null : mediaBuilder.build(),
//			autoplay,
//			currentTime,
//			synchronous,
//			null
//		);
//	}
//
//	/**
//	 * Asks the remote application to load the specified {@link Media} using the
//	 * specified parameters.
//	 * <p>
//	 * This can only succeed if the remote application supports the
//	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
//	 *
//	 * @param mediaBuilder the {@link MediaBuilder} to use to create the
//	 *            {@link Media} to load.
//	 * @param autoplay {@code true} to ask the remote application to start
//	 *            playback as soon as the {@link Media} has been loaded,
//	 *            {@code false} to ask it to transition to a paused state after
//	 *            loading.
//	 * @param currentTime the position in seconds where playback are to be
//	 *            started in the loaded {@link Media}.
//	 * @param synchronous {@code true} to make this call blocking until a
//	 *            response is received or times out, {@code false} to make it
//	 *            return immediately always returning {@code null}.
//	 * @param responseTimeout the response timeout in milliseconds if
//	 *            {@code synchronous} is {@code true}. If zero or negative,
//	 *            {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
//	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
//	 *         {@code true} and a reply is received in time, {@code null} if
//	 *         {@code synchronous} is {@code false}.
//	 * @throws IOException If the response times out or an error occurs during
//	 *             the operation.
//	 */
//	@Nullable
//	public MediaStatus load(
//		MediaBuilder mediaBuilder,
//		boolean autoplay,
//		double currentTime,
//		boolean synchronous,
//		long responseTimeout
//	) throws IOException {
//		return channel.load(
//			senderId,
//			destinationId,
//			id,
//			mediaBuilder == null ? null : mediaBuilder.build(),
//			autoplay,
//			currentTime,
//			synchronous,
//			responseTimeout,
//			null
//		);
//	}
//
//	/**
//	 * Asks the remote application to load the specified {@link Media} using the
//	 * specified parameters and {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the
//	 * timeout value.
//	 * <p>
//	 * This can only succeed if the remote application supports the
//	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
//	 *
//	 * @param media the {@link Media} to load.
//	 * @param autoplay {@code true} to ask the remote application to start
//	 *            playback as soon as the {@link Media} has been loaded,
//	 *            {@code false} to ask it to transition to a paused state after
//	 *            loading.
//	 * @param currentTime the position in seconds where playback are to be
//	 *            started in the loaded {@link Media}.
//	 * @param synchronous {@code true} to make this call blocking until a
//	 *            response is received or times out, {@code false} to make it
//	 *            return immediately always returning {@code null}.
//	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
//	 *         {@code true} and a reply is received in time, {@code null} if
//	 *         {@code synchronous} is {@code false}.
//	 * @throws IOException If the response times out or an error occurs during
//	 *             the operation.
//	 */
//	@Nullable
//	public MediaStatus load(
//		Media media,
//		boolean autoplay,
//		double currentTime,
//		boolean synchronous
//	) throws IOException {
//		return channel.load(senderId, destinationId, id, media, autoplay, currentTime, synchronous, null);
//	}
//
//	/**
//	 * Asks the remote application to load the specified {@link Media} using the
//	 * specified parameters.
//	 * <p>
//	 * This can only succeed if the remote application supports the
//	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
//	 *
//	 * @param media the {@link Media} to load.
//	 * @param autoplay {@code true} to ask the remote application to start
//	 *            playback as soon as the {@link Media} has been loaded,
//	 *            {@code false} to ask it to transition to a paused state after
//	 *            loading.
//	 * @param currentTime the position in seconds where playback are to be
//	 *            started in the loaded {@link Media}.
//	 * @param synchronous {@code true} to make this call block until a response
//	 *            is received or times out, {@code false} to make it return
//	 *            immediately always returning {@code null}.
//	 * @param responseTimeout the response timeout in milliseconds if
//	 *            {@code synchronous} is {@code true}. If zero or negative,
//	 *            {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
//	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
//	 *         {@code true} and a reply is received in time, {@code null} if
//	 *         {@code synchronous} is {@code false}.
//	 * @throws IOException If the response times out or an error occurs during
//	 *             the operation.
//	 */
//	@Nullable
//	public MediaStatus load(
//		Media media,
//		boolean autoplay,
//		double currentTime,
//		boolean synchronous,
//		long responseTimeout
//	) throws IOException {
//		return channel.load(
//			senderId,
//			destinationId,
//			id,
//			media,
//			autoplay,
//			currentTime,
//			synchronous,
//			responseTimeout,
//			null
//		);
//	}
//
//	/**
//	 * Asks the remote application to load the specified {@link Media} using the
//	 * specified parameters and {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the
//	 * timeout value.
//	 * <p>
//	 * This can only succeed if the remote application supports the
//	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
//	 *
//	 * @param media the {@link Media} to load.
//	 * @param autoplay {@code true} to ask the remote application to start
//	 *            playback as soon as the {@link Media} has been loaded,
//	 *            {@code false} to ask it to transition to a paused state after
//	 *            loading.
//	 * @param currentTime the position in seconds where playback are to be
//	 *            started in the loaded {@link Media}.
//	 * @param synchronous {@code true} to make this call block until a response
//	 *            is received or times out, {@code false} to make it return
//	 *            immediately always returning {@code null}.
//	 * @param customData the custom application data to send to the remote
//	 *            application with the load command.
//	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
//	 *         {@code true} and a reply is received in time, {@code null} if
//	 *         {@code synchronous} is {@code false}.
//	 * @throws IOException If the response times out or an error occurs during
//	 *             the operation.
//	 */
//	@Nullable
//	public MediaStatus load(
//		Media media,
//		boolean autoplay,
//		double currentTime,
//		boolean synchronous,
//		Map<String, Object> customData
//	) throws IOException {
//		return channel.load(senderId, destinationId, id, media, autoplay, currentTime, synchronous, customData);
//	}

	/**
	 * Asks the remote application to load the specified {@link Media}
	 * using the specified parameters.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
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
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code synchronous} is {@code true}. If zero or negative,
	 *            {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @param customData the custom application data to send to the remote
	 *            application with the load command.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus load( //TODO: (Nad) JAvaDocs
		@Nonnull Media media,
		@Nullable Boolean autoplay,
		@Nullable Double currentTime,
		@Nullable Map<String, Object> customData,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		return channel.load(
			senderId,
			destinationId,
			autoplay,
			currentTime,
			media,
			customData,
			synchronous,
			responseTimeout
		);
	}

	@Nullable
	public MediaStatus load( //TODO: (Nad) JAvaDocs
		@Nonnull Media media,
		@Nullable List<Integer> activeTrackIds,
		@Nullable Boolean autoplay,
		@Nullable String credentials,
		@Nullable String credentialsType,
		@Nullable Double currentTime,
		@Nullable Map<String, Object> customData,
		@Nullable LoadOptions loadOptions,
		@Nullable Double playbackRate,
		@Nullable QueueData queueData,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		return channel.load(
			senderId,
			destinationId,
			activeTrackIds,
			autoplay,
			credentials,
			credentialsType,
			currentTime,
			customData,
			loadOptions,
			media,
			playbackRate,
			queueData,
			synchronous,
			responseTimeout
		);
	}

	/**
	 * Asks the remote application to start playing the media referenced by the
	 * specified media session ID, using
	 * {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the timeout value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param mediaSessionId the media session ID for which the play request
	 *            applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus play(int mediaSessionId, boolean synchronous) throws IOException {
		return channel.play(senderId, destinationId, id, mediaSessionId, synchronous);
	}

	/**
	 * Asks the remote application to start playing the media referenced by the
	 * specified media session ID.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param mediaSessionId the media session ID for which the play request
	 *            applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code synchronous} is {@code true}. If zero or negative,
	 *            {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus play(int mediaSessionId, boolean synchronous, long responseTimeout) throws IOException {
		return channel.play(senderId, destinationId, id, mediaSessionId, synchronous, responseTimeout);
	}

	/**
	 * Asks the remote application to pause playback of the media referenced by
	 * the specified media session ID, using
	 * {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the timeout value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param mediaSessionId the media session ID for which the pause request
	 *            applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus pause(int mediaSessionId, boolean synchronous) throws IOException {
		return channel.pause(senderId, destinationId, id, mediaSessionId, synchronous);
	}

	/**
	 * Asks the remote application to pause playback of the media referenced by
	 * the specified media session ID.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param mediaSessionId the media session ID for which the pause request
	 *            applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code synchronous} is {@code true}. If zero or negative,
	 *            {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus pause(int mediaSessionId, boolean synchronous, long responseTimeout) throws IOException {
		return channel.pause(senderId, destinationId, id, mediaSessionId, synchronous, responseTimeout);
	}

	/**
	 * Asks the remote application to move the playback position of the media
	 * referenced by the specified media session ID to the specified position,
	 * using {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the timeout value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param mediaSessionId the media session ID for which the pause request
	 *            applies.
	 * @param currentTime the new playback position in seconds.
	 * @param resumeState the desired media player state after the seek is
	 *            complete. If {@code null}, it will retain the state it had
	 *            before seeking.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus seek(
		int mediaSessionId,
		double currentTime,
		@Nullable ResumeState resumeState,
		boolean synchronous
	) throws IOException {
		return channel.seek(senderId, destinationId, id, mediaSessionId, currentTime, resumeState, synchronous);
	}

	/**
	 * Asks the remote application to move the playback position of the media
	 * referenced by the specified media session ID to the specified position.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param mediaSessionId the media session ID for which the pause request
	 *            applies.
	 * @param currentTime the new playback position in seconds.
	 * @param resumeState the desired media player state after the seek is
	 *            complete. If {@code null}, it will retain the state it had
	 *            before seeking.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code synchronous} is {@code true}. If zero or negative,
	 *            {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus seek(
		int mediaSessionId,
		double currentTime,
		@Nullable ResumeState resumeState,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		return channel.seek(
			senderId,
			destinationId,
			id,
			mediaSessionId,
			currentTime,
			resumeState,
			synchronous,
			responseTimeout
		);
	}

	/**
	 * Asks the remote application to stop playback and unload the media
	 * referenced by the specified media session ID, using
	 * {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the timeout value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param mediaSessionId the media session ID for which the
	 *            {@link MediaVolume} request applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus stop(int mediaSessionId, boolean synchronous) throws IOException {
		return channel.stopMedia(senderId, destinationId, mediaSessionId, synchronous);
	}

	/**
	 * Asks the remote application to stop playback and unload the media
	 * referenced by the specified media session ID.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param mediaSessionId the media session ID for which the
	 *            {@link MediaVolume} request applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code synchronous} is {@code true}. If zero or negative,
	 *            {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus stop(int mediaSessionId, boolean synchronous, long responseTimeout) throws IOException {
		return channel.stopMedia(senderId, destinationId, mediaSessionId, synchronous, responseTimeout);
	}

	/**
	 * Asks the remote application to change the volume level or mute state of
	 * the stream of the specified media session. Please note that this is
	 * different from the device volume level or mute state, and that this will
	 * give the user no visual indication.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param mediaSessionId the media session ID for which the
	 *            {@link MediaVolume} request applies.
	 * @param volume the {@link MediaVolume} to set.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IllegalArgumentException If {@code volume} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus setVolume(
		int mediaSessionId,
		@Nonnull MediaVolume volume,
		boolean synchronous
	) throws IOException {
		return channel.setMediaVolume(senderId, destinationId, id, mediaSessionId, volume, synchronous);
	}

	/**
	 * Requests an updated {@link MediaStatus} from the remote application. This
	 * method is always blocking.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
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
	 * Requests an updated {@link MediaStatus} from the remote application. This
	 * method is always blocking.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param responseTimeout the response timeout in milliseconds. If zero or
	 *            negative, {@link Channel#DEFAULT_RESPONSE_TIMEOUT} will be
	 *            used.
	 * @return The resulting {@link MediaStatus} if a reply is received in time,
	 *         or {@code null} if a timeout occurs.
	 * @throws IOException If an error occurs during the operation.
	 */
	@Nullable
	public MediaStatus getMediaStatus(long responseTimeout) throws IOException {
		return channel.getMediaStatus(senderId, destinationId, responseTimeout);
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
	 * is closed.
	 *
	 * @author Nadahar
	 */
	public interface SessionClosedListener {

		/**
		 * Called when the specified {@link Session} is closed.
		 *
		 * @param session the {@link Session} that is being closed.
		 */
		void closed(@Nonnull Session session);
	}
}
