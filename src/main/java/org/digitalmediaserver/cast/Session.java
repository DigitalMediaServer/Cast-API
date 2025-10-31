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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.digitalmediaserver.cast.message.request.Request;
import org.digitalmediaserver.cast.message.entity.LoadOptions;
import org.digitalmediaserver.cast.message.entity.Media;
import org.digitalmediaserver.cast.message.entity.MediaStatus;
import org.digitalmediaserver.cast.message.entity.MediaVolume;
import org.digitalmediaserver.cast.message.entity.QueueData;
import org.digitalmediaserver.cast.message.entity.Media.MediaBuilder;
import org.digitalmediaserver.cast.message.enumeration.ResumeState;
import org.digitalmediaserver.cast.message.request.Load;
import org.digitalmediaserver.cast.message.response.Response;
import org.digitalmediaserver.cast.util.Util;


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

	/** The source ID used by this {@link Session} */
	@Nonnull
	protected final String sourceId;

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
	 * @param sourceId the source ID to use.
	 * @param sessionId the session ID to use.
	 * @param destinationId the destination ID to use.
	 * @param channel the {@link Channel} to use.
	 */
	public Session(
		@Nonnull String sourceId,
		@Nonnull String sessionId,
		@Nonnull String destinationId,
		@Nonnull Channel channel
	) {
		Util.requireNotBlank(sourceId, "sourceId");
		Util.requireNotBlank(sessionId, "sessionId");
		Util.requireNotBlank(destinationId, "destinationId");
		Util.requireNotNull(channel, "channel");
		this.sourceId = sourceId;
		this.id = sessionId;
		this.destinationId = destinationId;
		this.channel = channel;
	}

	/**
	 * @return The source ID used by this {@link Session}.
	 */
	@Nonnull
	public String getSourceId() {
		return sourceId;
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
	 *
	 * @apiNote This operation is non-blocking.
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
	 *
	 * @apiNote This operation is non-blocking.
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

	/**
	 * Asks the remote application to execute the specified {@link Load}
	 * request, using {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the timeout
	 * value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
	 *
	 * @param loadRequest the {@link Load} request to send.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true}, {@code null} if {@code synchronous} is
	 *         {@code false}.
	 * @throws IllegalArgumentException If {@code session} or
	 *             {@code loadRequest} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 *
	 * @apiNote This operation is blocking if {@code synchronous} is
	 *          {@code true}, otherwise non-blocking.
	 */
	@Nullable
	public MediaStatus load(@Nonnull Load loadRequest, boolean synchronous) throws IOException {
		return channel.load(this, loadRequest, synchronous, Channel.DEFAULT_RESPONSE_TIMEOUT);
	}

	/**
	 * Asks the remote application to execute the specified {@link Load}
	 * request.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
	 *
	 * @param loadRequest the {@link Load} request to send.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code synchronous} is {@code true}. If zero or negative,
	 *            {@value Channel#DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true}, {@code null} if {@code synchronous} is
	 *         {@code false}.
	 * @throws IllegalArgumentException If {@code session} or
	 *             {@code loadRequest} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 *
	 * @apiNote This operation is blocking if {@code synchronous} is
	 *          {@code true}, otherwise non-blocking.
	 */
	@Nullable
	public MediaStatus load(@Nonnull Load loadRequest, boolean synchronous, long responseTimeout) throws IOException {
		return channel.load(this, loadRequest, synchronous, responseTimeout);
	}

	/**
	 * Asks the remote application to load the resulting {@link Media} created
	 * from the specified {@link MediaBuilder} using the specified parameters
	 * and {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the timeout value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
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
	 *         {@code true}, {@code null} if {@code synchronous} is
	 *         {@code false}.
	 * @throws IllegalArgumentException If {@code mediaBuilder} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking if {@code synchronous} is
	 *          {@code true}, otherwise non-blocking.
	 */
	@Nullable
	public MediaStatus load(
		@Nonnull MediaBuilder mediaBuilder,
		@Nullable Boolean autoplay,
		@Nullable Double currentTime,
		boolean synchronous
	) throws IOException {
		Util.requireNotNull(mediaBuilder, "mediaBuilder");
		return channel.load(
			this,
			autoplay,
			currentTime,
			mediaBuilder.build(),
			synchronous,
			Channel.DEFAULT_RESPONSE_TIMEOUT
		);
	}

	/**
	 * Asks the remote application to load the resulting {@link Media} created
	 * from the specified {@link MediaBuilder} using the specified parameters.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
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
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code synchronous} is {@code true}. If zero or negative,
	 *            {@value Channel#DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true}, {@code null} if {@code synchronous} is
	 *         {@code false}.
	 * @throws IllegalArgumentException If {@code mediaBuilder} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking if {@code synchronous} is
	 *          {@code true}, otherwise non-blocking.
	 */
	@Nullable
	public MediaStatus load(
		@Nonnull MediaBuilder mediaBuilder,
		@Nullable Boolean autoplay,
		@Nullable Double currentTime,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		Util.requireNotNull(mediaBuilder, "mediaBuilder");
		return channel.load(
			this,
			autoplay,
			currentTime,
			mediaBuilder.build(),
			synchronous,
			responseTimeout
		);
	}

	/**
	 * Asks the remote application to load the specified {@link Media} using the
	 * specified parameters and {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the
	 * timeout value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
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
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true}, {@code null} if {@code synchronous} is
	 *         {@code false}.
	 * @throws IllegalArgumentException If {@code media} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking if {@code synchronous} is
	 *          {@code true}, otherwise non-blocking.
	 */
	@Nullable
	public MediaStatus load(
		@Nonnull Media media,
		@Nullable Boolean autoplay,
		@Nullable Double currentTime,
		boolean synchronous
	) throws IOException {
		return channel.load(
			this,
			autoplay,
			currentTime,
			media,
			synchronous,
			Channel.DEFAULT_RESPONSE_TIMEOUT
		);
	}

	/**
	 * Asks the remote application to load the specified {@link Media} using the
	 * specified parameters.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
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
	 *            {@value Channel#DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true}, {@code null} if {@code synchronous} is
	 *         {@code false}.
	 * @throws IllegalArgumentException If {@code session} or {@code media} is
	 *             {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking if {@code synchronous} is
	 *          {@code true}, otherwise non-blocking.
	 */
	@Nullable
	public MediaStatus load(
		@Nonnull Media media,
		@Nullable Boolean autoplay,
		@Nullable Double currentTime,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		return channel.load(
			this,
			autoplay,
			currentTime,
			media,
			synchronous,
			responseTimeout
		);
	}

	/**
	 * Asks the remote application to load the specified {@link Media} using the
	 * specified parameters.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
	 *
	 * @param media the {@link Media} to load.
	 * @param activeTrackIds the {@link List} of track IDs that are active. If
	 *            the list is not provided, the default tracks will be active.
	 * @param autoplay {@code true} to ask the remote application to start
	 *            playback as soon as the {@link Media} has been loaded,
	 *            {@code false} to ask it to transition to a paused state after
	 *            loading.
	 * @param currentTime the position in seconds from the start for where
	 *            playback is to start in the loaded {@link Media}. If the
	 *            content is live content, and {@code currentTime} is not
	 *            specified, the stream will start at the live position.
	 * @param playbackRate the media playback rate.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true}, {@code null} if {@code synchronous} is
	 *         {@code false}.
	 * @throws IllegalArgumentException If {@code media} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking if {@code synchronous} is
	 *          {@code true}, otherwise non-blocking.
	 */
	@Nullable
	public MediaStatus load(
		@Nonnull Media media,
		@Nullable List<Integer> activeTrackIds,
		@Nullable Boolean autoplay,
		@Nullable Double currentTime,
		@Nullable Double playbackRate,
		boolean synchronous
	) throws IOException {
		return channel.load(
			this,
			activeTrackIds,
			autoplay,
			null,
			null,
			currentTime,
			null,
			null,
			media,
			playbackRate,
			null,
			synchronous,
			Channel.DEFAULT_RESPONSE_TIMEOUT
		);
	}

	/**
	 * Asks the remote application to load the specified {@link Media} using the
	 * specified parameters and {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the
	 * timeout value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
	 *
	 * @param media the {@link Media} to load.
	 * @param activeTrackIds the {@link List} of track IDs that are active. If
	 *            the list is not provided, the default tracks will be active.
	 * @param autoplay {@code true} to ask the remote application to start
	 *            playback as soon as the {@link Media} has been loaded,
	 *            {@code false} to ask it to transition to a paused state after
	 *            loading.
	 * @param currentTime the position in seconds from the start for where
	 *            playback is to start in the loaded {@link Media}. If the
	 *            content is live content, and {@code currentTime} is not
	 *            specified, the stream will start at the live position.
	 * @param playbackRate the media playback rate.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code synchronous} is {@code true}. If zero or negative,
	 *            {@value Channel#DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true}, {@code null} if {@code synchronous} is
	 *         {@code false}.
	 * @throws IllegalArgumentException If {@code media} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking if {@code synchronous} is
	 *          {@code true}, otherwise non-blocking.
	 */
	@Nullable
	public MediaStatus load(
		@Nonnull Media media,
		@Nullable List<Integer> activeTrackIds,
		@Nullable Boolean autoplay,
		@Nullable Double currentTime,
		@Nullable Double playbackRate,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		return channel.load(
			this,
			activeTrackIds,
			autoplay,
			null,
			null,
			currentTime,
			null,
			null,
			media,
			playbackRate,
			null,
			synchronous,
			responseTimeout
		);
	}

	/**
	 * Asks the remote application to load the specified {@link Media} or
	 * {@link QueueData}s using the specified parameters, using
	 * {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the timeout value. Either
	 * {@code media} or {@code queueData} must be non-{@code null}.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
	 *
	 * @param media the {@link Media} to load.
	 * @param activeTrackIds the {@link List} of track IDs that are active. If
	 *            the list is not provided, the default tracks will be active.
	 * @param autoplay {@code true} to ask the remote application to start
	 *            playback as soon as the {@link Media} has been loaded,
	 *            {@code false} to ask it to transition to a paused state after
	 *            loading.
	 * @param credentials the user credentials, if any.
	 * @param credentialsType the credentials type, if any. The type
	 *            '{@code cloud}' is a reserved type used by load requests that
	 *            were originated by voice assistant commands.
	 * @param currentTime the position in seconds from the start for where
	 *            playback is to start in the loaded {@link Media}. If the
	 *            content is live content, and {@code currentTime} is not
	 *            specified, the stream will start at the live position.
	 * @param customData the custom application data to send to the remote
	 *            application with the load command.
	 * @param loadOptions the additional load options, if any.
	 * @param playbackRate the media playback rate.
	 * @param queueData the queue data.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true}, {@code null} if {@code synchronous} is
	 *         {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking if {@code synchronous} is
	 *          {@code true}, otherwise non-blocking.
	 */
	@Nullable
	public MediaStatus load(
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
		boolean synchronous
	) throws IOException {
		return channel.load(
			this,
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
			Channel.DEFAULT_RESPONSE_TIMEOUT
		);
	}

	/**
	 * Asks the remote application to load the specified {@link Media} or
	 * {@link QueueData}s using the specified parameters. Either {@code media}
	 * or {@code queueData} must be non-{@code null}.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
	 *
	 * @param media the {@link Media} to load.
	 * @param activeTrackIds the {@link List} of track IDs that are active. If
	 *            the list is not provided, the default tracks will be active.
	 * @param autoplay {@code true} to ask the remote application to start
	 *            playback as soon as the {@link Media} has been loaded,
	 *            {@code false} to ask it to transition to a paused state after
	 *            loading.
	 * @param credentials the user credentials, if any.
	 * @param credentialsType the credentials type, if any. The type
	 *            '{@code cloud}' is a reserved type used by load requests that
	 *            were originated by voice assistant commands.
	 * @param currentTime the position in seconds from the start for where
	 *            playback is to start in the loaded {@link Media}. If the
	 *            content is live content, and {@code currentTime} is not
	 *            specified, the stream will start at the live position.
	 * @param customData the custom application data to send to the remote
	 *            application with the load command.
	 * @param loadOptions the additional load options, if any.
	 * @param playbackRate the media playback rate.
	 * @param queueData the queue data.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code synchronous} is {@code true}. If zero or negative,
	 *            {@value Channel#DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true}, {@code null} if {@code synchronous} is
	 *         {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking if {@code synchronous} is
	 *          {@code true}, otherwise non-blocking.
	 */
	@Nullable
	public MediaStatus load(
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
			this,
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
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
	 *
	 * @param mediaSessionId the media session ID for which the play request
	 *            applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true}, {@code null} if {@code synchronous} is
	 *         {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking if {@code synchronous} is
	 *          {@code true}, otherwise non-blocking.
	 */
	@Nullable
	public MediaStatus play(int mediaSessionId, boolean synchronous) throws IOException {
		return channel.play(this, mediaSessionId, synchronous);
	}

	/**
	 * Asks the remote application to start playing the media referenced by the
	 * specified media session ID.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
	 *
	 * @param mediaSessionId the media session ID for which the play request
	 *            applies.
	 * @param responseTimeout the response timeout in milliseconds. If zero or
	 *            negative, {@value Channel#DEFAULT_RESPONSE_TIMEOUT} will be
	 *            used.
	 * @return The resulting {@link MediaStatus}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking.
	 */
	@Nonnull
	public MediaStatus play(int mediaSessionId, long responseTimeout) throws IOException {
		return channel.play(this, mediaSessionId, responseTimeout);
	}

	/**
	 * Asks the remote application to pause playback of the media referenced by
	 * the specified media session ID, using
	 * {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the timeout value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
	 *
	 * @param mediaSessionId the media session ID for which the pause request
	 *            applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true}, {@code null} if {@code synchronous} is
	 *         {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking if {@code synchronous} is
	 *          {@code true}, otherwise non-blocking.
	 */
	@Nullable
	public MediaStatus pause(int mediaSessionId, boolean synchronous) throws IOException {
		return channel.pause(this, mediaSessionId, synchronous);
	}

	/**
	 * Asks the remote application to pause playback of the media referenced by
	 * the specified media session ID.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
	 *
	 * @param mediaSessionId the media session ID for which the pause request
	 *            applies.
	 * @param responseTimeout the response timeout in milliseconds. If zero or
	 *            negative, {@value Channel#DEFAULT_RESPONSE_TIMEOUT} will be
	 *            used.
	 * @return The resulting {@link MediaStatus}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking.
	 */
	@Nonnull
	public MediaStatus pause(int mediaSessionId, long responseTimeout) throws IOException {
		return channel.pause(this, mediaSessionId, responseTimeout);
	}

	/**
	 * Asks the remote application to move the playback position of the media
	 * referenced by the specified media session ID to the specified position,
	 * using {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the timeout value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
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
	 *         {@code true}, {@code null} if {@code synchronous} is
	 *         {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking if {@code synchronous} is
	 *          {@code true}, otherwise non-blocking.
	 */
	@Nullable
	public MediaStatus seek(
		int mediaSessionId,
		double currentTime,
		@Nullable ResumeState resumeState,
		boolean synchronous
	) throws IOException {
		return channel.seek(this, mediaSessionId, currentTime, resumeState, synchronous);
	}

	/**
	 * Asks the remote application to move the playback position of the media
	 * referenced by the specified media session ID to the specified position.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
	 *
	 * @param mediaSessionId the media session ID for which the pause request
	 *            applies.
	 * @param currentTime the new playback position in seconds.
	 * @param resumeState the desired media player state after the seek is
	 *            complete. If {@code null}, it will retain the state it had
	 *            before seeking.
	 * @param responseTimeout the response timeout in milliseconds. If zero or
	 *            negative, {@value Channel#DEFAULT_RESPONSE_TIMEOUT} will be
	 *            used.
	 * @return The resulting {@link MediaStatus}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking.
	 */
	@Nonnull
	public MediaStatus seek(
		int mediaSessionId,
		double currentTime,
		@Nullable ResumeState resumeState,
		long responseTimeout
	) throws IOException {
		return channel.seek(this, mediaSessionId, currentTime, resumeState, responseTimeout);
	}

	/**
	 * Asks the remote application to stop playback and unload the media
	 * referenced by the specified media session ID, using
	 * {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the timeout value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
	 *
	 * @param mediaSessionId the media session ID for which the
	 *            {@link MediaVolume} request applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true}, {@code null} if {@code synchronous} is
	 *         {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking if {@code synchronous} is
	 *          {@code true}, otherwise non-blocking.
	 */
	@Nullable
	public MediaStatus stop(int mediaSessionId, boolean synchronous) throws IOException {
		return channel.stopMedia(this, mediaSessionId, synchronous);
	}

	/**
	 * Asks the remote application to stop playback and unload the media
	 * referenced by the specified media session ID.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
	 *
	 * @param mediaSessionId the media session ID for which the
	 *            {@link MediaVolume} request applies.
	 * @param responseTimeout the response timeout in milliseconds. If zero or
	 *            negative, {@value Channel#DEFAULT_RESPONSE_TIMEOUT} will be
	 *            used.
	 * @return The resulting {@link MediaStatus}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking.
	 */
	@Nonnull
	public MediaStatus stop(int mediaSessionId, long responseTimeout) throws IOException {
		return channel.stopMedia(this, mediaSessionId, responseTimeout);
	}

	/**
	 * Asks the remote application to change the volume level or mute state of
	 * the stream of the specified media session. Please note that this is
	 * different from the device volume level or mute state, and that this will
	 * give the user no visual indication.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
	 *
	 * @param mediaSessionId the media session ID for which the
	 *            {@link MediaVolume} request applies.
	 * @param volume the {@link MediaVolume} to set.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true}, {@code null} if {@code synchronous} is
	 *         {@code false}.
	 * @throws IllegalArgumentException If {@code volume} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking if {@code synchronous} is
	 *          {@code true}, otherwise non-blocking.
	 */
	@Nullable
	public MediaStatus setVolume(
		int mediaSessionId,
		@Nonnull MediaVolume volume,
		boolean synchronous
	) throws IOException {
		return channel.setMediaVolume(this, mediaSessionId, volume, synchronous);
	}

	/**
	 * Requests a list of updated {@link MediaStatus}es from the remote application. This
	 * method is always blocking.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
	 *
	 * @return The resulting {@link List} of {@link MediaStatus}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking.
	 */
	@Nonnull
	public List<MediaStatus> getMediaStatus() throws IOException {
		return channel.getMediaStatus(this);
	}

	/**
	 * Requests a list of updated {@link MediaStatus}es from the remote application. This
	 * method is always blocking.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * {@link CastDevice#CAST_MEDIA_NAMESPACE}.
	 *
	 * @param responseTimeout the response timeout in milliseconds. If zero or
	 *            negative, {@link Channel#DEFAULT_RESPONSE_TIMEOUT} will be
	 *            used.
	 * @return The resulting {@link List} of {@link MediaStatus}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 * @apiNote This operation is blocking.
	 */
	@Nonnull
	public List<MediaStatus> getMediaStatus(long responseTimeout) throws IOException {
		return channel.getMediaStatus(this, responseTimeout);
	}

	/**
	 * Sends the specified {@link Request} with the specified namespace using
	 * this {@link Session}.
	 *
	 * @param <T> the class of the {@link Response} object.
	 * @param namespace the namespace to use.
	 * @param request the {@link Request} to send.
	 * @param responseClass the response class to to block and wait for a
	 *            response or {@code null} to return immediately.
	 * @return The {@link Response} if the response is received in time, or
	 *         {@code null} if the {@code responseClass} is {@code null} or a
	 *         timeout occurs.
	 * @throws IOException If an error occurs during the operation.
	 *
	 * @apiNote This operation is blocking if {@code responseClass} is
	 *          specified, and non-blocking if {@code responseClass} is
	 *          {@code null}.
	 */
	public <T extends Response> T sendGenericRequest(
		String namespace,
		Request request,
		Class<T> responseClass
	) throws IOException {
		return channel.sendGenericRequest(this, namespace, request, responseClass);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		if (sourceId != null) {
			builder.append("sourceId=").append(sourceId).append(", ");
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
