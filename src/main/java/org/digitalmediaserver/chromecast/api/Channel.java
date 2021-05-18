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
import org.digitalmediaserver.chromecast.api.CastException.InvalidCastException;
import org.digitalmediaserver.chromecast.api.CastException.LaunchErrorCastException;
import org.digitalmediaserver.chromecast.api.CastException.LoadCancelledCastException;
import org.digitalmediaserver.chromecast.api.CastException.LoadFailedCastException;
import org.digitalmediaserver.chromecast.api.Session.SessionClosedListener;
import org.digitalmediaserver.chromecast.api.StandardMessage.CloseConnection;
import org.digitalmediaserver.chromecast.api.StandardMessage.Connect;
import org.digitalmediaserver.chromecast.api.StandardMessage.Ping;
import org.digitalmediaserver.chromecast.api.StandardMessage.Pong;
import org.digitalmediaserver.chromecast.api.StandardRequest.GetAppAvailability;
import org.digitalmediaserver.chromecast.api.StandardRequest.GetStatus;
import org.digitalmediaserver.chromecast.api.StandardRequest.Launch;
import org.digitalmediaserver.chromecast.api.StandardRequest.Load;
import org.digitalmediaserver.chromecast.api.StandardRequest.Pause;
import org.digitalmediaserver.chromecast.api.StandardRequest.Play;
import org.digitalmediaserver.chromecast.api.StandardRequest.Seek;
import org.digitalmediaserver.chromecast.api.StandardRequest.SetVolume;
import org.digitalmediaserver.chromecast.api.StandardRequest.Stop;
import org.digitalmediaserver.chromecast.api.StandardResponse.AppAvailabilityResponse;
import org.digitalmediaserver.chromecast.api.StandardResponse.InvalidResponse;
import org.digitalmediaserver.chromecast.api.StandardResponse.LaunchErrorResponse;
import org.digitalmediaserver.chromecast.api.StandardResponse.LoadCancelledResponse;
import org.digitalmediaserver.chromecast.api.StandardResponse.LoadFailedResponse;
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

	static {
		CAST_API_HEARTBEAT_MARKER.add(CAST_API_MARKER);
	}

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
				new Connect(null, VirtualConnectionType.STRONG),
				PLATFORM_SENDER_ID,
				PLATFORM_RECEIVER_ID
			);

			// Start regular pinging
			PingTask pingTask = new PingTask();
			pingTimer = new Timer(remoteName + " PING timer");
			pingTimer.schedule(pingTask, 1000, PING_PERIOD);
		}

		// Reset the cached volume on every connect
		synchronized (cachedVolumeLock) {
			cachedVolume = null;
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
				socket = null;
			}
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
	 *             response from the cast device is a different type than what
	 *             was expected, or if an error occurs during the operation.
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
			if (response.untypedResult instanceof InvalidResponse) {
				InvalidResponse invalid = (InvalidResponse) response.untypedResult;
				throw new InvalidCastException("Invalid request: " + invalid.getReason());
			} else if (response.untypedResult instanceof LoadCancelledResponse) {
				throw new LoadCancelledCastException(
					"Loading of media was cancelled",
					((LoadCancelledResponse) response.untypedResult).getItemId()
				);
			} else if (response.untypedResult instanceof LoadFailedResponse) {
				throw new LoadFailedCastException("Unable to load media");
			} else if (response.untypedResult instanceof LaunchErrorResponse) {
				throw new LaunchErrorCastException("Application launch error: " + ((LaunchErrorResponse) response.untypedResult).getReason());
			}
			throw new CastException(
				"Failed to deserialize response to " + responseClass.getSimpleName()
			);
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
	@Nullable
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
				new Connect(userAgent, connectionType),
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
		SessionClosedListener listener = session.getSessionClosedListener();
		if (listener != null) {
			listener.closed(session);
		}
		write(
			"urn:x-cast:com.google.cast.tp.connection",
			new CloseConnection(),
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

	public MediaStatus load(
		@Nonnull Session session,
		Media media,
		boolean autoplay,
		double currentTime,
		boolean synchronous,
		@Nullable Map<String, Object> customData
	) throws IOException {
		return load(
			session,
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
		Media media,
		boolean autoplay,
		double currentTime,
		boolean synchronous,
		long responseTimeout,
		@Nullable Map<String, Object> customData
	) throws IOException {
		requireNotNull(session, "session");
		MediaStatusResponse status = send(
			session,
			"urn:x-cast:com.google.cast.media",
			new Load(
				session.id,
				media,
				autoplay,
				currentTime,
				customData
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
		boolean synchronous
	) throws IOException {
		return seek(
			session,
			mediaSessionId,
			currentTime,
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
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		requireNotNull(session, "session");
		MediaStatusResponse status = send(
			session,
			"urn:x-cast:com.google.cast.media",
			new Seek(mediaSessionId, session.id, currentTime),
			session.sourceId,
			session.destinationId,
			synchronous ? MediaStatusResponse.class : null,
			responseTimeout
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	/**
	 * Sets the device {@link Volume} to the values of the specified instance. A
	 * {@link Volume} instance contains both the volume level and the mute
	 * state.
	 * <p>
	 * If the device has {@link VolumeControlType#MASTER} and the this call
	 * changes the volume level more than the device specified "step", this
	 * method will start a {@link Timer} based gradual change towards the
	 * specified volume level.
	 *
	 * @param volume the {@link Volume} instance whose values to set.
	 * @throws CastException If the cast device has
	 *             {@link VolumeControlType#FIXED}.
	 * @throws IOException If an error occurs during the operation.
	 */
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
				throw new CastException("Cannot set volume level or mute since the device has a fixed volume");
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
			responseClass,
			DEFAULT_RESPONSE_TIMEOUT
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
		boolean setVolume;
		synchronized (cachedVolumeLock) {
			setVolume = cachedVolume == null;
			cachedVolume = volume;
		}

		if (
			setVolume &&
			volume.getControlType() != VolumeControlType.FIXED &&
			volume.getLevel() != null &&
			volume.getLevel().doubleValue() < 1.0
		) {
			// There's a long-standing bug in the ChromeCast software that resets
			// the device volume to 100% when the device is restarted. It correctly
			// reports the volume level it's supposed to have, but audio is played
			// back with 100% volume regardless. This little trick is to mitigate
			// the bug by setting the volume to what the device reports the first
			// time we receive one. Hopefully this won't negatively impact devices
			// without the bug.
			final double targetLevel = volume.getLevel().doubleValue();
			final double interrimLevel;
			if (targetLevel > 0.1) {
				interrimLevel = targetLevel - 0.05;
			} else {
				interrimLevel = targetLevel + 0.05;
			}

			CastDevice.EXECUTOR.execute(new Runnable() {

				@Override
				public void run() {
					try {
						send(
							null,
							"urn:x-cast:com.google.cast.receiver",
							new SetVolume(new Volume(null, Double.valueOf(interrimLevel), null, null)),
							PLATFORM_SENDER_ID,
							PLATFORM_RECEIVER_ID,
							null,
							0L
						);
					} catch (IOException e) {
						LOGGER.warn(
							CAST_API_MARKER,
							"Failed to set the initial interrim volume on {}: {}",
							remoteName,
							e.getMessage()
						);
						LOGGER.trace(CAST_API_MARKER, "", e);
					}
				}
			});
			CastDevice.EXECUTOR.execute(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(150L);
						send(
							null,
							"urn:x-cast:com.google.cast.receiver",
							new SetVolume(new Volume(null, Double.valueOf(targetLevel), null, null)),
							PLATFORM_SENDER_ID,
							PLATFORM_RECEIVER_ID,
							null,
							0L
						);
					} catch (IOException | InterruptedException e) {
						LOGGER.warn(
							CAST_API_MARKER,
							"Failed to set the initial volume on {}: {}",
							remoteName,
							e.getMessage()
						);
						LOGGER.trace(CAST_API_MARKER, "", e);
					}
				}
			});
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

	/**
	 * A {@link TimerTask} that will gradually increase or decrease the volume
	 * level of the cast device until the target level is reached.
	 *
	 * @author Nadahar
	 */
	protected class GradualVolumeTask extends TimerTask {

		/** The interval */
		protected final double interval;

		/** The target level */
		protected final double target;

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
						CAST_API_MARKER,
						"An error occurred while gradually adjusting the volume " +
						"level of {}, stopping gradual adjustment: {}", remoteName,
						e.getMessage()
					);
					LOGGER.trace(CAST_API_MARKER, "", e);
					shutdownTask();
				}
			}
		}

		/**
		 * Shuts down this task.
		 */
		protected void shutdownTask() {
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
					if (PLATFORM_RECEIVER_ID.equals(message.getSourceId())) {
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
								SessionClosedListener closedListener;
								for (Session tmpSession : closedNow) {
									closedListener = tmpSession.getSessionClosedListener();
									if (closedListener != null) {
										closedListener.closed(tmpSession);
									}
								}
							} else {
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
				} else {
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

		/** The {@link Session} if one applies */
		@Nullable
		protected final Session session;

		/** The expected response {@link Class} */
		protected final Class<T> responseClass;

		/** The timeout in milliseconds */
		protected final long requestTimeout;

		/** Whether the associated {@link Session} has been closed */
		@GuardedBy("this")
		protected boolean closed;

		/** The {@link ResultProcessorResult} */
		@GuardedBy("this")
		protected ResultProcessorResult<T> result;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param session the {@link Session} if one applies to the request.
		 * @param responseClass the expected response class.
		 * @param requestTimeout the timeout value in milliseconds.
		 */
		public ResultProcessor(@Nullable Session session, @Nonnull Class<T> responseClass, long requestTimeout) {
			requireNotNull(responseClass, "responseClass");
			this.session = session;
			this.responseClass = responseClass;
			this.requestTimeout = requestTimeout < 1 ? DEFAULT_RESPONSE_TIMEOUT : requestTimeout;
		}

		/**
		 * Called if the {@link Session} linked to this {@link ResultProcessor}
		 * is closed.
		 */
		public void sessionClosed() {
			synchronized (this) {
				closed = true;
				this.notifyAll();
			}
		}

		/**
		 * Processes the specified message that has been routed to this
		 * {@link ResultProcessor} by its request ID.
		 *
		 * @param jsonMSG the message content formatted as JSON.
		 * @throws JsonMappingException If the JSON mapping fails.
		 * @throws JsonProcessingException If the JSON can't be processed.
		 */
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

		/**
		 * Returns the {@link ResultProcessorResult} when it is due. If the
		 * response hasn't been received yet, this method will block until it is
		 * or the timeout expires.
		 *
		 * @return The response.
		 * @throws InterruptedException If the thread is interrupted while
		 *             waiting for the response.
		 * @throws TimeoutException If waiting for the response times out.
		 * @throws CastException If the {@link Session} is closed while waiting
		 *             for the response.
		 */
		@Nonnull
		public ResultProcessorResult<T> get() throws InterruptedException, TimeoutException, CastException {
			synchronized (this) {
				if (result != null) {
					return result;
				}
				this.wait(requestTimeout);
				if (closed) {
					throw new CastException("The session was closed by the cast device");
				}
				if (result == null) {
					throw new TimeoutException();
				}
				return result;
			}
		}

		/**
		 * @return The {@link Session} tied to this {@link ResultProcessor}, if
		 *         any.
		 */
		@Nullable
		public Session getSession() {
			return session;
		}
	}

	/**
	 * A holder for the resulting {@link Response}.
	 *
	 * @param <T> the response type.
	 */
	protected class ResultProcessorResult<T extends Response> {

		/** The {@link Response} as the expected type */
		@Nullable
		protected final T typedResult;

		/** The {@link Response} as an unexpected type */
		@Nullable
		protected final StandardResponse untypedResult;

		/** The response as JSON */
		@Nullable
		protected final String unprocessedResult;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param typedResult the {@link Response} as the expected type.
		 * @param untypedResult the {@link Response} as an unexpected type.
		 * @param unprocessedResult the response as a JSON.
		 */
		public ResultProcessorResult(
			@Nullable T typedResult,
			@Nullable StandardResponse untypedResult,
			@Nullable String unprocessedResult
		) {
			this.typedResult = typedResult;
			this.untypedResult = untypedResult;
			this.unprocessedResult = unprocessedResult;
		}
	}
}
