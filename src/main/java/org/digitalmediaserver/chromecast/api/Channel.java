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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import org.digitalmediaserver.chromecast.api.CastEvent.CastEventListener;
import org.digitalmediaserver.chromecast.api.CastEvent.CastEventListenerList;
import org.digitalmediaserver.chromecast.api.CastEvent.CastEventType;
import org.digitalmediaserver.chromecast.api.CastEvent.DefaultCastEvent;
import org.digitalmediaserver.chromecast.api.ChromeCastException.ErrorResponseChromeCastException;
import org.digitalmediaserver.chromecast.api.ChromeCastException.LaunchErrorCastException;
import org.digitalmediaserver.chromecast.api.ChromeCastException.UnprocessedChromeCastException;
import org.digitalmediaserver.chromecast.api.ChromeCastException.UntypedChromeCastException;
import org.digitalmediaserver.chromecast.api.ImmutableCastMessage.ImmutableBinaryCastMessage;
import org.digitalmediaserver.chromecast.api.ImmutableCastMessage.ImmutableStringCastMessage;
import org.digitalmediaserver.chromecast.api.Session.SessionClosedListener;
import org.digitalmediaserver.chromecast.api.StandardRequest.GetAppAvailability;
import org.digitalmediaserver.chromecast.api.StandardRequest.GetStatus;
import org.digitalmediaserver.chromecast.api.StandardRequest.Launch;
import org.digitalmediaserver.chromecast.api.StandardRequest.Load;
import org.digitalmediaserver.chromecast.api.StandardRequest.Pause;
import org.digitalmediaserver.chromecast.api.StandardRequest.Play;
import org.digitalmediaserver.chromecast.api.StandardRequest.ResumeState;
import org.digitalmediaserver.chromecast.api.StandardRequest.Seek;
import org.digitalmediaserver.chromecast.api.StandardRequest.SetVolume;
import org.digitalmediaserver.chromecast.api.StandardRequest.Stop;
import org.digitalmediaserver.chromecast.api.StandardRequest.StopMedia;
import org.digitalmediaserver.chromecast.api.StandardRequest.VolumeRequest;
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * Internal class for low-level communication with ChromeCast device. It's
 * normally desirable to use {@link CastDevice} or {@link Session} methods
 * instead of calling this class directly.
 */
public class Channel implements Closeable {

	private static final Logger LOGGER = LoggerFactory.getLogger(Channel.class);

	/** The logging {@link Marker} to use for logging */
	public static final Marker CHROMECAST_API_MARKER = MarkerFactory.getMarker("chromecast-api");

	/** The standard port used for cast devices */
	public static final int STANDARD_DEVICE_PORT = 8009;

	/** The delay between {@code PING} requests in milliseconds */
	protected static final long PING_PERIOD = 10 * 1000; //TODO: (Nad) 5 sec suggested in doc, was 30

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

	/** The {@link Executor} that is used for asynchronous operations */
	@Nonnull
	protected static final Executor EXECUTOR = createExecutor();

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
	 * The currently known {@link Session}s belonging to this {@link Channel}
	 */
	@Nonnull
	@GuardedBy("sessionsLock")
	protected final Set<Session> sessions = new HashSet<>();

	/** The cached volume synchronization object */
	@Nonnull
	protected final Object cachedVolumeLock = new Object();

	/** The last received {@link Volume} instance */
	@Nullable
	@GuardedBy("cachedVolumeLock")
	protected Volume cachedVolume;

	/** The gradual volume synchronization object */
	@Nonnull
	protected final Object gradualVolumeLock = new Object();

	/** The {@link Timer} used to handle gradual volume change */
	@Nullable
	@GuardedBy("gradualVolumeLock")
	protected Timer gradualVolumeTimer;

	/** The {@link TimerTask} that executes the gradual volume change */
	@Nullable
	@GuardedBy("gradualVolumeLock")
	protected TimerTask gradualVolumeTask;

	/**
	 * Creates a new channel using the specified parameters and the standard
	 * port.
	 *
	 * @param host the host IP address or hostname of the cast device.
	 * @param remoteName the name to use for the cast device in logging.
	 * @param listeners the {@link CastEventListenerList} to use when sending
	 *            events.
	 */
	public Channel(
		@Nonnull String host,
		@Nonnull String remoteName,
		@Nonnull CastEventListenerList listeners
	) {
		this(host, STANDARD_DEVICE_PORT, remoteName, listeners);
	}

	/**
	 * Creates a new channel using the specified parameters.
	 *
	 * @param host the host IP address or hostname of the cast device.
	 * @param port the port of the cast device.
	 * @param remoteName the name to use for the cast device in logging.
	 * @param listeners the {@link CastEventListenerList} to use when sending
	 *            events.
	 */
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
	 * Creates a new channel using the specified parameters and the standard
	 * port.
	 *
	 * @param address the IP address of the cast device.
	 * @param remoteName the name to use for the cast device in logging.
	 * @param listeners the {@link CastEventListenerList} to use when sending
	 *            events.
	 * @throws IllegalArgumentException If {@code listeners} is {@code null} or
	 *             if {@code remoteName} is blank.
	 */
	public Channel(
		@Nonnull InetAddress address,
		@Nonnull String remoteName,
		@Nonnull CastEventListenerList listeners
	) {
		this(address == null ? null : new InetSocketAddress(address, STANDARD_DEVICE_PORT), remoteName, listeners);
	}

	/**
	 * Creates a new channel using the specified parameters.
	 *
	 * @param address the IP address of the cast device.
	 * @param port the port of the cast device.
	 * @param remoteName the name to use for the cast device in logging.
	 * @param listeners the {@link CastEventListenerList} to use when sending
	 *            events.
	 * @throws IllegalArgumentException If {@code listeners} is {@code null}, if
	 *             {@code address} or {@code remoteName} is blank or if
	 *             {@code port} is outside the range of valid port numbers.
	 */
	@SuppressFBWarnings("NP_NULL_PARAM_DEREF")
	public Channel(
		@Nonnull InetAddress address,
		int port,
		@Nonnull String remoteName,
		@Nonnull CastEventListenerList listeners
	) {
		this(address == null || port == 0 ? null : new InetSocketAddress(address, port), remoteName, listeners);
	}

	/**
	 * Creates a new channel using the specified parameters.
	 *
	 * @param socketAddress the {@link SocketAddress} of the cast device.
	 * @param remoteName the name to use for the cast device in logging.
	 * @param listeners the {@link CastEventListenerList} to use when sending
	 *            events.
	 * @throws IllegalArgumentException If {@code socketAddress} or
	 *             {@code listeners} is {@code null} or if {@code remoteName} is
	 *             blank.
	 */
	public Channel(
		@Nonnull InetSocketAddress socketAddress,
		@Nonnull String remoteName,
		@Nonnull CastEventListenerList listeners
	) {
		requireNotNull(socketAddress, "socketAddress");
		requireNotBlank(remoteName, "remoteName");
		requireNotNull(listeners, "listeners");
		this.address = socketAddress;
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
			ImmutableBinaryCastMessage response = (ImmutableBinaryCastMessage) readMessage(socket.getInputStream());
			CastChannel.DeviceAuthMessage authResponse = CastChannel.DeviceAuthMessage.parseFrom(response.getPayload());
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
	 * this {@link Channel} is already closed, this is a no-op.
	 *
	 * @throws IOException If an error occurs during the operation.
	 */
	@Override
	public void close() throws IOException {
		Set<Session> closedSessions = null;
		synchronized (sessionsLock) {
			synchronized (socketLock) {
				if (socket == null || socket.isClosed() || !socket.isConnected()) {
					// Already closed
					return;
				}

				if (!sessions.isEmpty()) {
					closedSessions = new HashSet<>(sessions);
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
			}
		}

		cancelPendingDisconnected();
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
	 *         if it's open.
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
	 * @param session if {@code responseClass} is non-{@code null} and the
	 *            destination is an application, this must be the
	 *            {@link Session} that has been established with said
	 *            application. This makes sure that the waiting for the response
	 *            will be terminated if the {@link Session} is terminated. If
	 *            {@code responseClass} is {@code null} or the request is
	 *            destined to the cast device itself, this should be
	 *            {@code null}.
	 * @param namespace the namespace to use.
	 * @param message the {@link Request} to send.
	 * @param sourceId the source ID to use.
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
	 * @throws IllegalArgumentException If {@code namespace} is {@code null} or
	 *             invalid (see {@link #validateNamespace(String)} for
	 *             constraints).
	 * @throws IOException If {@code responseClass} is non-{@code null} and the
	 *             response from the cast device a different type than what was
	 *             expected, or if an error occurs during the operation.
	 */
	public <T extends Response> T send(
		@Nullable Session session,
		String namespace,
		Request message,
		String sourceId,
		String destinationId,
		Class<T> responseClass,
		long responseTimeout
	) throws IOException {
		validateNamespace(namespace);
		Long requestId = requestCounter.getAndIncrement();
		message.setRequestId(requestId);

		if (responseClass == null) {
			write(namespace, message, sourceId, destinationId);
			return null;
		}

		ResultProcessor<T> rp = new ResultProcessor<>(session, responseClass, responseTimeout);
		synchronized (requests) {
			requests.put(requestId, rp);
		}

		write(namespace, message, sourceId, destinationId);
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
	@Nullable
	public ReceiverStatus getReceiverStatus() throws IOException {
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
	@Nullable
	public ReceiverStatus getReceiverStatus(long responseTimeout) throws IOException {
		ReceiverStatusResponse status = send(
			null,
			"urn:x-cast:com.google.cast.receiver",
			new GetStatus(),
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
			null,
			"urn:x-cast:com.google.cast.receiver",
			new GetAppAvailability(applicationId),
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
			null,
			"urn:x-cast:com.google.cast.receiver",
			new Launch(applicationId),
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
	 * @throws NullPointerException If {@code application} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	public ReceiverStatus stopApplication(
		@Nonnull Application application,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		ReceiverStatusResponse status = send(
			null,
			"urn:x-cast:com.google.cast.receiver",
			new Stop(application.getSessionId()),
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
	 * @param sourceId the source ID to use.
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
		@Nonnull String sourceId,
		@Nonnull Application application,
		@Nullable String userAgent,
		@Nonnull VirtualConnectionType connectionType
	) throws IOException {
		requireNotBlank(sourceId, "sourceId");
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
					sourceId.equals(session.getSourceId())
				) {
					return session;
				}
			}
			write(
				"urn:x-cast:com.google.cast.tp.connection",
				StandardMessage.connect(userAgent, connectionType),
				sourceId,
				destinationId
			);
			result = new Session(sourceId, sessionId, destinationId, this);
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
		cancelPendingClosed(session.getDestinationId());
		SessionClosedListener listener = session.getSessionClosedListener();
		if (listener != null) {
			listener.closed(session);
		}
		write(
			"urn:x-cast:com.google.cast.tp.connection",
			StandardMessage.closeConnection(),
			session.getSourceId(),
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
	 * {@link Session}, using {@value #DEFAULT_RESPONSE_TIMEOUT} as the timeout
	 * value.
	 *
	 * @param session the {@link Session} to use.
	 * @return The resulting {@link MediaStatus}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus getMediaStatus(@Nonnull Session session) throws IOException {
		return getMediaStatus(session, DEFAULT_RESPONSE_TIMEOUT);
	}

	/**
	 * Request a {@link MediaStatus} from the application with the specified
	 * {@link Session}.
	 *
	 * @param session the {@link Session} to use.
	 * @param responseTimeout the response timeout in milliseconds. If zero or
	 *            negative, {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link MediaStatus}.
	 * @throws IllegalArgumentException If {@code session} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus getMediaStatus(@Nonnull Session session, long responseTimeout) throws IOException {
		requireNotNull(session, "session");
		MediaStatusResponse status = send(
			session,
			"urn:x-cast:com.google.cast.media",
			new GetStatus(),
			session.sourceId,
			session.destinationId,
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
	 * @param session the {@link Session} to use.
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
	 *         {@code synchronous} is {@code false}.
	 * @throws IllegalArgumentException If {@code session} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus load(
		@Nonnull Session session,
		@Nullable Boolean autoplay,
		@Nullable Double currentTime,
		Media media,
		@Nullable Map<String, Object> customData,
		boolean synchronous
	) throws IOException {
		return load(
			session,
			autoplay,
			currentTime,
			media,
			customData,
			synchronous,
			DEFAULT_RESPONSE_TIMEOUT
		);
	}

	/**
	 * Asks the targeted remote application to load the specified {@link Media}
	 * using the specified parameters.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param session the {@link Session} to use.
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
	 * @throws IllegalArgumentException If {@code session} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus load(
		@Nonnull Session session,
		@Nullable Boolean autoplay,
		@Nullable Double currentTime,
		Media media,
		@Nullable Map<String, Object> customData,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		requireNotNull(session, "session");
		MediaStatusResponse status = send(
			session,
			"urn:x-cast:com.google.cast.media",
			new Load(null, autoplay, null, null, currentTime, customData, null, media, null, null),
			session.sourceId,
			session.destinationId,
			synchronous ? MediaStatusResponse.class : null,
			responseTimeout
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	/**
	 * Asks the targeted remote application to load the specified {@link Media}
	 * using the specified parameters.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param session the {@link Session} to use.
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
	 * @param media the {@link Media} to load.
	 * @param playbackRate the media playback rate.
	 * @param queueData the queue data.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code synchronous} is {@code true}. If zero or negative,
	 *            {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IllegalArgumentException If {@code session} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 *
	 */
	@Nullable
	public MediaStatus load(
		@Nonnull Session session,
		@Nullable List<Integer> activeTrackIds,
		@Nullable Boolean autoplay,
		@Nullable String credentials,
		@Nullable String credentialsType,
		@Nullable Double currentTime,
		@Nullable Map<String, Object> customData,
		@Nullable LoadOptions loadOptions,
		@Nonnull Media media,
		@Nullable Double playbackRate,
		@Nullable QueueData queueData,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		requireNotNull(session, "session");
		MediaStatusResponse status = send(
			session,
			"urn:x-cast:com.google.cast.media",
			new Load(
				activeTrackIds,
				autoplay,
				credentials,
				credentialsType,
				currentTime,
				customData,
				loadOptions,
				media,
				playbackRate,
				queueData
			),
			session.sourceId,
			session.destinationId,
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
	 * @param session the {@link Session} to use.
	 * @param mediaSessionId the media session ID for which the play request
	 *            applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IllegalArgumentException If {@code session} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus play(
		@Nonnull Session session,
		int mediaSessionId,
		boolean synchronous
	) throws IOException {
		return play(session, mediaSessionId, synchronous, DEFAULT_RESPONSE_TIMEOUT);
	}

	/**
	 * Asks the remote application to start playing the media referenced by the
	 * specified media session ID.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param session the {@link Session} to use.
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
	 * @throws IllegalArgumentException If {@code session} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus play(
		@Nonnull Session session,
		int mediaSessionId,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		requireNotNull(session, "session");
		MediaStatusResponse status = send(
			session,
			"urn:x-cast:com.google.cast.media",
			new Play(mediaSessionId, session.id),
			session.sourceId,
			session.destinationId,
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
	 * @param session the {@link Session} to use.
	 * @param mediaSessionId the media session ID for which the pause request
	 *            applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IllegalArgumentException If {@code session} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus pause(
		@Nonnull Session session,
		int mediaSessionId,
		boolean synchronous
	) throws IOException {
		return pause(session, mediaSessionId, synchronous, DEFAULT_RESPONSE_TIMEOUT);
	}

	/**
	 * Asks the remote application to pause playback of the media referenced by
	 * the specified media session ID.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param session the {@link Session} to use.
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
	 * @throws IllegalArgumentException If {@code session} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus pause(
		@Nonnull Session session,
		int mediaSessionId,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		requireNotNull(session, "session");
		MediaStatusResponse status = send(
			session,
			"urn:x-cast:com.google.cast.media",
			new Pause(mediaSessionId, session.id),
			session.sourceId,
			session.destinationId,
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
	 * @param session the {@link Session} to use.
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
	 * @throws IllegalArgumentException If {@code session} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus seek(
		@Nonnull Session session,
		int mediaSessionId,
		double currentTime,
		@Nullable ResumeState resumeState,
		boolean synchronous
	) throws IOException {
		return seek(
			session,
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
	 * @param session the {@link Session} to use.
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
	 * @throws IllegalArgumentException If {@code session} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus seek(
		@Nonnull Session session,
		int mediaSessionId,
		double currentTime,
		@Nullable ResumeState resumeState,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		requireNotNull(session, "session");
		MediaStatusResponse status = send(
			session,
			"urn:x-cast:com.google.cast.media",
			new Seek(mediaSessionId, session.id, currentTime, resumeState),
			session.sourceId,
			session.destinationId,
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
	 * @param session the {@link Session} to use.
	 * @param mediaSessionId the media session ID for which the
	 *            {@link MediaVolume} request applies.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IllegalArgumentException If {@code session} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus stopMedia(@Nonnull Session session, int mediaSessionId, boolean synchronous) throws IOException {
		return stopMedia(session, mediaSessionId, synchronous, DEFAULT_RESPONSE_TIMEOUT);
	}

	/**
	 * Asks the remote application to stop playback and unload the media
	 * referenced by the specified media session ID.
	 * <p>
	 * This can only succeed if the remote application supports the
	 * "{@code urn:x-cast:com.google.cast.media}" namespace.
	 *
	 * @param session the {@link Session} to use.
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
	 * @throws IllegalArgumentException If {@code session} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus stopMedia(
		@Nonnull Session session,
		int mediaSessionId,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		requireNotNull(session, "session");
		MediaStatusResponse status = send(
			session,
			"urn:x-cast:com.google.cast.media",
			new StopMedia(mediaSessionId, null),
			session.sourceId,
			session.destinationId,
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
	 * @param session the {@link Session} to use.
	 * @param mediaSessionId the media session ID for which the
	 *            {@link MediaVolume} request applies.
	 * @param volume the {@link MediaVolume} to set.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link MediaStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IllegalArgumentException If {@code session} or {@code volume} is
	 *             {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus setMediaVolume(
		@Nonnull Session session,
		int mediaSessionId,
		@Nonnull MediaVolume volume,
		boolean synchronous
	) throws IOException {
		return setMediaVolume(
			session,
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
	 * @param session the {@link Session} to use.
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
	 *         {@code synchronous} is {@code false}.
	 * @throws IllegalArgumentException If {@code sessionId} or {@code volume}
	 *             is {@code null}.
	 * @throws IllegalArgumentException If {@code session} is {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public MediaStatus setMediaVolume(
		@Nonnull Session session,
		int mediaSessionId,
		@Nonnull MediaVolume volume,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		requireNotNull(session, "session");
		MediaStatusResponse status = send(
			session,
			"urn:x-cast:com.google.cast.media",
			new VolumeRequest(session.id, mediaSessionId, volume, null),
			session.sourceId,
			session.destinationId,
			synchronous ? MediaStatusResponse.class : null,
			responseTimeout
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	@Nullable
	public void setVolume(Volume volume) throws IOException { //TODO: (Nad) JavaDocs
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
				lastVolume.getControlType() == VolumeControlType.MASTER &&
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
						gradualVolumeTimer.schedule(task, 150, 150);
					} else if (gradualVolumeTimer != null) {
						gradualVolumeTimer.cancel();
						gradualVolumeTimer = null;
					}
				}
			}
		}
		doSetVolume(volume, false, DEFAULT_RESPONSE_TIMEOUT);
	}

	/**
	 * Sets the device volume level using the specified parameters, without any
	 * checks or evaluations.
	 *
	 * @param volume the {@link Volume} instance to send to the cast device.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code synchronous} is {@code true}. If zero or negative,
	 *            {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link ReceiverStatus} if {@code synchronous} is
	 *         {@code true} and a reply is received in time, {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	protected ReceiverStatus doSetVolume(Volume volume, boolean synchronous, long responseTimeout) throws IOException {
		ReceiverStatusResponse status = send(
			null,
			"urn:x-cast:com.google.cast.receiver",
			new SetVolume(volume),
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
	 * Sends the specified {@link Request} with the specified namespace using
	 * the specified {@link Session} and {@value #DEFAULT_RESPONSE_TIMEOUT} as
	 * the timeout value.
	 *
	 * @param <T> the class of the {@link Response} object.
	 * @param session the {@link Session} to use.
	 * @param namespace the namespace to use.
	 * @param request the {@link Request} to send.
	 * @param responseClass the response class to to block and wait for a
	 *            response or {@code null} to return immediately.
	 * @return The {@link Response} if the response is received in time, or
	 *         {@code null} if the {@code responseClass} is {@code null} or a
	 *         timeout occurs.
	 * @throws IllegalArgumentException If {@link Session} is {@code null} or if
	 *             {@code namespace} is {@code null} or invalid (see
	 *             {@link #validateNamespace(String)} for constraints).
	 * @throws IOException If an error occurs during the operation.
	 */
	public <T extends Response> T sendGenericRequest(
		@Nonnull Session session,
		@Nonnull String namespace,
		Request request,
		Class<T> responseClass
	) throws IOException {
		requireNotNull(session, "session");
		return send(
			session,
			namespace,
			request,
			session.sourceId,
			session.destinationId,
			responseClass, DEFAULT_RESPONSE_TIMEOUT
		);
	}

	/**
	 * Sends the specified {@link Request} with the specified namespace using
	 * the specified {@link Session}.
	 *
	 * @param <T> the class of the {@link Response} object.
	 * @param session the {@link Session} to use.
	 * @param namespace the namespace to use.
	 * @param request the {@link Request} to send.
	 * @param responseClass the response class to to block and wait for a
	 *            response or {@code null} to return immediately.
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code responseClass} is non-{@code null}. If zero or
	 *            negative, {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The {@link Response} if the response is received in time, or
	 *         {@code null} if the {@code responseClass} is {@code null} or a
	 *         timeout occurs.
	 * @throws IllegalArgumentException If {@link Session} is {@code null} or if
	 *             {@code namespace} is {@code null} or invalid (see
	 *             {@link #validateNamespace(String)} for constraints).
	 * @throws IOException If an error occurs during the operation.
	 */
	public <T extends Response> T sendGenericRequest(
		@Nonnull Session session,
		@Nonnull String namespace,
		Request request,
		Class<T> responseClass,
		long responseTimeout
	) throws IOException {
		requireNotNull(session, "session");
		return send(
			session,
			namespace,
			request,
			session.sourceId,
			session.destinationId,
			responseClass,
			responseTimeout
		);
	}

	/**
	 * Sends the specified {@link Request} with the specified namespace using
	 * the specified source and destination IDs and
	 * {@value #DEFAULT_RESPONSE_TIMEOUT} as the timeout value. This is for
	 * requests that aren't associated with a {@link Session}.
	 *
	 * @param <T> the class of the {@link Response} object.
	 * @param sourceId the source ID to use.
	 * @param destinationId the destination ID to use.
	 * @param namespace the namespace to use.
	 * @param request the {@link Request} to send.
	 * @param responseClass the response class to to block and wait for a
	 *            response or {@code null} to return immediately.
	 * @return The {@link Response} if the response is received in time, or
	 *         {@code null} if the {@code responseClass} is {@code null} or a
	 *         timeout occurs.
	 * @throws IllegalArgumentException If {@code namespace} is {@code null} or
	 *             invalid (see {@link #validateNamespace(String)} for
	 *             constraints).
	 * @throws IOException If an error occurs during the operation.
	 */
	public <T extends Response> T sendGenericRequest(
		@Nonnull String sourceId,
		@Nonnull String destinationId,
		@Nonnull String namespace,
		Request request,
		Class<T> responseClass
	) throws IOException {
		return send(null, namespace, request, sourceId, destinationId, responseClass, DEFAULT_RESPONSE_TIMEOUT);
	}

	/**
	 * Sends the specified {@link Request} with the specified namespace using
	 * the specified source and destination IDs. This is for requests that
	 * aren't associated with a {@link Session}.
	 *
	 * @param <T> the class of the {@link Response} object.
	 * @param sourceId the source ID to use.
	 * @param destinationId the destination ID to use.
	 * @param namespace the namespace to use.
	 * @param request the {@link Request} to send.
	 * @param responseClass the response class to to block and wait for a
	 *            response or {@code null} to return immediately.
	 * @param responseTimeout the response timeout in milliseconds if
	 *            {@code responseClass} is non-{@code null}. If zero or
	 *            negative, {@value #DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The {@link Response} if the response is received in time, or
	 *         {@code null} if the {@code responseClass} is {@code null} or a
	 *         timeout occurs.
	 * @throws IllegalArgumentException If {@code namespace} is {@code null} or
	 *             invalid (see {@link #validateNamespace(String)} for
	 *             constraints).
	 * @throws IOException If an error occurs during the operation.
	 */
	public <T extends Response> T sendGenericRequest(
		@Nonnull String sourceId,
		@Nonnull String destinationId,
		@Nonnull String namespace,
		Request request,
		Class<T> responseClass,
		long responseTimeout
	) throws IOException {
		return send(null, namespace, request, sourceId, destinationId, responseClass, responseTimeout);
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

	/**
	 * Writes the specified {@link Message} to the socket using the specified
	 * parameters.
	 *
	 * @param namespace the namespace to use.
	 * @param message the {@link Message} to write.
	 * @param sourceId the source ID to use.
	 * @param destinationId the destination ID to use.
	 * @throws IOException If an error occurs during the operation.
	 */
	protected void write(String namespace, Message message, String sourceId, String destinationId) throws IOException {
		write(namespace, jsonMapper.writeValueAsString(message), sourceId, destinationId);
	}

	/**
	 * Writes the specified ({@code JSON} formatted) {@link String} to the
	 * socket using the specified parameters.
	 *
	 * @param namespace the namespace to use.
	 * @param message the message content to write.
	 * @param sourceId the source ID to use.
	 * @param destinationId the destination ID to use.
	 * @throws IOException If an error occurs during the operation.
	 */
	protected void write(String namespace, String message, String sourceId, String destinationId) throws IOException {
		LOGGER.debug(CHROMECAST_API_MARKER, "Sending message to {}; \"{}\"", remoteName, message);
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
	 * Loops through any requests waiting for a response and cancels any that
	 * waits for a reply from the specified peer/destination ID. This is to be
	 * used when the {@link Session} with the specified peer/destination ID has
	 * been closed, since there's no point in waiting for a response that will
	 * never arrive.
	 *
	 * @param peerId the peer/destination ID that whose {@link Session} has been
	 *            closed.
	 * @return {@code true} if a waiting request was cancelled, {@code false}
	 *         otherwise.
	 */
	protected boolean cancelPendingClosed(@Nullable String peerId) {
		if (isBlank(peerId)) {
			return false;
		}
		boolean result = false;
		ResultProcessor<?> processor;
		Session session;
		synchronized (requests) {
			for (Iterator<ResultProcessor<?>> iterator = requests.values().iterator(); iterator.hasNext();) {
				processor = iterator.next();
				session = processor.session;
				if (session != null && peerId.equals(session.getDestinationId())) {
					processor.sessionClosed();
					iterator.remove();
					result = true;
				}
			}
		}
		return result;
	}

	/**
	 * Loops through all requests waiting for a response and cancels them. This
	 * is to be used when the connection disconnects, since there's no point in
	 * waiting for a response that will never arrive.
	 */
	protected void cancelPendingDisconnected() {
		ResultProcessor<?> processor;
		Session session;
		synchronized (requests) {
			for (Iterator<ResultProcessor<?>> iterator = requests.values().iterator(); iterator.hasNext();) {
				processor = iterator.next();
				session = processor.session;
				if (session != null) {
					processor.sessionClosed();
					iterator.remove();
				}
			}
		}
	}

	/**
	 * Reads the next {@link CastMessage} from the specified {@link InputStream}
	 * in a blocking fashion. This method will not return until a message is
	 * either completely read, {@code EOF} is reached or an {@link IOException}
	 * is thrown.
	 *
	 * @param inputStream the {@link InputStream} from which to read.
	 * @return The resulting {@link ImmutableCastMessage}.
	 * @throws IOException If {@code EOF} is reached or an error occurs-.
	 */
	@Nonnull
	protected static ImmutableCastMessage readMessage(InputStream inputStream) throws IOException {
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
		return ImmutableCastMessage.create(CastMessage.parseFrom(buf));
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
	 * @return The new {@link Executor}.
	 */
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
		 *
		 * @throws AssertionError If the {@code JSON} mapper can't serialize the
		 *             {@code PING} message.
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
					LOGGER.warn(
						CHROMECAST_API_MARKER,
						"An error occurred while gradually adjusting the volume " +
						"level of {}, stopping gradual adjustment: {}", remoteName,
						e.getMessage()
					);
					LOGGER.trace(CHROMECAST_API_MARKER, "", e);
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

	/**
	 * A {@link Thread} implementation tailored to process incoming messages on
	 * the socket.
	 *
	 * @author Nadahar
	 */
	protected class InputHandler extends Thread {

		private volatile boolean running;

		@Nonnull
		private final InputStream is;

		@Nonnull
		private final CastMessage pongMessage;

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
			ImmutableCastMessage message = null;
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
					if (message instanceof ImmutableStringCastMessage) {
						jsonMessage = ((ImmutableStringCastMessage) message).getPayload();
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
						LOGGER.debug(
							CHROMECAST_API_MARKER,
							"{} InputHandler: Received string message \"{}\"",
							remoteName,
							jsonMessage
						);
						EXECUTOR.execute(new StringMessageHandler((ImmutableStringCastMessage) message));
					} else if (message != null) {
						LOGGER.debug(
							CHROMECAST_API_MARKER,
							"{} InputHandler: Received message with binary payload ({} bytes)",
							remoteName,
							((ImmutableBinaryCastMessage) message).getPayload() == null ?
								"unknown number of" :
								((ImmutableBinaryCastMessage) message).getPayload().size()
						);
						EXECUTOR.execute(new BinaryMessageHandler((ImmutableBinaryCastMessage) message));
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
						sb.append("namespace: ").append(message.getNamespace());
						sb.append(", protocol version: ").append(message.getProtocolVersion().getNumber());
						if (message instanceof ImmutableStringCastMessage) {
							sb.append(", string payload: ").append(((ImmutableStringCastMessage) message).getPayload());
						} else {
							sb.append(", binary payload: ").append(((ImmutableBinaryCastMessage) message).getPayload());
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

		/**
		 * Tells this {@link InputHandler} to stop processing and shut down.
		 */
		public void stopProcessing() {
			running = false;
		}
	}

	/**
	 * A {@link Runnable} implementation for processing a single incoming
	 * string-based message.
	 *
	 * @author Nadahar
	 */
	protected class StringMessageHandler implements Runnable {

		/** The message to process */
		@Nonnull
		protected final ImmutableStringCastMessage message;

		/** The string payload of the message */
		@Nonnull
		protected final String jsonMessage;

		/**
		 * Creates a new handler for a single string-based message.
		 *
		 * @param message the message to process.
		 * @throws IllegalArgumentException If {@code message} is {@code null}
		 *             or the string payload is blank.
		 */
		public StringMessageHandler(@Nonnull ImmutableStringCastMessage message) {
			requireNotNull(message, "message");
			this.message = message;
			this.jsonMessage = message.getPayload().replaceFirst("\"type\"", "\"responseType\"");
			requireNotBlank(jsonMessage, "jsonMessage");
		}

		@Override
		public void run() {
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
					if (!listeners.isEmpty()) {
						listeners.fire(new DefaultCastEvent<>(
							CastEventType.CUSTOM_MESSAGE,
							new CustomMessageEvent(
								message.getSourceId(),
								message.getDestinationId(),
								message.getNamespace(),
								message.getPayload()
							)
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
							Session session;
							Set<Session> closedNow = new HashSet<>();
							synchronized (sessionsLock) {
								for (Iterator<Session> iterator = sessions.iterator(); iterator.hasNext();) {
									session = iterator.next();
									if (peerId.equals(session.getDestinationId())) {
										closedNow.add(session);
										iterator.remove();
									}
								}
							}
							if (!closedNow.isEmpty()) {
								cancelPendingClosed(peerId);
								SessionClosedListener closedListener;
								for (Session tmpSession : closedNow) {
									closedListener = tmpSession.getSessionClosedListener();
									if (closedListener != null) {
										closedListener.closed(tmpSession);
									}
								}
							} else {
								if (!cancelPendingClosed(peerId)) {
									// Didn't match any "known" session, pass it on to listeners
									if (!listeners.isEmpty()) {
										listeners.fire(new DefaultCastEvent<>(
											CastEventType.CLOSE,
											new CloseMessageEvent(
												message.getSourceId(),
												message.getDestinationId(),
												message.getNamespace()
											)
										));
									}
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

					if (response instanceof StandardResponse && response.getEventType() != null) {
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

		protected final ImmutableBinaryCastMessage message;

		public BinaryMessageHandler(@Nonnull ImmutableBinaryCastMessage message) {
			requireNotNull(message, "message");
			this.message = message;
		}

		@Override
		public void run() {
			if (!listeners.isEmpty()) {
				listeners.fire(new DefaultCastEvent<>(CastEventType.CUSTOM_MESSAGE, new CustomMessageEvent(
					message.getSourceId(),
					message.getDestinationId(),
					message.getNamespace(),
					message.getPayload()
				)));
			}
		}
	}

	protected class ResultProcessor<T extends Response> {

		@Nullable
		private final Session session;
		private final Class<T> responseClass;
		private final long requestTimeout;
		private boolean closed;
		private ResultProcessorResult<T> result;

		public ResultProcessor(@Nullable Session session, @Nonnull Class<T> responseClass, long requestTimeout) {
			requireNotNull(responseClass, "responseClass");
			this.session = session;
			this.responseClass = responseClass;
			this.requestTimeout = requestTimeout < 1 ? DEFAULT_RESPONSE_TIMEOUT : requestTimeout;
		}

		public void sessionClosed() {
			synchronized (this) {
				closed = true;
				this.notifyAll();
			}
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
				this.notifyAll();
			}
		}

		@Nonnull
		public ResultProcessorResult<T> get() throws InterruptedException, TimeoutException, ChromeCastException {
			synchronized (this) {
				if (result != null) {
					return result;
				}
				this.wait(requestTimeout);
				if (closed) {
					throw new ChromeCastException("The session was closed by the cast device");
				}
				if (result == null) {
					throw new TimeoutException();
				}
				return result;
			}
		}

		@Nullable
		public Session getSession() {
			return session;
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
