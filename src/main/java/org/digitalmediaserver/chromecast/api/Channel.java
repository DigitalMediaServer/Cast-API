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

import static org.digitalmediaserver.chromecast.api.Util.isBlank;
import static org.digitalmediaserver.chromecast.api.Util.readB32Int;
import static org.digitalmediaserver.chromecast.api.Util.requireNotBlank;
import static org.digitalmediaserver.chromecast.api.Util.requireNotNull;
import static org.digitalmediaserver.chromecast.api.Util.writeB32Int;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import org.digitalmediaserver.chromecast.api.CastChannel.CastMessage;
import org.digitalmediaserver.chromecast.api.CastChannel.CastMessage.PayloadType;
import org.digitalmediaserver.chromecast.api.CastEvent.CastEventListenerList;
import org.digitalmediaserver.chromecast.api.CastEvent.CastEventType;
import org.digitalmediaserver.chromecast.api.CastEvent.DefaultCastEvent;
import org.digitalmediaserver.chromecast.api.StandardMessage.Ping;
import org.digitalmediaserver.chromecast.api.StandardMessage.Pong;
import org.digitalmediaserver.chromecast.api.StandardResponse.AppAvailabilityResponse;
import org.digitalmediaserver.chromecast.api.StandardResponse.InvalidResponse;
import org.digitalmediaserver.chromecast.api.StandardResponse.LaunchErrorResponse;
import org.digitalmediaserver.chromecast.api.StandardResponse.LoadFailedResponse;
import org.digitalmediaserver.chromecast.api.StandardResponse.MediaStatusResponse;
import org.digitalmediaserver.chromecast.api.StandardResponse.ReceiverStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Internal class for low-level communication with cast devices. It's normally
 * desirable to use {@link CastDevice} or {@link Session} methods instead of
 * calling this class directly.
 */
public class Channel implements Closeable {

	private static final Logger LOGGER = LoggerFactory.getLogger(Channel.class);

	/** The logging {@link Marker} used for logging */
	public static final Marker CAST_API_MARKER = MarkerFactory.getMarker("Cast API");

	/** The logging {@link Marker} used for ping logging */
	public static final Marker CAST_API_HEARTBEAT_MARKER = MarkerFactory.getMarker("Cast API Heartbeat");

	/** The standard port used for cast devices */
	public static final int STANDARD_DEVICE_PORT = 8009;

	/** The delay between {@code PING} requests in milliseconds */
	protected static final long PING_PERIOD = 10L * 1000L;

	/** The default response timeout in milliseconds */
	public static final long DEFAULT_RESPONSE_TIMEOUT = 30 * 1000;

	/** Google's fixed receiver ID to use for the cast device itself */
	public static final String PLATFORM_RECEIVER_ID = "receiver-0";

	/**
	 * Google's fixed source ID to use for the "sender platform" (as opposed to
	 * the sender application)
	 */
	public static final String PLATFORM_SENDER_ID = "sender-0";

	/**
	 * An array of what is defined as "standard response" types, to be treated
	 * as immutable
	 */
	protected static final JsonSubTypes.Type[] STANDARD_RESPONSE_TYPES =
		StandardResponse.class.getAnnotation(JsonSubTypes.class).value();

	/** The registered {@link CastEventListener}s */
	@Nonnull
	protected final CastEventListenerList listeners;

	/** The socket synchronization object */
	@Nonnull
	protected final Object socketLock = new Object();

	/**
	 * The {@link Socket} instance use to communicate with the remote device.
	 */
	@Nullable
	@GuardedBy("socketLock")
	protected Socket socket;

	/** The IP address and port of the cast device */
	@Nonnull
	protected final InetSocketAddress address;

	/** The name used for the remote party in logging */
	@Nonnull
	protected final String remoteName;

	/**
	 * The sender id used in this channel
	 */
	@Nonnull
	protected final String sourceId;

	/** {@link Timer} for PING requests */
	@GuardedBy("socketLock")
	protected Timer pingTimer;

	/**
	 * The {@link Thread} that delegates incoming requests to processing
	 * threads
	 */
	@GuardedBy("socketLock")
	protected InputHandler inputHandler;

	/**
	 * Counter for producing request numbers
	 */
	@Nonnull
	protected final AtomicLong requestCounter = new AtomicLong(new Random().nextInt(65536) + 1L);

	/**
	 * Processors of requests by their identifiers
	 */
	@Nonnull
	@GuardedBy("requests")
	protected final Map<Long, ResultProcessor<? extends Response>> requests = new HashMap<>();

	/**
	 * Single mapper object for marshalling JSON
	 */
	@Nonnull
	protected final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();

	/** The sessions synchronization object */
	@Nonnull
	protected final Object sessionsLock = new Object();

	/**
	 * The currently known sessions belonging to this {@link Channel}
	 */
	@Nonnull
	@GuardedBy("sessionsLock")
	protected final Set<String> sessions = new HashSet<>();

	/**
	 * How much time to wait until request is processed
	 */
	protected volatile long requestTimeout = DEFAULT_RESPONSE_TIMEOUT;

	static {
		CAST_API_HEARTBEAT_MARKER.add(CAST_API_MARKER);
	}

	public Channel(
		@Nonnull String host,
		@Nonnull String remoteName,
		@Nullable String sourceId,
		@Nonnull CastEventListenerList listeners
	) {
		this(host, STANDARD_DEVICE_PORT, remoteName, sourceId, listeners);
	}

	public Channel(
		@Nonnull String host,
		int port,
		@Nonnull String remoteName,
		@Nullable String sourceId,
		@Nonnull CastEventListenerList listeners
	) {
		requireNotBlank(host, "host");
		requireNotBlank(remoteName, "remoteName");
		requireNotBlank(sourceId, "sourceId");
		requireNotNull(listeners, "listeners");
		this.address = new InetSocketAddress(host, port);
		this.remoteName = remoteName;
		this.sourceId = sourceId;
		this.listeners = listeners;
	}

	/**
	 * Establishes a connection to the associated remote cast device.
	 *
	 * @return {@code true} if a connection was established, {@code false} if
	 *         there was no need.
	 *
	 * @throws KeyManagementException If there's a problem with key management
	 *             that prevents connection.
	 * @throws NoSuchAlgorithmException If the required cryptographic algorithm
	 *             isn't available in the JVM.
	 * @throws CastException If there was an authentication problem with the
	 *             cast device.
	 * @throws IOException If an error occurs during the operation.
	 */
	public boolean connect() throws IOException, NoSuchAlgorithmException, KeyManagementException {
		synchronized (socketLock) {
			if (socket != null && socket.isConnected() && !socket.isClosed()) {
				// Already connected, nothing to do
				return false;
			}

			if (socket != null) {
				socket.close();
				socket = null;
			}
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, new TrustManager[] {new X509TrustAllManager()}, new SecureRandom());
			socket = sc.getSocketFactory().createSocket();
			socket.setSoTimeout(0);
			socket.connect(address);

			// Authenticate
			CastChannel.DeviceAuthMessage authMessage = CastChannel.DeviceAuthMessage.newBuilder()
				.setChallenge(CastChannel.AuthChallenge.newBuilder().build())
				.build();

			CastMessage msg = CastMessage.newBuilder()
				.setDestinationId(PLATFORM_RECEIVER_ID)
				.setNamespace("urn:x-cast:com.google.cast.tp.deviceauth")
				.setPayloadType(CastMessage.PayloadType.BINARY)
				.setProtocolVersion(CastMessage.ProtocolVersion.CASTV2_1_0)
				.setSourceId(PLATFORM_SENDER_ID)
				.setPayloadBinary(authMessage.toByteString())
				.build();

			write(msg);
			CastMessage response = readMessage(socket.getInputStream());
			CastChannel.DeviceAuthMessage authResponse = CastChannel.DeviceAuthMessage.parseFrom(response.getPayloadBinary());
			if (authResponse.hasError()) {
				throw new CastException("Authentication failed: " + authResponse.getError().getErrorType().toString());
			}

			// Start input handler
			inputHandler = new InputHandler(socket.getInputStream());
			inputHandler.start();

			// Send 'CONNECT' message to start session
			write(
				"urn:x-cast:com.google.cast.tp.connection",
				StandardMessage.connect(),
				PLATFORM_RECEIVER_ID
			);

			// Start regular pinging
			PingTask pingTask = new PingTask();
			pingTimer = new Timer(remoteName + " PING timer");
			pingTimer.schedule(pingTask, 1000, PING_PERIOD);
		}

		// Send connect event
		listeners.fire(new DefaultCastEvent<>(CastEventType.CONNECTED, Boolean.TRUE));
		return true;
	}

	/**
	 * Closes this {@link Channel} and any {@link Session}s belonging to it. If
	 * this {@link Channel} is already closed, this is a no-op.
	 *
	 * @throws IOException If an error occurs during the operation.
	 */
	@Override
	public void close() throws IOException {
		synchronized (sessionsLock) {
			synchronized (socketLock) {
				if (socket == null || socket.isClosed() || !socket.isConnected()) {
					// Already closed
					return;
				}

				if (!sessions.isEmpty()) {
					sessions.clear();
				}

				if (pingTimer != null) {
					pingTimer.cancel();
					pingTimer = null;
				}

				if (inputHandler != null) {
					inputHandler.stopProcessing();
					inputHandler = null;
				}

				socket.close();
				socket = null;
			}
		}

		// Send disconnect event
		listeners.fire(new DefaultCastEvent<>(CastEventType.CONNECTED, Boolean.FALSE));
	}

	/**
	 * @return {@code true} if this {@link Channel} is closed, {@code false}
	 *         if it's open.
	 */
	public boolean isClosed() {
		synchronized (socketLock) {
			return socket == null || socket.isClosed() || !socket.isConnected();
		}
	}

	public <T extends Response> T send(
		String namespace,
		Request message,
		String destinationId,
		Class<T> responseClass
	) throws IOException {
		Long requestId = requestCounter.getAndIncrement();
		message.setRequestId(requestId);
		if (!requestId.equals(message.getRequestId())) {
			throw new IllegalStateException("Request Id getter/setter contract violation");
		}

		if (responseClass == null) {
			write(namespace, message, destinationId);
			return null;
		}

		ResultProcessor<T> rp = new ResultProcessor<>(responseClass);
		synchronized (requests) {
			requests.put(requestId, rp);
		}

		write(namespace, message, destinationId);
		try {
			T response = rp.get();
			if (response instanceof InvalidResponse) {
				InvalidResponse invalid = (InvalidResponse) response;
				throw new CastException("Invalid request: " + invalid.getReason());
			} else if (response instanceof LoadFailedResponse) {
				throw new CastException("Unable to load media");
			} else if (response instanceof LaunchErrorResponse) {
				LaunchErrorResponse launchError = (LaunchErrorResponse) response;
				throw new CastException("Application launch error: " + launchError.getReason());
			}
			return response;
		} catch (InterruptedException e) {
			throw new CastException("Interrupted while waiting for response", e);
		} catch (TimeoutException e) {
			throw new CastException("Waiting for response timed out", e);
		} finally {
			synchronized (requests) {
				requests.remove(requestId);
			}
		}
	}

	public ReceiverStatus getReceiverStatus() throws IOException {
		ReceiverStatusResponse status = send(
			"urn:x-cast:com.google.cast.receiver",
			StandardRequest.status(),
			PLATFORM_RECEIVER_ID,
			ReceiverStatusResponse.class
		);
		return status == null ? null : status.getStatus();
	}

	/**
	 * Queries the cast device if the application represented by the specified
	 * application ID is available, using {@value #DEFAULT_RESPONSE_TIMEOUT} as
	 * the timeout value.
	 *
	 * @param applicationId the application ID for which to query availability.
	 * @return {@code true} if the application is available, {@code false} if
	 *         it's not.
	 * @throws IOException If the response times out or if an error occurs
	 *             during the operation.
	 */
	public boolean isApplicationAvailable(String applicationId) throws IOException {
		AppAvailabilityResponse availability = send(
			"urn:x-cast:com.google.cast.receiver",
			StandardRequest.getAppAvailability(applicationId),
			PLATFORM_RECEIVER_ID,
			AppAvailabilityResponse.class
		);
		return availability != null && "APP_AVAILABLE".equals(availability.getAvailability().get(applicationId));
	}

	@Nullable
	public ReceiverStatus launch(String applicationId) throws IOException {
		ReceiverStatusResponse status = send(
			"urn:x-cast:com.google.cast.receiver",
			StandardRequest.launch(applicationId),
			PLATFORM_RECEIVER_ID,
			ReceiverStatusResponse.class
		);
		return status == null ? null : status.getStatus();
	}

	public ReceiverStatus stop(@Nonnull String sessionId) throws IOException {
		ReceiverStatusResponse status = send(
			"urn:x-cast:com.google.cast.receiver",
			StandardRequest.stop(sessionId),
			PLATFORM_RECEIVER_ID,
			ReceiverStatusResponse.class
		);
		return status == null ? null : status.getStatus();
	}

	protected void startSession(String destinationId) throws IOException {
		if (!sessions.contains(destinationId)) {
			write("urn:x-cast:com.google.cast.tp.connection", StandardMessage.connect(), destinationId);
			sessions.add(destinationId);
		}
	}

	/**
	 * Asks the targeted remote application to load the specified {@link Media}
	 * using the specified parameters.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param destinationId the destination ID to use.
	 * @param sessionId the session ID to use.
	 * @param media the {@link Media} to load.
	 * @param autoplay {@code true} to ask the remote application to start
	 *            playback as soon as the {@link Media} has been loaded,
	 *            {@code false} to ask it to transition to a paused state after
	 *            loading.
	 * @param currentTime the position in seconds where playback are to be
	 *            started in the loaded {@link Media}.
	 * @param customData the custom application data to send to the remote
	 *            application with the load command.
	 * @return The resulting {@link MediaStatus}.
	 * @throws IOException If an error occurs during the operation.
	 */
	@Nullable
	public MediaStatus load(
		@Nonnull String destinationId,
		@Nonnull String sessionId,
		Media media,
		boolean autoplay,
		double currentTime,
		@Nullable Map<String, String> customData
	) throws IOException {
		startSession(destinationId);
		MediaStatusResponse status = send(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.load(sessionId, media, autoplay, currentTime, customData),
			destinationId,
			MediaStatusResponse.class
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	/**
	 * Asks the remote application to start playing the media referenced by the
	 * specified media session ID, using {@value #DEFAULT_RESPONSE_TIMEOUT} as
	 * the timeout value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param destinationId the destination ID to use.
	 * @param sessionId the session ID to use.
	 * @param mediaSessionId the media session ID for which the play request
	 *            applies.
	 * @return The resulting {@link MediaStatus}.
	 * @throws IOException If an error occurs during the operation.
	 */
	@Nullable
	public MediaStatus play(
		@Nonnull String destinationId,
		@Nonnull String sessionId,
		long mediaSessionId
	) throws IOException {
		startSession(destinationId);
		MediaStatusResponse status = send(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.play(sessionId, mediaSessionId),
			destinationId,
			MediaStatusResponse.class
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	/**
	 * Asks the remote application to pause playback of the media referenced by
	 * the specified media session ID, using {@value #DEFAULT_RESPONSE_TIMEOUT}
	 * as the timeout value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param destinationId the destination ID to use.
	 * @param sessionId the session ID to use.
	 * @param mediaSessionId the media session ID for which the pause request
	 *            applies.
	 * @return The resulting {@link MediaStatus}.
	 * @throws IOException If an error occurs during the operation.
	 */
	@Nullable
	public MediaStatus pause(
		@Nonnull String destinationId,
		@Nonnull String sessionId,
		long mediaSessionId
	) throws IOException {
		startSession(destinationId);
		MediaStatusResponse status = send(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.pause(sessionId, mediaSessionId),
			destinationId,
			MediaStatusResponse.class
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	/**
	 * Asks the remote application to move the playback position of the media
	 * referenced by the specified media session ID to the specified position.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param destinationId the destination ID to use.
	 * @param sessionId the session ID to use.
	 * @param mediaSessionId the media session ID for which the pause request
	 *            applies.
	 * @param currentTime the new playback position in seconds.
	 * @return The resulting {@link MediaStatus}.
	 * @throws IOException If an error occurs during the operation.
	 */
	@Nullable
	public MediaStatus seek(
		@Nonnull String destinationId,
		@Nonnull String sessionId,
		long mediaSessionId,
		double currentTime
	) throws IOException {
		startSession(destinationId);
		MediaStatusResponse status = send(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.seek(sessionId, mediaSessionId, currentTime),
			destinationId,
			MediaStatusResponse.class
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	@Nullable
	public ReceiverStatus setVolume(Volume volume) throws IOException {
		ReceiverStatusResponse status = send(
			"urn:x-cast:com.google.cast.receiver",
			StandardRequest.setVolume(volume),
			PLATFORM_RECEIVER_ID,
			ReceiverStatusResponse.class
		);
		return status == null ? null : status.getStatus();
	}

	@Nullable
	public MediaStatus getMediaStatus(@Nonnull String destinationId) throws IOException {
		startSession(destinationId);
		MediaStatusResponse status = send(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.status(),
			destinationId,
			MediaStatusResponse.class
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	public <T extends Response> T sendGenericRequest(
		@Nonnull String destinationId,
		@Nonnull String namespace,
		Request request,
		Class<T> responseClass
	) throws IOException {
		startSession(destinationId);
		return send(namespace, request, destinationId, responseClass);
	}

	public void setRequestTimeout(long requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	/**
	 * Claims (retrieves and removes from {@code requests}) the
	 * {@link ResultProcessorResult} for the specified request ID if one exists.
	 *
	 * @param requestId the request ID to look up.
	 * @return The one and only {@link ResultProcessor} instance for the
	 *         specified request ID if it exists and hasn't already been
	 *         claimed, or {@code null}.
	 */
	@Nullable
	protected ResultProcessor<? extends Response> acquireResultProcessor(long requestId) {
		if (requestId < 1L) {
			return null;
		}
		synchronized (requests) {
			return requests.remove(Long.valueOf(requestId));
		}
	}

	/**
	 * Writes the specified {@link Message} to the socket using the specified
	 * parameters.
	 *
	 * @param namespace the namespace to use.
	 * @param message the {@link Message} to write.
	 * @param destinationId the destination ID to use.
	 * @throws IOException If an error occurs during the operation.
	 */
	protected void write(String namespace, Message message, String destinationId) throws IOException {
		write(namespace, jsonMapper.writeValueAsString(message), destinationId);
	}

	/**
	 * Writes the specified ({@code JSON} formatted) {@link String} to the
	 * socket using the specified parameters.
	 *
	 * @param namespace the namespace to use.
	 * @param message the message content to write.
	 * @param destinationId the destination ID to use.
	 * @throws IOException If an error occurs during the operation.
	 */
	protected void write(String namespace, String message, String destinationId) throws IOException {
		LOGGER.debug(
			CAST_API_MARKER,
			"Sending message to {} with namespace '{}': \"{}\"",
			remoteName,
			namespace,
			message
		);
		CastMessage msg = CastMessage.newBuilder()
			.setProtocolVersion(CastMessage.ProtocolVersion.CASTV2_1_0)
			.setSourceId(sourceId)
			.setDestinationId(destinationId)
			.setNamespace(namespace)
			.setPayloadType(CastMessage.PayloadType.STRING)
			.setPayloadUtf8(message)
			.build();
		write(msg);
	}

	/**
	 * Writes the specified {@link CastMessage} to the socket.
	 *
	 * @param message the {@link CastMessage} to write.
	 * @throws IOException If an error occurs during the operation.
	 */
	protected void write(CastMessage message) throws IOException {
		OutputStream os;
		synchronized (socketLock) {
			if (socket == null) {
				throw new SocketException("Socket is null");
			}
			os = socket.getOutputStream();
			// Include in synchronized section to force serialization of outgoing messages
			writeB32Int(message.getSerializedSize(), os);
			message.writeTo(os);
		}
	}

	/**
	 * Reads the next {@link CastMessage} from the specified {@link InputStream}
	 * in a blocking fashion. This method will not return until a message is
	 * either completely read, {@code EOF} is reached or an {@link IOException}
	 * is thrown.
	 *
	 * @param inputStream the {@link InputStream} from which to read.
	 * @return The resulting {@link CastMessage}.
	 * @throws IOException If {@code EOF} is reached or an error occurs-.
	 */
	@Nonnull
	protected static CastMessage readMessage(InputStream inputStream) throws IOException {
		int size = readB32Int(inputStream);
		byte[] buf = new byte[size];
		int read = 0;
		while (read < size) {
			int readNow = inputStream.read(buf, read, buf.length - read);
			if (readNow == -1) {
				throw new CastException("Incomplete message, ended after reading " + read + " of " + size + " bytes");
			}
			read += readNow;
		}
		return CastMessage.parseFrom(buf);
	}

	/**
	 * Determines if the message referenced by the specified {@link JsonNode} is
	 * among the "standard responses" by looking at the {@code type} field
	 * exclusively.
	 *
	 * @param parsedMessage the {@link JsonNode} referencing a parsed, received
	 *            message.
	 * @return {@code true} if the message is deemed not to be among the
	 *         "standard responses", {@code false} if it is.
	 */
	protected static boolean isCustomMessage(@Nullable JsonNode parsedMessage) {
		if (parsedMessage == null) {
			return true;
		}
		if (parsedMessage.has("responseType")) {
			String parsedType = parsedMessage.get("responseType").asText();
			for (JsonSubTypes.Type type : STANDARD_RESPONSE_TYPES) {
				if (type.name().equals(parsedType)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Validates that the specified namespace conforms to some very basic
	 * constraints.
	 *
	 * @param namespace the namespace to validate.
	 * @throws IllegalArgumentException If the validation fails.
	 */
	public static void validateNamespace(@Nonnull String namespace) throws IllegalArgumentException {
		requireNotBlank(namespace, "namespace");
		if (namespace.length() > 128) {
			throw new IllegalArgumentException("Invalid namespace length " + namespace.length());
		} else if (!namespace.startsWith("urn:x-cast:")) {
			throw new IllegalArgumentException("Namespace must begin with the prefix \"urn:x-cast:\"");
		} else if (namespace.length() == 11) {
			throw new IllegalArgumentException(
				"Namespace must begin with the prefix \"urn:x-cast:\" and have non-empty suffix"
			);
		}
	}

	/**
	 * A {@link TimerTask} that will send {@code PING} messages to the cast
	 * device upon execution.
	 *
	 * @author Nadahar
	 */
	protected class PingTask extends TimerTask {

		/** The {@link Ping} message */
		protected final CastMessage message;

		/**
		 * Creates a new instance.
		 *
		 * @throws AssertionError If the {@code JSON} mapper can't serialize the
		 *             {@code PING} message.
		 */
		public PingTask() {
			String messageString;
			try {
				messageString = jsonMapper.writeValueAsString(new Ping());
			} catch (JsonProcessingException e) {
				throw new AssertionError("Couldn't generate JSON for 'PING' message");
			}
			message = CastMessage.newBuilder()
				.setProtocolVersion(CastMessage.ProtocolVersion.CASTV2_1_0)
				.setSourceId(PLATFORM_SENDER_ID)
				.setDestinationId(PLATFORM_RECEIVER_ID)
				.setNamespace("urn:x-cast:com.google.cast.tp.heartbeat")
				.setPayloadType(CastMessage.PayloadType.STRING)
				.setPayloadUtf8(messageString)
				.build();
		}

		@Override
		public void run() {
			if (LOGGER.isTraceEnabled(CAST_API_HEARTBEAT_MARKER)) {
				LOGGER.trace(CAST_API_HEARTBEAT_MARKER, "Pinging {}", remoteName);
			}
			try {
				write(message);
			} catch (IOException e) {
				LOGGER.warn(
					CAST_API_MARKER,
					"An error occurred while sending 'PING' to {}: {}",
					remoteName,
					e.getMessage()
				);
				LOGGER.trace(CAST_API_MARKER, "", e);
			}
		}
	}

	protected class InputHandler extends Thread {

		/** The "running" state */
		protected volatile boolean running;

		/** The {@link InputStream} to process */
		@Nonnull
		protected final InputStream is;

		/** The cached {@link Pong} message */
		@Nonnull
		protected final CastMessage pongMessage;

		/**
		 * Creates a new instance bound to the specified {@link InputStream}.
		 *
		 * @param inputStream the {@link InputStream} to process.
		 */
		public InputHandler(@Nonnull InputStream inputStream) {
			super(remoteName + " input handler");
			requireNotNull(inputStream, "inputStream");
			this.is = inputStream;

			String messageString;
			try {
				messageString = jsonMapper.writeValueAsString(new Pong());
			} catch (JsonProcessingException e) {
				throw new AssertionError("Couldn't generate JSON for 'PONG' message");
			}
			pongMessage = CastMessage.newBuilder()
				.setProtocolVersion(CastMessage.ProtocolVersion.CASTV2_1_0)
				.setSourceId(PLATFORM_SENDER_ID)
				.setDestinationId(PLATFORM_RECEIVER_ID)
				.setNamespace("urn:x-cast:com.google.cast.tp.heartbeat")
				.setPayloadType(CastMessage.PayloadType.STRING)
				.setPayloadUtf8(messageString)
				.build();
			this.running = true;
		}

		@Override
		public void run() {
			String jsonMessage;
			CastMessage message = null;
			PayloadType payloadType;
			try {
				while (running) {
					message = null;
					try {
						message = readMessage(is);
					} catch (SocketTimeoutException e) {
						if (running) {
							LOGGER.debug(
								CAST_API_MARKER,
								"{} InputHandler: Received an unexpected SocketTimeoutException - attempting to resume: {}",
								remoteName,
								e.getMessage()
							);
						continue;
						} else {
							break;
						}
					}
					if (message != null && (payloadType = message.getPayloadType()) != null) {
						switch (payloadType) {
							case BINARY:
								LOGGER.trace(
									CAST_API_MARKER,
									"{} InputHandler: Received message with binary payload ({} bytes)",
									remoteName,
									message.getPayloadBinary() == null ? "unknown number of" : message.getPayloadBinary().size()
								);
								listeners.fire(new DefaultCastEvent<>(CastEventType.CUSTOM_MESSAGE, new CustomMessageEvent(
									message.getSourceId(),
									message.getDestinationId(),
									message.getNamespace(),
									message.getPayloadBinary()
								)));
								break;
							case STRING:
								jsonMessage = message.getPayloadUtf8();
								if (isBlank(jsonMessage)) {
									LOGGER.trace(
										CAST_API_MARKER,
										"{} InputHandler: Received an empty string message - ignoring",
										remoteName
									);
									continue;
								}
								jsonMessage = jsonMessage.replaceFirst("\"type\"", "\"responseType\"");
								if ("urn:x-cast:com.google.cast.tp.heartbeat".equals(message.getNamespace())) {
									// Deal with PING/PONG directly
									JsonNode parsedMessage = jsonMapper.readTree(jsonMessage);
									JsonNode tmpNode = parsedMessage.get("responseType");
									String responseType = tmpNode == null ? "" : tmpNode.asText("");
									if ("PING".equals(responseType)) {
										LOGGER.trace(
											CAST_API_HEARTBEAT_MARKER,
											"Received PING from {}, replying with PONG",
											remoteName
										);
										write(pongMessage);
									} else if ("PONG".equals(responseType)) {
										LOGGER.trace(CAST_API_HEARTBEAT_MARKER, "Received PONG from {}", remoteName);
									} else {
										LOGGER.trace(
											CAST_API_HEARTBEAT_MARKER,
											"Received unexpected heartbeat message of type \"{}\" from {}",
											responseType,
											remoteName
										);
									}
									continue;
								}
								LOGGER.trace(
									CAST_API_MARKER,
									"{} InputHandler: Received string message \"{}\"",
									remoteName,
									jsonMessage
								);
								processStringMessage(message, jsonMessage);
								break;
							default:
								LOGGER.warn(
									CAST_API_MARKER,
									"{} InputHandler: Received a message with an unknown payload type '{}'",
									remoteName,
									payloadType
								);
								break;

						}
					} else if (message != null) {
						LOGGER.warn(
							CAST_API_MARKER,
							"{} InputHandler: Received a message without a payload type",
							remoteName
						);
					} else {
						LOGGER.warn(
							CAST_API_MARKER,
							"{} InputHandler: Received a null message",
							remoteName
						);
					}
				}
			} catch (IOException e) {
				if (running) {
					LOGGER.error(CAST_API_MARKER, "{} InputHandler exception, terminating handler: ", remoteName, e.getMessage());
					if (message != null && LOGGER.isDebugEnabled(CAST_API_MARKER)) {
						StringBuilder sb = new StringBuilder();
						sb.append("namespace: ").append(message.getNamespace());
						sb.append(", protocol version: ").append(message.getProtocolVersion().getNumber());
						if (message.hasPayloadUtf8()) {
							sb.append(", string payload: ").append(message.getPayloadUtf8());
						}
						if (message.hasPayloadBinary()) {
							sb.append(", binary payload: ").append(message.getPayloadBinary());
						}
						LOGGER.debug(CAST_API_MARKER, "Triggering (potentially partial) message: {}", sb.toString());
					}
					LOGGER.trace(CAST_API_MARKER, "", e);
					running = false;
				} else {
					LOGGER.trace(
						CAST_API_MARKER,
						"Exception while shutting down {} InputHandler: {}",
						remoteName,
						e.getMessage()
					);
				}
				try {
					close();
				} catch (IOException ioe) {
					LOGGER.debug(
						CAST_API_MARKER,
						"An error occurred while closing {} socket: {}",
						remoteName,
						e.getMessage()
					);
				}
			}
		}

		/**
		 * Processes a single incoming string-based message from the specified
		 * parameters.
		 *
		 * @param message the {@link CastMessage} to process.
		 * @param jsonMessage the adapted string payload to use.
		 */
		protected void processStringMessage(@Nonnull CastMessage message, @Nonnull String jsonMessage) {
			try {
				JsonNode parsedMessage = jsonMapper.readTree(jsonMessage);
				String responseType;
				long requestId;
				if (parsedMessage == null) {
					responseType = "";
					requestId = -1L;
				} else {
					JsonNode tmpNode = parsedMessage.get("responseType");
					responseType = tmpNode == null ? "" : tmpNode.asText("");
					tmpNode = parsedMessage.get("requestId");
					requestId = tmpNode == null ? -1L : tmpNode.asLong(-1L);
				}
				ResultProcessor<? extends Response> resultProcessor;
				if (requestId > 0L && (resultProcessor = acquireResultProcessor(requestId)) != null) {
					resultProcessor.process(jsonMessage);
				} else if (parsedMessage == null || isCustomMessage(parsedMessage)) {
					listeners.fire(new DefaultCastEvent<>(
						CastEventType.CUSTOM_MESSAGE,
						new CustomMessageEvent(
							message.getSourceId(),
							message.getDestinationId(),
							message.getNamespace(),
							message.getPayloadUtf8()
						)
					));
				} else if ("CLOSE".equals(responseType)) {
					try {
						close();
					} catch (IOException e) {
						LOGGER.debug(
							CAST_API_MARKER,
							"An error occurred while closing {} socket: {}",
							remoteName,
							e.getMessage()
						);
					}
					if (!listeners.isEmpty()) {
						listeners.fire(new DefaultCastEvent<>(CastEventType.CLOSE, jsonMapper.treeToValue(parsedMessage, StandardResponse.class)));
					}
				} else if (!listeners.isEmpty()) {
					StandardResponse response;
					if (!isBlank(responseType)) {
						try {
							response = jsonMapper.treeToValue(parsedMessage, StandardResponse.class);
						} catch (JsonMappingException e) {
							response = null;
						}
					} else {
						response = null;
					}

					if (response instanceof StandardResponse && response.getEventType() != null) {
						listeners.fire(new DefaultCastEvent<>(response.getEventType(), response));
					} else {
						LOGGER.error(
							CAST_API_MARKER,
							"Received unhandled \"{}\" message from {}, this should not happen: {}",
							responseType,
							remoteName,
							parsedMessage
						);
						listeners.fire(new DefaultCastEvent<>(CastEventType.UNKNOWN, parsedMessage));
					}
				}
			} catch (JsonProcessingException e) {
				LOGGER.warn(
					CAST_API_MARKER,
					"Error while processing JSON message from {}: {}",
					remoteName,
					e.getMessage()
				);
				LOGGER.trace(CAST_API_MARKER, "", e);
			}
		}

		/**
		 * Tells this {@link InputHandler} to stop processing and shut down.
		 */
		public void stopProcessing() {
			running = false;
		}
	}

	/**
	 * Internal class used to tie responses to requests based on request IDs.
	 *
	 * @param <T> the response type.
	 */
	protected class ResultProcessor<T extends Response> {

		/** The expected response {@link Class} */
		protected final Class<T> responseClass;
		protected T result;

		protected ResultProcessor(Class<T> responseClass) {
			if (responseClass == null) {
				throw new NullPointerException();
			}
			this.responseClass = responseClass;
		}

		/**
		 * Processes the specified message that has been routed to this
		 * {@link ResultProcessor} by its request ID.
		 *
		 * @param jsonMSG the message content formatted as JSON.
		 * @throws JsonMappingException If the JSON mapping fails.
		 * @throws JsonProcessingException If the JSON can't be processed.
		 */
		public void process(String jsonMSG) throws JsonMappingException, JsonProcessingException {
			synchronized (this) {
				this.result = jsonMapper.readValue(jsonMSG, responseClass);
				this.notify();
			}
		}

		public T get() throws InterruptedException, TimeoutException {
			synchronized (this) {
				if (result != null) {
					return result;
				}
				this.wait(requestTimeout);
				if (result == null) {
					throw new TimeoutException();
				}
				return result;
			}
		}
	}
}
