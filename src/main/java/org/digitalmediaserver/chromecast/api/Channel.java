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
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.digitalmediaserver.chromecast.api.ChromeCastException.ErrorResponseChromeCastException;
import org.digitalmediaserver.chromecast.api.ChromeCastException.LaunchErrorCastException;
import org.digitalmediaserver.chromecast.api.ChromeCastException.UnprocessedChromeCastException;
import org.digitalmediaserver.chromecast.api.ChromeCastException.UntypedChromeCastException;
import org.digitalmediaserver.chromecast.api.Session.SessionClosedListener;
import org.digitalmediaserver.chromecast.api.StandardRequest.ResumeState;
import org.digitalmediaserver.chromecast.api.StandardResponse.AppAvailabilityResponse;
import org.digitalmediaserver.chromecast.api.StandardResponse.ErrorResponse;
import org.digitalmediaserver.chromecast.api.StandardResponse.LaunchErrorResponse;
import org.digitalmediaserver.chromecast.api.StandardResponse.MediaStatusResponse;
import org.digitalmediaserver.chromecast.api.StandardResponse.ReceiverStatusResponse;
import org.digitalmediaserver.chromecast.api.Volume.VolumeControlType;
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
 * Internal class for low-level communication with ChromeCast device. It's
 * normally desirable to use {@link ChromeCast} or {@link Session} methods
 * instead of calling this class directly.
 */
public class Channel implements Closeable {

	private static final Logger LOGGER = LoggerFactory.getLogger(Channel.class);
	public static final Marker CHROMECAST_API_MARKER = MarkerFactory.getMarker("chromecast-api"); //TODO: (Nad) Use Marker everywhere..

	/** The delay between {@code PING} requests in milliseconds */
	protected static final long PING_PERIOD = 10 * 1000; //TODO: (Nad) 5 sec suggested in doc, was 30

	/** The default response timeout in milliseconds */
	public static final long DEFAULT_RESPONSE_TIMEOUT = 30 * 1000;

	public static final String PLATFORM_RECEIVER_ID = "receiver-0";
	public static final String PLATFORM_SENDER_ID = "sender-0";

	protected static final JsonSubTypes.Type[] STANDARD_RESPONSE_TYPES =
		StandardResponse.class.getAnnotation(JsonSubTypes.class).value();

	@Nonnull
	protected static final Executor EXECUTOR = createExecutor();

	@Nonnull
	protected final CastEventListenerList listeners;

	@Nonnull
	protected final Object socketLock = new Object();

	/**
	 * The {@link Socket} instance use to communicate with the remote device.
	 */
	@Nullable
	@GuardedBy("socketLock")
	protected Socket socket;

	/**
	 * Address of ChromeCast
	 */
	@Nonnull
	protected final InetSocketAddress address;

	/** The name used for the remote party in logging */
	@Nonnull
	protected final String remoteName;

	/**
	 * Timer for PING requests
	 */
	@GuardedBy("socketLock")
	protected Timer pingTimer;

	/**
	 * The {@link Thread} that delegates incoming requests to processing threads.
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

	@Nonnull
	protected final Object sessionsLock = new Object();

	/**
	 * Destination ids of sessions opened within this channel
	 */
	@Nonnull
	@GuardedBy("sessionsLock")
	protected final Set<Session> sessions = new HashSet<>();

	@Nonnull
	protected final Object cachedVolumeLock = new Object();

	/** The last received {@link Volume} instance */
	@Nullable
	@GuardedBy("cachedVolumeLock")
	protected Volume cachedVolume;

	@Nonnull
	protected final Object gradualVolumeLock = new Object();

	/** The {@link Timer} used to handle gradual volume change */
	@Nullable
	@GuardedBy("cachedVolumeLock")
	protected Timer gradualVolumeTimer;

	/** The {@link TimerTask} that executes the gradual volume change */
	@Nullable
	@GuardedBy("cachedVolumeLock")
	protected TimerTask gradualVolumeTask;

	public Channel(
		@Nonnull String host,
		@Nonnull String remoteName,
		@Nonnull CastEventListenerList listeners
	) {
		this(host, 8009, remoteName, listeners);
	}

	public Channel(
		@Nonnull String host,
		int port,
		@Nonnull String remoteName,
		@Nonnull CastEventListenerList listeners
	) {
		requireNotBlank(host, "host");
		requireNotBlank(remoteName, "remoteName");
		requireNotNull(listeners, "listeners");
		this.address = new InetSocketAddress(host, port);
		this.remoteName = remoteName;
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
	 * @throws ChromeCastException If there was an authentication problem with
	 *             the cast device.
	 * @throws IOException If an error occurs during the operation.
	 */
	public boolean connect() throws IOException, NoSuchAlgorithmException, KeyManagementException {
		synchronized (socketLock) {
			if (socket != null && socket.isConnected() && !socket.isClosed()) {
				// Already connected, nothing to do
				return false;
			}

			if (socket == null || socket.isClosed()) {
				SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, new TrustManager[] {new X509TrustAllManager()}, new SecureRandom());
				socket = sc.getSocketFactory().createSocket();
				socket.setSoTimeout(0);
				socket.connect(address);
			}

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
				throw new ChromeCastException("Authentication failed: " + authResponse.getError().getErrorType().toString());
			}

			// Start input handler
			inputHandler = new InputHandler(socket.getInputStream());
			inputHandler.start();

			// Send 'CONNECT' message to start session
			write(
				"urn:x-cast:com.google.cast.tp.connection",
				StandardMessage.connect(null, VirtualConnectionType.STRONG),
				PLATFORM_SENDER_ID,
				PLATFORM_RECEIVER_ID
			);

			// Start regular pinging
			PingTask pingTask = new PingTask();
			pingTimer = new Timer(remoteName + " PING timer");
			pingTimer.schedule(pingTask, 1000, PING_PERIOD);
		}

		// Send connect event
		if (!listeners.isEmpty()) {
			EXECUTOR.execute(new Runnable() {

				@Override
				public void run() {
					listeners.fire(new DefaultCastEvent<>(CastEventType.CONNECTED, Boolean.TRUE));
				}
			});
		}

		return true;
	}

	/**
	 * Closes this {@link Channel} and any {@link Session}s belonging to it. If
	 * this {@link Channel} is already closed, it simply returns.
	 *
	 * @throws IOException If an error occurs during the operation.
	 */
	@Override
	public void close() throws IOException {
		Set<Session> closedSessions = null;
		synchronized (socketLock) {
			if (socket == null || socket.isClosed() || !socket.isConnected()) {
				// Already closed
				return;
			}

			synchronized (sessionsLock) {
				if (!sessions.isEmpty()) {
					closedSessions = new HashSet<>(sessions);
					sessions.clear();
				}
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
		}

		if (closedSessions != null) {
			SessionClosedListener closedListener;
			for (Session session : closedSessions) {
				closedListener = session.getSessionClosedListener();
				if (closedListener != null) {
					closedListener.closed(session);
				}
			}
		}

		// Send disconnect event
		if (!listeners.isEmpty()) {
			EXECUTOR.execute(new Runnable() {

				@Override
				public void run() {
					listeners.fire(new DefaultCastEvent<>(CastEventType.CONNECTED, Boolean.FALSE));
				}
			});
		}
	}

	/**
	 * @return {@code true} if this {@link Channel} is closed, {@code false}
	 *         otherwise.
	 */
	public boolean isClosed() {
		synchronized (socketLock) {
			return socket == null || socket.isClosed() || !socket.isConnected();
		}
	}

	/**
	 * Sends the specified {@link Request} to the specified destination using
	 * the specified parameters.
	 *
	 * @param <T> the class of the {@link Response} object.
	 * @param namespace the namespace to use.
	 * @param message the {@link Request} to send.
	 * @param senderId the sender ID to use.
	 * @param destinationId the destination ID to use.
	 * @param responseClass the class of the expected response for synchronous
	 *            (blocking) behavior, or {@code null} for asynchronous behavior
	 *            that returns {@code null} immediately.
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code responseClass} is non-{@code null}. If zero or
	 *            negative, {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The {@link Response} object of the specified type if
	 *         {@code responseClass} is non-{@code null}, {@code null} if
	 *         {@code responseClass} is {@code null}.
	 * @throws IOException If {@code responseClass} is non-{@code null} and the
	 *             response from the cast device a different type than what was
	 *             expected, or if an error occurs during the operation.
	 */
	public <T extends Response> T send(
		String namespace,
		Request message,
		String senderId,
		String destinationId,
		Class<T> responseClass,
		long responseTimeout
	) throws IOException {
		Long requestId = requestCounter.getAndIncrement();
		message.setRequestId(requestId);
		if (!requestId.equals(message.getRequestId())) {
			throw new IllegalStateException("Request Id getter/setter contract violation"); //TODO: (Nad) Runtime.. bad?
		}

		if (responseClass == null) {
			write(namespace, message, senderId, destinationId);
			return null;
		}

		ResultProcessor<T> rp = new ResultProcessor<>(responseClass, responseTimeout);
		synchronized (requests) {
			requests.put(requestId, rp);
		}

		write(namespace, message, senderId, destinationId);
		try {
			ResultProcessorResult<T> response = rp.get();
			if (response.typedResult != null) {
				return response.typedResult;
			}
			if (response.untypedResult instanceof ErrorResponse) {
				throw new ErrorResponseChromeCastException(
					"Cast device returned an error: " + response.untypedResult,
					(ErrorResponse) response.untypedResult
				);
			}
			if (response.untypedResult instanceof LaunchErrorResponse) {
				throw new LaunchErrorCastException(
					"Application launch error: " + ((LaunchErrorResponse) response.untypedResult).getReason()
				);
			}
			if (response.untypedResult != null) {
				throw new UntypedChromeCastException(
					"Cast device returned " + response.untypedResult.getClass().getSimpleName() +
					" instead of the expected " + responseClass.getSimpleName(),
					response.untypedResult
				);
			}
			throw new UnprocessedChromeCastException(
				"Failed to deserialize response to " + responseClass.getSimpleName(),
				response.unprocessedResult
			);
		} catch (InterruptedException e) {
			throw new ChromeCastException("Interrupted while waiting for response", e);
		} catch (TimeoutException e) {
			throw new ChromeCastException("Waiting for response timed out", e);
		} finally {
			synchronized (requests) {
				requests.remove(requestId);
			}
		}
	}

	/**
	 * Request a {@link ReceiverStatus} from the cast device, using
	 * {@value #DEFAULT_RESPONSE_TIMEOUT} as the timeout value.
	 *
	 * @return The resulting {@link ReceiverStatus}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	public ReceiverStatus getReceiverStatus() throws IOException { //TODO: (Nad) DO the same in Session..? (responseTimeout)
		return getReceiverStatus(DEFAULT_RESPONSE_TIMEOUT);
	}

	/**
	 * Request a {@link ReceiverStatus} from the cast device.
	 *
	 * @param responseTimeout the response timeout in milliseconds. If zero or
	 *            negative, {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link ReceiverStatus}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	public ReceiverStatus getReceiverStatus(long responseTimeout) throws IOException {
		ReceiverStatusResponse status = send(
			"urn:x-cast:com.google.cast.receiver",
			StandardRequest.status(),
			PLATFORM_SENDER_ID,
			PLATFORM_RECEIVER_ID,
			ReceiverStatusResponse.class,
			responseTimeout
		);
		ReceiverStatus result;
		if (status == null || (result = status.getStatus()) == null) {
			return null;
		}
		cacheVolume(result);
		return result;
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
		return isApplicationAvailable(applicationId, DEFAULT_RESPONSE_TIMEOUT);
	}

	/**
	 * Queries the cast device if the application represented by the specified
	 * application ID is available.
	 *
	 * @param applicationId the application ID for which to query availability.
	 * @param responseTimeout the response timeout in milliseconds if. If zero
	 *            or negative, {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return {@code true} if the application is available, {@code false} if
	 *         it's not.
	 * @throws IOException If the response times out or if an error occurs
	 *             during the operation.
	 */
	public boolean isApplicationAvailable(String applicationId, long responseTimeout) throws IOException {
		AppAvailabilityResponse availability = send(
			"urn:x-cast:com.google.cast.receiver",
			StandardRequest.getAppAvailability(applicationId),
			PLATFORM_SENDER_ID,
			PLATFORM_RECEIVER_ID,
			AppAvailabilityResponse.class,
			responseTimeout
		);
		return availability != null && "APP_AVAILABLE".equals(availability.getAvailability().get(applicationId));
	}

	/**
	 * Asks the cast device to launch the application represented by the
	 * specified application ID, using {@value #DEFAULT_RESPONSE_TIMEOUT} as the
	 * timeout value.
	 *
	 * @param applicationId the application ID for the application to launch.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link ReceiverStatus} or {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	public ReceiverStatus launch(String applicationId, boolean synchronous) throws IOException {
		return launch(applicationId, synchronous, DEFAULT_RESPONSE_TIMEOUT);
	}

	/**
	 * Asks the cast device to launch the application represented by the
	 * specified application ID.
	 *
	 * @param applicationId the application ID for the application to launch.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code synchronous} is {@code true}. If zero or negative,
	 *            {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link ReceiverStatus} or {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public ReceiverStatus launch(String applicationId, boolean synchronous, long responseTimeout) throws IOException {
		ReceiverStatusResponse status = send(
			"urn:x-cast:com.google.cast.receiver",
			StandardRequest.launch(applicationId),
			PLATFORM_SENDER_ID,
			PLATFORM_RECEIVER_ID,
			synchronous ? ReceiverStatusResponse.class : null,
			responseTimeout
		);
		ReceiverStatus result;
		if (status == null || (result = status.getStatus()) == null) {
			return null;
		}
		cacheVolume(result);
		return result;
	}

	/**
	 * Asks the cast device to stop the specified {@link Application}, using
	 * {@value #DEFAULT_RESPONSE_TIMEOUT} as the timeout value.
	 *
	 * @param application the {@link Application} to stop.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link ReceiverStatus} or {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	public ReceiverStatus stopApplication(@Nonnull Application application, boolean synchronous) throws IOException {
		return stopApplication(application, synchronous, DEFAULT_RESPONSE_TIMEOUT);
	}

	/**
	 * Asks the cast device to stop the specified {@link Application}.
	 *
	 * @param application the {@link Application} to stop.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code synchronous} is {@code true}. If zero or negative,
	 *            {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link ReceiverStatus} or {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	public ReceiverStatus stopApplication(
		@Nonnull Application application,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		ReceiverStatusResponse status = send(
			"urn:x-cast:com.google.cast.receiver",
			StandardRequest.stop(application.getSessionId()),
			PLATFORM_SENDER_ID,
			PLATFORM_RECEIVER_ID,
			synchronous ? ReceiverStatusResponse.class : null,
			responseTimeout
		);
		ReceiverStatus result;
		if (status == null || (result = status.getStatus()) == null) {
			return null;
		}
		cacheVolume(result);
		return result;
	}

	/**
	 * Establishes a {@link Session} with the specified {@link Application}
	 * unless one already exists, in which case the existing {@link Session} is
	 * returned.
	 *
	 * @param senderId the sender ID to use.
	 * @param application the {@link Application} to connect to.
	 * @param userAgent the user-agent String or {@code null}. It's not entirely
	 *            clear what this is used for other than reporting to Google, so
	 *            it might be that it's better left {@code null}.
	 * @param connectionType The {@link VirtualConnectionType} to use. Please
	 *            note that only {@link VirtualConnectionType#STRONG} and
	 *            {@link VirtualConnectionType#INVISIBLE} are allowed.
	 * @return The existing or new {@link Session}.
	 * @throws IOException If an error occurs during the operation.
	 */
	@Nonnull
	public Session startSession(
		@Nonnull String senderId,
		@Nonnull Application application,
		@Nullable String userAgent,
		@Nonnull VirtualConnectionType connectionType
	) throws IOException {
		requireNotBlank(senderId, "senderId");
		requireNotNull(application, "application");
		String sessionId = application.getSessionId();
		requireNotBlank(sessionId, "application.getSessionId()");
		String destinationId = application.getTransportId();
		requireNotBlank(destinationId, "application.getTransportId()");
		Session result = null;
		synchronized (sessionsLock) {
			for (Session session : sessions) {
				if (
					sessionId.equals(session.getId()) &&
					destinationId.equals(session.getDestinationId()) &&
					senderId.equals(session.getSenderId())
				) {
					return session;
				}
			}
			write(
				"urn:x-cast:com.google.cast.tp.connection",
				StandardMessage.connect(userAgent, connectionType),
				senderId,
				destinationId
			);
			result = new Session(senderId, sessionId, destinationId, this);
			sessions.add(result);
		}
		return result;
	}

	/**
	 * Closes the specified {@link Session} unless it's already closed, in which
	 * case nothing is done.
	 *
	 * @param session the {@link Session} to close.
	 * @return {@code true} if the {@link Session} was closed, {@code false} if
	 *         it was closed already.
	 * @throws IOException If an error occurs during the operation.
	 */
	public boolean closeSession(@Nullable Session session) throws IOException {
		if (session == null) {
			return false;
		}
		synchronized (sessionsLock) {
			if (!sessions.remove(session)) {
				return false;
			}
		}
		SessionClosedListener listener = session.getSessionClosedListener();
		if (listener != null) {
			listener.closed(session);
		}
		write(
			"urn:x-cast:com.google.cast.tp.connection",
			StandardMessage.closeConnection(),
			session.getSenderId(),
			session.getDestinationId()
		);
		return true;
	}

	/**
	 * Checks whether or not the specified {@link Session} is closed.
	 *
	 * @param session the Session to check.
	 * @return {@code true} if the specified {@link Session} is closed,
	 *         {@code false} if it's connected.
	 */
	public boolean isSessionClosed(@Nullable Session session) {
		if (session == null) {
			return true;
		}
		synchronized (sessionsLock) {
			return !sessions.contains(session);
		}
	}

	/**
	 * Request a {@link MediaStatus} from the application with the specified
	 * destination ID, using {@value #DEFAULT_RESPONSE_TIMEOUT} as the timeout
	 * value.
	 *
	 * @param senderId the sender ID to use.
	 * @param destinationId the destination ID to use.
	 * @return The resulting {@link MediaStatus}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus getMediaStatus(@Nonnull String senderId, @Nonnull String destinationId) throws IOException {
		return getMediaStatus(senderId, destinationId, DEFAULT_RESPONSE_TIMEOUT);
	}

	/**
	 * Request a {@link MediaStatus} from the application with the specified
	 * destination ID.
	 *
	 * @param senderId the sender ID to use.
	 * @param destinationId the destination ID to use.
	 * @param responseTimeout the response timeout in milliseconds. If zero or
	 *            negative, {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link MediaStatus}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus getMediaStatus(
		@Nonnull String senderId,
		@Nonnull String destinationId,
		long responseTimeout
	) throws IOException {
		MediaStatusResponse status = send(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.status(),
			senderId,
			destinationId,
			MediaStatusResponse.class,
			responseTimeout
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	/**
	 * Asks the targeted remote application to load the specified {@link Media}
	 * using the specified parameters, using {@value #DEFAULT_RESPONSE_TIMEOUT}
	 * as the timeout value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param senderId the sender ID to use.
	 * @param destinationId the destination ID to use.
	 * @param sessionId the session ID to use.
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
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus load(
		@Nonnull String senderId,
		@Nonnull String destinationId,
		@Nonnull String sessionId,
		Media media,
		boolean autoplay,
		double currentTime,
		boolean synchronous,
		@Nullable Map<String, Object> customData
	) throws IOException {
		return load(
			senderId,
			destinationId,
			sessionId,
			media,
			autoplay,
			currentTime,
			synchronous,
			DEFAULT_RESPONSE_TIMEOUT,
			customData
		);
	}

	/**
	 * Asks the targeted remote application to load the specified {@link Media}
	 * using the specified parameters.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param senderId the sender ID to use.
	 * @param destinationId the destination ID to use.
	 * @param sessionId the session ID to use.
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
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus load(
		@Nonnull String senderId,
		@Nonnull String destinationId,
		@Nonnull String sessionId,
		Media media,
		boolean autoplay,
		double currentTime,
		boolean synchronous,
		long responseTimeout,
		@Nullable Map<String, Object> customData
	) throws IOException {
		MediaStatusResponse status = send(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.load(sessionId, media, autoplay, currentTime, customData),
			senderId,
			destinationId,
			synchronous ? MediaStatusResponse.class : null,
			responseTimeout
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
	 * @param senderId the sender ID to use.
	 * @param destinationId the destination ID to use.
	 * @param sessionId the session ID to use.
	 * @param mediaSessionId the media session ID for which the play request
	 *            applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus play(
		@Nonnull String senderId,
		@Nonnull String destinationId,
		@Nonnull String sessionId,
		long mediaSessionId,
		boolean synchronous
	) throws IOException {
		return play(senderId, destinationId, sessionId, mediaSessionId, synchronous, DEFAULT_RESPONSE_TIMEOUT);
	}

	/**
	 * Asks the remote application to start playing the media referenced by the
	 * specified media session ID.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param senderId the sender ID to use.
	 * @param destinationId the destination ID to use.
	 * @param sessionId the session ID to use.
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
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus play(
		@Nonnull String senderId,
		@Nonnull String destinationId,
		@Nonnull String sessionId,
		long mediaSessionId,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		MediaStatusResponse status = send(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.play(sessionId, mediaSessionId),
			senderId,
			destinationId,
			synchronous ? MediaStatusResponse.class : null,
			responseTimeout
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
	 * @param senderId the sender ID to use.
	 * @param destinationId the destination ID to use.
	 * @param sessionId the session ID to use.
	 * @param mediaSessionId the media session ID for which the pause request
	 *            applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus pause(
		@Nonnull String senderId,
		@Nonnull String destinationId,
		@Nonnull String sessionId,
		long mediaSessionId,
		boolean synchronous
	) throws IOException {
		return pause(senderId, destinationId, sessionId, mediaSessionId, synchronous, DEFAULT_RESPONSE_TIMEOUT);
	}

	/**
	 * Asks the remote application to pause playback of the media referenced by
	 * the specified media session ID.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param senderId the sender ID to use.
	 * @param destinationId the destination ID to use.
	 * @param sessionId the session ID to use.
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
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus pause(
		@Nonnull String senderId,
		@Nonnull String destinationId,
		@Nonnull String sessionId,
		long mediaSessionId,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		MediaStatusResponse status = send(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.pause(sessionId, mediaSessionId),
			senderId,
			destinationId,
			synchronous ? MediaStatusResponse.class : null,
			responseTimeout
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	/**
	 * Asks the remote application to move the playback position of the media
	 * referenced by the specified media session ID to the specified position,
	 * using {@value #DEFAULT_RESPONSE_TIMEOUT} as the timeout value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param senderId the sender ID to use.
	 * @param destinationId the destination ID to use.
	 * @param sessionId the session ID to use.
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
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus seek(
		@Nonnull String senderId,
		@Nonnull String destinationId,
		@Nonnull String sessionId,
		long mediaSessionId,
		double currentTime,
		@Nullable ResumeState resumeState,
		boolean synchronous
	) throws IOException {
		return seek(
			senderId,
			destinationId,
			sessionId,
			mediaSessionId,
			currentTime,
			resumeState,
			synchronous,
			DEFAULT_RESPONSE_TIMEOUT
		);
	}

	/**
	 * Asks the remote application to move the playback position of the media
	 * referenced by the specified media session ID to the specified position.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param senderId the sender ID to use.
	 * @param destinationId the destination ID to use.
	 * @param sessionId the session ID to use.
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
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus seek(
		@Nonnull String senderId,
		@Nonnull String destinationId,
		@Nonnull String sessionId,
		long mediaSessionId,
		double currentTime,
		@Nullable ResumeState resumeState,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		MediaStatusResponse status = send(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.seek(sessionId, mediaSessionId, currentTime, resumeState),
			senderId,
			destinationId,
			synchronous ? MediaStatusResponse.class : null,
			responseTimeout
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	/**
	 * Asks the remote application to stop playback and unload the media
	 * referenced by the specified media session ID, using
	 * {@value #DEFAULT_RESPONSE_TIMEOUT} as the timeout value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param senderId the sender ID to use.
	 * @param destinationId the destination ID to use.
	 * @param mediaSessionId the media session ID for which the
	 *            {@link MediaVolume} request applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IllegalArgumentException If {@code senderId} or
	 *             {@code destinationId} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus stopMedia(
		@Nonnull String senderId,
		@Nonnull String destinationId,
		long mediaSessionId,
		boolean synchronous
	) throws IOException {
		return stopMedia(senderId, destinationId, mediaSessionId, synchronous, DEFAULT_RESPONSE_TIMEOUT);
	}

	/**
	 * Asks the remote application to stop playback and unload the media
	 * referenced by the specified media session ID.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param senderId the sender ID to use.
	 * @param destinationId the destination ID to use.
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
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IllegalArgumentException If {@code senderId} or
	 *             {@code destinationId} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus stopMedia(
		@Nonnull String senderId,
		@Nonnull String destinationId,
		long mediaSessionId,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		MediaStatusResponse status = send(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.stopMedia(mediaSessionId, null),
			senderId,
			destinationId,
			synchronous ? MediaStatusResponse.class : null,
			responseTimeout
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	/**
	 * Asks the remote application to change the volume level or mute state of
	 * the stream of the specified media session. Please note that this is
	 * different from the device volume level or mute state, and that this will
	 * give the user no visual indication, using
	 * {@value #DEFAULT_RESPONSE_TIMEOUT} as the timeout value.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param senderId the sender ID to use.
	 * @param destinationId the destination ID to use.
	 * @param sessionId the session ID to use.
	 * @param mediaSessionId the media session ID for which the
	 *            {@link MediaVolume} request applies.
	 * @param volume the {@link MediaVolume} to set.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IllegalArgumentException If {@code senderId},
	 *             {@code destinationId}, {@code sessionId} or {@code volume} is
	 *             {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus setMediaVolume(
		@Nonnull String senderId,
		@Nonnull String destinationId,
		@Nonnull String sessionId,
		long mediaSessionId,
		@Nonnull MediaVolume volume,
		boolean synchronous
	) throws IOException {
		return setMediaVolume(
			senderId,
			destinationId,
			sessionId,
			mediaSessionId,
			volume,
			synchronous,
			DEFAULT_RESPONSE_TIMEOUT
		);
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
	 * @param senderId the sender ID to use.
	 * @param destinationId the destination ID to use.
	 * @param sessionId the session ID to use.
	 * @param mediaSessionId the media session ID for which the
	 *            {@link MediaVolume} request applies.
	 * @param volume the {@link MediaVolume} to set.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code synchronous} is {@code true}. If zero or negative,
	 *            {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false} or a timeout occurs.
	 * @throws IllegalArgumentException If {@code senderId},
	 *             {@code destinationId}, {@code sessionId} or {@code volume} is
	 *             {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus setMediaVolume(
		@Nonnull String senderId,
		@Nonnull String destinationId,
		@Nonnull String sessionId,
		long mediaSessionId,
		@Nonnull MediaVolume volume,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		MediaStatusResponse status = send(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.volumeRequest(sessionId, mediaSessionId, volume, null),
			senderId,
			destinationId,
			synchronous ? MediaStatusResponse.class : null,
			responseTimeout
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	@Nullable
	public void setVolume(Volume volume) throws IOException {
		if (volume == null) {
			return;
		}
		Volume lastVolume;
		synchronized (cachedVolumeLock) {
			lastVolume = cachedVolume;
		}
		if (lastVolume != null) {
			if (lastVolume.getControlType() == VolumeControlType.FIXED) {
				throw new ChromeCastException("Cannot set volume level or mute since the device has a fixed volume");
			}
			Double currentLevelObj, targetLevelObj, stepIntervalObj;
			if (
				(currentLevelObj = lastVolume.getLevel()) != null &&
				(targetLevelObj = volume.getLevel()) != null &&
//				lastVolume.getControlType() == VolumeControlType.MASTER && //TODO: (Nad) Temp test
				(stepIntervalObj = lastVolume.getStepInterval()) != null
			) {
				GradualVolumeTask task = null;
				double currentLevel, targetLevel, stepInterval, diff;
				if (
					(currentLevel = currentLevelObj.doubleValue()) != (targetLevel = targetLevelObj.doubleValue()) &&
					(diff = Math.abs(targetLevel - currentLevel)) > (stepInterval = stepIntervalObj.doubleValue())
				) {
					int steps = (int) Math.ceil(diff / stepInterval);
					if (steps > 1) {
						if (targetLevel < currentLevel) {
							stepInterval = -stepInterval;
						}
						task = new GradualVolumeTask(stepInterval, targetLevel);
						volume = volume.modify().level(Double.valueOf(currentLevel + stepInterval)).build();
					}
				}

				synchronized (gradualVolumeLock) {
					if (gradualVolumeTask != null) {
						gradualVolumeTask.cancel();
					}
					if (task != null) {
						if (gradualVolumeTimer == null) {
							gradualVolumeTimer = new Timer(remoteName + " gradual volume timer");
						}
						gradualVolumeTask = task;
						gradualVolumeTimer.schedule(task, 500, 500);
					} else if (gradualVolumeTimer != null) {
						gradualVolumeTimer.cancel();
						gradualVolumeTimer = null;
					}
				}
			}
		}
		doSetVolume(volume, false, DEFAULT_RESPONSE_TIMEOUT);
	}

	@Nullable
	protected ReceiverStatus doSetVolume(Volume volume, boolean synchronous, long responseTimeout) throws IOException {
		ReceiverStatusResponse status = send(
			"urn:x-cast:com.google.cast.receiver",
			StandardRequest.setVolume(volume),
			PLATFORM_SENDER_ID,
			PLATFORM_RECEIVER_ID,
			synchronous ? ReceiverStatusResponse.class : null,
			responseTimeout
		);
		ReceiverStatus result;
		if (status == null || (result = status.getStatus()) == null) {
			return null;
		}
		cacheVolume(result);
		return result;
	}

	public <T extends Response> T sendGenericRequest(
		@Nonnull String senderId,
		@Nonnull String destinationId,
		@Nonnull String namespace,
		Request request,
		Class<T> responseClass
	) throws IOException {
		return send(namespace, request, senderId, destinationId, responseClass, DEFAULT_RESPONSE_TIMEOUT);
	}

	public <T extends Response> T sendGenericRequest(
		@Nonnull String senderId,
		@Nonnull String destinationId,
		@Nonnull String namespace,
		Request request,
		Class<T> responseClass,
		long responseTimeout
	) throws IOException {
		return send(namespace, request, senderId, destinationId, responseClass, responseTimeout);
	}

	/**
	 * Caches the {@link Volume} instance from the specified
	 * {@link ReceiverStatus} as as long as neither are {@code null}.
	 *
	 * @param receiverStatus the {@link ReceiverStatus} from which to get the
	 *            {@link Volume} to store.
	 */
	protected void cacheVolume(@Nullable ReceiverStatus receiverStatus) {
		Volume volume;
		if (receiverStatus != null && (volume = receiverStatus.getVolume()) != null) {
			cacheVolume(volume);
		}
	}

	/**
	 * Caches the specified {@link Volume} instance as long as it's not
	 * {@code null}.
	 *
	 * @param volume the {@link Volume} instance to store.
	 */
	protected void cacheVolume(@Nullable Volume volume) {
		if (volume == null) {
			return;
		}
		synchronized (cachedVolumeLock) {
			cachedVolume = volume;
		}
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

	protected void write(String namespace, Message message, String senderId, String destinationId) throws IOException {
		write(namespace, jsonMapper.writeValueAsString(message), senderId, destinationId);
	}

	protected void write(String namespace, String message, String senderId, String destinationId) throws IOException {
		LOGGER.trace(CHROMECAST_API_MARKER, "Sending message to {}; \"{}\"", remoteName, message);
		CastMessage msg = CastMessage.newBuilder()
			.setProtocolVersion(CastMessage.ProtocolVersion.CASTV2_1_0)
			.setSourceId(senderId)
			.setDestinationId(destinationId)
			.setNamespace(namespace)
			.setPayloadType(CastMessage.PayloadType.STRING)
			.setPayloadUtf8(message)
			.build();
		write(msg);
	}

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
				throw new ChromeCastException("Incomplete message, ended after reading " + read + " of " + size + " bytes");
			}
			read += readNow;
		}
		return CastMessage.parseFrom(buf);
	}

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

	public static void validateNamespace(@Nonnull String nameSpace) throws IllegalArgumentException { //TODO: (Nad) Use..u
		requireNotBlank(nameSpace, "namespace");
		if (nameSpace.length() > 128) {
			throw new IllegalArgumentException("Invalid namespace length " + nameSpace.length());
		} else if (!nameSpace.startsWith("urn:x-cast:")) {
			throw new IllegalArgumentException("Namespace must begin with the prefix \"urn:x-cast:\"");
		} else if (nameSpace.length() == 11) {
			throw new IllegalArgumentException("Namespace must begin with the prefix \"urn:x-cast:\" and have non-empty suffix");
		}
	}

	protected static Executor createExecutor() {
		ThreadPoolExecutor result = new ThreadPoolExecutor(
			0,
			Integer.MAX_VALUE,
			300L,
			TimeUnit.SECONDS,
			new SynchronousQueue<Runnable>(true),
			new ThreadFactory() {

				private final AtomicInteger threadNumber = new AtomicInteger(1);

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "ChromeCast API worker #" + threadNumber.getAndIncrement());
				}
			}
		);
		return result;
	}

	/**
	 * A {@link TimerTask} that will send {@code PING} messages to the cast
	 * device upon execution.
	 *
	 * @author Nadahar
	 */
	protected class PingTask extends TimerTask {

		private final String messageString;
		private final CastMessage message;

		/**
		 * Creates a new instance.
		 */
		public PingTask() {
			try {
				messageString = jsonMapper.writeValueAsString(StandardMessage.ping());
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
			if (LOGGER.isTraceEnabled(CHROMECAST_API_MARKER)) {
				LOGGER.trace(CHROMECAST_API_MARKER, "Pinging {}", remoteName);
			}
			try {
				write(message);
			} catch (IOException e) {
				LOGGER.warn(
					CHROMECAST_API_MARKER,
					"An error occurred while sending 'PING' to {}: {}",
					remoteName,
					e.getMessage()
				);
				LOGGER.trace(CHROMECAST_API_MARKER, "", e);
			}
		}
	}

	/**
	 * A {@link TimerTask} that will gradually increase or decrease the volume
	 * level of the cast device until the target level is reached.
	 *
	 * @author Nadahar
	 */
	protected class GradualVolumeTask extends TimerTask {

		private final double interval;

		private final double target;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param interval the partial volume level change to use for each step,
		 *            positive to increase the volume level, negative to
		 *            decrease it.
		 * @param target the target volume level that will trigger a shutdown of
		 *            the task when reached.
		 */
		public GradualVolumeTask(double interval, double target) {
			this.interval = interval;
			this.target = target;
		}

		@Override
		public void run() {
			Volume currentVolume;
			synchronized (cachedVolumeLock) {
				currentVolume = cachedVolume;
			}
			if (currentVolume == null || currentVolume.getLevel() == null) {
				shutdownTask();
			} else {
				double currentLevel = currentVolume.getLevel().doubleValue();
				double newLevel = currentLevel + interval;
				if (interval > 0d) {
					if (newLevel > target) {
						newLevel = target;
						shutdownTask();
					}
				} else {
					if (newLevel < target) {
						newLevel = target;
						shutdownTask();
					}
				}
				Volume newVolume = new Volume(null, Double.valueOf(newLevel), null, null);
				try {
					doSetVolume(newVolume, false, DEFAULT_RESPONSE_TIMEOUT);
				} catch (IOException e) {
					// TODO Auto-generated catch block //TODO: (Nad) Make
					e.printStackTrace();
					shutdownTask();
				}
			}
		}

		private void shutdownTask() {
			cancel();
			synchronized (gradualVolumeLock) {
				if (gradualVolumeTask == this) {
					gradualVolumeTask = null;
					if (gradualVolumeTimer != null) {
						gradualVolumeTimer.cancel();
						gradualVolumeTimer = null;
					}
				}
			}
		}
	}

	protected class InputHandler extends Thread {

		private volatile boolean running;

		@Nonnull
		private final InputStream is;

		@Nonnull
		private final CastMessage pongMessage;

		public InputHandler(@Nonnull InputStream inputStream) {
			super(remoteName + " input handler");
			requireNotNull(inputStream, "inputStream");
			this.is = inputStream;

			String messageString;
			try {
				messageString = jsonMapper.writeValueAsString(StandardMessage.pong());
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
								CHROMECAST_API_MARKER,
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
									CHROMECAST_API_MARKER,
									"{} InputHandler: Received message with binary payload ({} bytes)",
									remoteName,
									message.getPayloadBinary() == null ? "unknown number of" : message.getPayloadBinary().size()
								);
								EXECUTOR.execute(new BinaryMessageHandler(message));
								break;
							case STRING:
								jsonMessage = message.getPayloadUtf8();
								if (isBlank(jsonMessage)) {
									LOGGER.trace(
										CHROMECAST_API_MARKER,
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
											CHROMECAST_API_MARKER,
											"Received PING from {}, replying with PONG",
											remoteName
										);
										write(pongMessage);
									} else if ("PONG".equals(responseType)) {
										LOGGER.trace(CHROMECAST_API_MARKER, "Received PONG from {}", remoteName);
									} else {
										LOGGER.trace(
											CHROMECAST_API_MARKER,
											"Received unexpected heartbeat message of type \"{}\" from {}",
											responseType,
											remoteName
										);
									}
									continue;
								}
								LOGGER.trace(
									CHROMECAST_API_MARKER,
									"{} InputHandler: Received string message \"{}\"",
									remoteName,
									jsonMessage
								);
								EXECUTOR.execute(new StringMessageHandler(message, jsonMessage));
								break;
							default:
								LOGGER.warn(
									CHROMECAST_API_MARKER,
									"{} InputHandler: Received a message with an unknown payload type '{}'",
									remoteName,
									payloadType
								);
								break;

						}
					} else if (message != null) {
						LOGGER.warn(
							CHROMECAST_API_MARKER,
							"{} InputHandler: Received a message without a payload type",
							remoteName
						);
					} else {
						LOGGER.warn(
							CHROMECAST_API_MARKER,
							"{} InputHandler: Received a null message",
							remoteName
						);
					}
				}
			} catch (IOException e) {
				if (running) {
					LOGGER.error(CHROMECAST_API_MARKER, "{} InputHandler exception, terminating handler: ", remoteName, e.getMessage());
					if (message != null && LOGGER.isDebugEnabled(CHROMECAST_API_MARKER)) {
						StringBuilder sb = new StringBuilder();
						if (message.hasNamespace()) {
							sb.append("namespace: ").append(message.getNamespace());
						}
						if (message.hasProtocolVersion() && message.getProtocolVersion() != null) {
							if (sb.length() > 0) {
								sb.append(", ");
							}
							sb.append("protocol version: ").append(message.getProtocolVersion().getNumber());
						}
						if (message.hasPayloadUtf8()) {
							if (sb.length() > 0) {
								sb.append(", ");
							}
							sb.append("string payload: ").append(message.getPayloadUtf8());
						}
						if (message.hasPayloadBinary()) {
							if (sb.length() > 0) {
								sb.append(", ");
							}
							sb.append("binary payload: ").append(message.getPayloadBinary());
						}
						LOGGER.debug(CHROMECAST_API_MARKER, "Triggering (potentially partial) message: {}", sb.toString());
					}
					LOGGER.trace(CHROMECAST_API_MARKER, "", e);
					running = false;
				} else {
					LOGGER.trace(
						CHROMECAST_API_MARKER,
						"Exception while shutting down {} InputHandler: {}",
						remoteName,
						e.getMessage()
					);
				}
				try {
					close();
				} catch (IOException ioe) {
					LOGGER.debug(
						CHROMECAST_API_MARKER,
						"An error occurred while closing {} socket: {}",
						remoteName,
						e.getMessage()
					);
				}
			}
		}

		public void stopProcessing() {
			running = false;
		}
	}

	protected class StringMessageHandler implements Runnable {

		@Nonnull
		protected final CastMessage message;

		@Nonnull
		protected final String jsonMessage;

		public StringMessageHandler(@Nonnull CastMessage message, @Nonnull String jsonMessage) {
			requireNotNull(message, "message");
			requireNotBlank(jsonMessage, "jsonMessage");
			this.message = message;
			this.jsonMessage = jsonMessage;
		}

		@Override
		public void run() {
			try {
				LOGGER.error(CHROMECAST_API_MARKER, "Parsing message: {}", jsonMessage); //TODO: (Nad) Temp test
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
					if (!listeners.isEmpty()) {
						listeners.fire(new DefaultCastEvent<>(
							CastEventType.CUSTOM_MESSAGE,
							new CustomMessageEvent(message.getNamespace(), message.getPayloadUtf8())
						));
					}
				} else if ("CLOSE".equals(responseType)) {
					if (PLATFORM_RECEIVER_ID.equals(message.getSourceId())) {
						try {
							close();
						} catch (IOException e) {
							LOGGER.debug(
								CHROMECAST_API_MARKER,
								"An error occurred while closing {} socket: {}",
								remoteName,
								e.getMessage()
							);
						}
					} else {
						String peerId = message.getSourceId();
						if (!isBlank(peerId)) {
							Set<Session> closedNow = new HashSet<>();
							synchronized (sessionsLock) {
								Session session;
								for (Iterator<Session> iterator = sessions.iterator(); iterator.hasNext();) {
									session = iterator.next();
									if (peerId.equals(session.getDestinationId())) {
										closedNow.add(session);
										iterator.remove();
									}
								}
							}
							if (!closedNow.isEmpty()) {
								SessionClosedListener closedListener;
								for (Session session : closedNow) {
									closedListener = session.getSessionClosedListener();
									if (closedListener != null) {
										closedListener.closed(session);
									}
								}
							} else {
								// Didn't match any "known" session, pass it on to listeners
								if (!listeners.isEmpty()) { //TODO: (Nad) Must include sourceId/destionationId to have any value..
									listeners.fire(new DefaultCastEvent<>(
										CastEventType.CLOSE,
										jsonMapper.treeToValue(parsedMessage, StandardResponse.class)
									));
								}
							}
						}
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

					if (response instanceof StandardResponse) {
//						LOGGER.error(CHROMECAST_API_MARKER, "Received unhandled standard response of type {}: {}", response.getClass().getSimpleName(), response); //TODO: (Nad) Log anything? Maybe in "CastEvent"?
						ReceiverStatus receiverStatus;
						if (
							response instanceof ReceiverStatusResponse &&
							(receiverStatus = ((ReceiverStatusResponse) response).getStatus()) != null
						) {
							cacheVolume(receiverStatus);
						}
						listeners.fire(new DefaultCastEvent<>(response.getEventType(), response));
					} else {
						LOGGER.error(
							CHROMECAST_API_MARKER,
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
					CHROMECAST_API_MARKER,
					"Error while processing JSON message from {}: {}",
					remoteName,
					e.getMessage()
				);
				LOGGER.trace(CHROMECAST_API_MARKER, "", e);
			}
		}
	}

	protected class BinaryMessageHandler implements Runnable {

		protected final CastMessage message;

		public BinaryMessageHandler(@Nonnull CastMessage message) {
			requireNotNull(message, "message");
			this.message = message;
		}

		@Override
		public void run() {
			if (!listeners.isEmpty()) {
				listeners.fire(new DefaultCastEvent<>(CastEventType.CUSTOM_MESSAGE, new CustomMessageEvent(
					message.getNamespace(),
					message.getPayloadBinary()
				)));
			}
		}
	}

	protected class ResultProcessor<T extends Response> {

		private final Class<T> responseClass;
		private final long requestTimeout;
		private ResultProcessorResult<T> result;

		private ResultProcessor(Class<T> responseClass, long requestTimeout) {
			if (responseClass == null) {
				throw new NullPointerException();
			}
			this.responseClass = responseClass;
			this.requestTimeout = requestTimeout < 1 ? DEFAULT_RESPONSE_TIMEOUT : requestTimeout;
		}

		@SuppressWarnings("unchecked")
		public void process(String jsonMSG) throws JsonMappingException, JsonProcessingException {
			Class<?> deserializeTo;
			if (StandardResponse.class.isAssignableFrom(responseClass)) {
				deserializeTo = StandardResponse.class;
			} else {
				deserializeTo = responseClass;
			}
			Object object;
			try {
				object = jsonMapper.readValue(jsonMSG, deserializeTo);
			} catch (IllegalArgumentException e) {
				synchronized (this) {
					this.result = new ResultProcessorResult<>(null, null, jsonMSG);
					this.notify();
					return;
				}
			}
			synchronized (this) {
				if (responseClass.isInstance(object)) {
					this.result = new ResultProcessorResult<>((T) object, null, null);
				} else {
					this.result = new ResultProcessorResult<>(null, (StandardResponse) object, null);
				}
				this.notify();
			}
		}

		@Nonnull
		public ResultProcessorResult<T> get() throws InterruptedException, TimeoutException {
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

	protected class ResultProcessorResult<T extends Response> {

		@Nullable
		protected final T typedResult;

		@Nullable
		protected final StandardResponse untypedResult;

		@Nullable
		protected final String unprocessedResult;

		public ResultProcessorResult(T typedResult, StandardResponse untypedResult, String unprocessedResult) {
			this.typedResult = typedResult;
			this.untypedResult = untypedResult;
			this.unprocessedResult = unprocessedResult;
		}
	}
}
