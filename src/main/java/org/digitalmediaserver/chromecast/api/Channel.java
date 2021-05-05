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

import static org.digitalmediaserver.chromecast.api.Util.intFromB32Bytes;
import static org.digitalmediaserver.chromecast.api.Util.readB32Int;
import static org.digitalmediaserver.chromecast.api.Util.writeB32Int;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import org.digitalmediaserver.chromecast.api.CastChannel.CastMessage;
import org.digitalmediaserver.chromecast.api.CastChannel.CastMessage.PayloadType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Internal class for low-level communication with ChromeCast device. Should
 * never be used directly, use {@link ChromeCast} methods instead
 */
public class Channel implements Closeable {

	private static final Logger LOGGER = LoggerFactory.getLogger(Channel.class); //TODO: (Nad) Marker
	public static final Marker CHROMECAST_API_MARKER = MarkerFactory.getMarker("chromecast-api");

	/**
	 * Period for sending ping requests (in ms)
	 */
	private static final long PING_PERIOD = 10 * 1000; //TODO: (Nad) 5 sec suggested in doc, was 30

	/**
	 * Default value of much time to wait until request is processed
	 */
	private static final long DEFAULT_REQUEST_TIMEOUT = 30 * 1000;

	private static final String DEFAULT_RECEIVER_ID = "receiver-0";

	private final EventListenerHolder eventListener;

	private static final JsonSubTypes.Type[] STANDARD_RESPONSE_TYPES =
		StandardResponse.class.getAnnotation(JsonSubTypes.class).value();

	private static void warn(String message, Exception ex) {
		LOGGER.warn(CHROMECAST_API_MARKER, "{}, caused by {}", message, ex.toString());
	}

	/**
	 * Single socket instance for transfers
	 */
	private Socket socket;

	/**
	 * Address of ChromeCast
	 */
	private final InetSocketAddress address;

	/**
	 * Name of sender used in this channel
	 */
	private final String name;

	/**
	 * Timer for PING requests
	 */
	private Timer pingTimer;

	/**
	 * Thread for processing incoming requests
	 */
	private ReadThread reader;

	/**
	 * Counter for producing request numbers
	 */
	protected final AtomicLong requestCounter = new AtomicLong(new Random().nextInt(65536) + 1L);

	/**
	 * Processors of requests by their identifiers
	 */
	private final Map<Long, ResultProcessor<? extends Response>> requests = new ConcurrentHashMap<>();

	/**
	 * Single mapper object for marshalling JSON
	 */
	@Nonnull
	private final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();

	/**
	 * Destination ids of sessions opened within this channel
	 */
	private Set<String> sessions = new HashSet<>();

	/**
	 * Indicates that this channel was closed (explicitly, by remote host or for
	 * some connectivity issue)
	 */
	private volatile boolean closed = true;
	private final Object closedSync = new Object();

	/**
	 * How much time to wait until request is processed
	 */
	private volatile long requestTimeout = DEFAULT_REQUEST_TIMEOUT;

	protected Channel(String host, EventListenerHolder eventListener) {
		this(host, 8009, eventListener);
	}

	protected Channel(String host, int port, EventListenerHolder eventListener) {
		this.address = new InetSocketAddress(host, port);
		this.name = "sender-" + new RandomString(10).nextString(); //TODO: (Nad) Parameterize?
		this.eventListener = eventListener;
	}

	/**
	 * Open the channel.
	 *
	 * <p>
	 * This function must be called before any other usage.
	 * </p>
	 *
	 * @throws IOException
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 */
	public void open() throws IOException, KeyManagementException, NoSuchAlgorithmException {
		if (!closed) {
			throw new ChromeCastException("Channel already opened.");
		}
		connect();
	}

	/**
	 * Establish connection to the ChromeCast device.
	 *
	 * @throws IOException
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 */
	private void connect() throws IOException, KeyManagementException, NoSuchAlgorithmException {
		synchronized (closedSync) {
			if (socket == null || socket.isClosed()) {
				SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, new TrustManager[] {new X509TrustAllManager()}, new SecureRandom());
				socket = sc.getSocketFactory().createSocket();
				socket.connect(address);
			}
			// Authenticate.
			CastChannel.DeviceAuthMessage authMessage = CastChannel.DeviceAuthMessage.newBuilder()
				.setChallenge(CastChannel.AuthChallenge.newBuilder().build())
				.build();

			CastMessage msg = CastMessage.newBuilder()
				.setDestinationId(DEFAULT_RECEIVER_ID)
				.setNamespace("urn:x-cast:com.google.cast.tp.deviceauth")
				.setPayloadType(CastMessage.PayloadType.BINARY)
				.setProtocolVersion(CastMessage.ProtocolVersion.CASTV2_1_0)
				.setSourceId(name)
				.setPayloadBinary(authMessage.toByteString())
				.build();

			write(msg);
			CastMessage response = read();
			CastChannel.DeviceAuthMessage authResponse = CastChannel.DeviceAuthMessage.parseFrom(response.getPayloadBinary());
			if (authResponse.hasError()) {
				throw new ChromeCastException("Authentication failed: " + authResponse.getError().getErrorType().toString());
			}

			// Send 'PING' message
			PingTask pingTask = new PingTask(); //TODO: (Nad) Why ping before connect..?
//			pingTask.run();

			// Send 'CONNECT' message to start session
			write("urn:x-cast:com.google.cast.tp.connection", StandardMessage.connect(), DEFAULT_RECEIVER_ID);

			// Start ping/pong and reader thread
			pingTimer = new Timer(name + " PING");
			pingTimer.schedule(pingTask, 1000, PING_PERIOD);

			reader = new ReadThread();
			reader.start();

			if (closed) {
				closed = false;
				notifyListenerOfConnectionEvent(true);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends StandardResponse> T sendStandard(
		String namespace,
		StandardRequest message,
		String destinationId
	) throws IOException {
		return send(namespace, message, destinationId, (Class<T>) StandardResponse.class);
	}

	private <T extends Response> T send(
		String namespace,
		Request message,
		String destinationId,
		Class<T> responseClass
	) throws IOException {
		// Try to reconnect
		if (isClosed()) {
			try {
				connect();
			} catch (GeneralSecurityException gse) {
				throw new ChromeCastException("Unexpected security exception", gse);
			}
		}

		Long requestId = requestCounter.getAndIncrement();
		message.setRequestId(requestId);
		if (!requestId.equals(message.getRequestId())) {
			throw new IllegalStateException("Request Id getter/setter contract violation"); //TODO: (Nad) Runtime.. bad?
		}

		if (responseClass == null) {
			write(namespace, message, destinationId);
			return null;
		}

		ResultProcessor<T> rp = new ResultProcessor<>(responseClass);
		requests.put(requestId, rp);

		write(namespace, message, destinationId);
		try {
			T response = rp.get();
			if (response instanceof StandardResponse.InvalidResponse) {
				StandardResponse.InvalidResponse invalid = (StandardResponse.InvalidResponse) response;
				throw new ChromeCastException("Invalid request: " + invalid.getReason());
			} else if (response instanceof StandardResponse.LoadFailedResponse) {
				throw new ChromeCastException("Unable to load media");
			} else if (response instanceof StandardResponse.LaunchErrorResponse) {
				StandardResponse.LaunchErrorResponse launchError = (StandardResponse.LaunchErrorResponse) response;
				throw new ChromeCastException("Application launch error: " + launchError.getReason());
			}
			return response;
		} catch (InterruptedException e) {
			throw new ChromeCastException("Interrupted while waiting for response", e);
		} catch (TimeoutException e) {
			throw new ChromeCastException("Waiting for response timed out", e);
		} finally {
			requests.remove(requestId);
		}
	}

	private void write(String namespace, Message message, String destinationId) throws IOException {
		write(namespace, jsonMapper.writeValueAsString(message), destinationId);
	}

	private void write(String namespace, String message, String destinationId) throws IOException {
		LOGGER.debug(CHROMECAST_API_MARKER, " --> {}", message);
		CastMessage msg = CastMessage.newBuilder()
			.setProtocolVersion(CastMessage.ProtocolVersion.CASTV2_1_0)
			.setSourceId(name)
			.setDestinationId(destinationId)
			.setNamespace(namespace)
			.setPayloadType(CastMessage.PayloadType.STRING)
			.setPayloadUtf8(message)
			.build();
		write(msg);
	}

	private void write(CastMessage message) throws IOException {
		OutputStream os = socket.getOutputStream();
		writeB32Int(message.getSerializedSize(), os);
		message.writeTo(os);
	}

	private CastMessage read() throws IOException {
		InputStream is = socket.getInputStream();
		byte[] buf = new byte[4];

		int read = 0;
//		while (read < buf.length) {
//			int nextByte = is.read();
//			if (nextByte == -1) {
//				throw new ChromeCastException("Remote socket closed after reading " + read + " of " + buf.length + " bytes");
//			}
//			buf[read++] = (byte) nextByte;
//		}
//
//		int size = intFromB32Bytes(buf);
		int size = readB32Int(is);
		buf = new byte[size];
		read = 0;
		while (read < size) {
			int nowRead = is.read(buf, read, buf.length - read);
			if (nowRead == -1) {
				throw new ChromeCastException("Remote socket closed after reading " + read + " of " + size + " bytes");
			}
			read += nowRead;
		}

		return CastMessage.parseFrom(buf);
	}

	private void notifyListenerOfConnectionEvent(final boolean connected) {
		if (this.eventListener != null) {
			this.eventListener.deliverConnectionEvent(connected);
		}
	}

	private void notifyListenersOfSpontaneousEvent(JsonNode json) throws JsonProcessingException {
		if (this.eventListener != null) {
			this.eventListener.deliverEvent(json);
		}
	}

	private void notifyListenersAppEvent(AppEvent event) {
		if (this.eventListener != null) {
			this.eventListener.deliverAppEvent(event);
		}
	}

	public Status getStatus() throws IOException {
		StandardResponse.StatusResponse status = sendStandard(
			"urn:x-cast:com.google.cast.receiver",
			StandardRequest.status(),
			DEFAULT_RECEIVER_ID
		);
		return status == null ? null : status.getStatus();
	}

	public boolean isAppAvailable(String appId) throws IOException {
		StandardResponse.AppAvailabilityResponse availability = sendStandard(
			"urn:x-cast:com.google.cast.receiver",
			StandardRequest.appAvailability(appId),
			DEFAULT_RECEIVER_ID
		);
		return availability != null && "APP_AVAILABLE".equals(availability.getAvailability().get(appId));
	}

	public Status launch(String appId) throws IOException {
		StandardResponse.StatusResponse status = sendStandard(
			"urn:x-cast:com.google.cast.receiver",
			StandardRequest.launch(appId),
			DEFAULT_RECEIVER_ID
		);
		return status == null ? null : status.getStatus();
	}

	public Status stop(String sessionId) throws IOException {
		StandardResponse.StatusResponse status = sendStandard(
			"urn:x-cast:com.google.cast.receiver",
			StandardRequest.stop(sessionId),
			DEFAULT_RECEIVER_ID
		);
		return status == null ? null : status.getStatus();
	}

	private void startSession(String destinationId) throws IOException {
		if (!sessions.contains(destinationId)) {
			write("urn:x-cast:com.google.cast.tp.connection", StandardMessage.connect(), destinationId);
			sessions.add(destinationId);
		}
	}

	public MediaStatus load(String destinationId, String sessionId, Media media, boolean autoplay, double currentTime,
		Map<String, String> customData) throws IOException {
		startSession(destinationId);
		StandardResponse.MediaStatusResponse status = sendStandard(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.load(sessionId, media, autoplay, currentTime, customData),
			destinationId
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	public MediaStatus play(String destinationId, String sessionId, long mediaSessionId) throws IOException {
		startSession(destinationId);
		StandardResponse.MediaStatusResponse status = sendStandard(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.play(sessionId, mediaSessionId),
			destinationId
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	public MediaStatus pause(String destinationId, String sessionId, long mediaSessionId) throws IOException {
		startSession(destinationId);
		StandardResponse.MediaStatusResponse status = sendStandard(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.pause(sessionId, mediaSessionId),
			destinationId
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	public MediaStatus seek(String destinationId, String sessionId, long mediaSessionId, double currentTime) throws IOException {
		startSession(destinationId);
		StandardResponse.MediaStatusResponse status = sendStandard(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.seek(sessionId, mediaSessionId, currentTime),
			destinationId
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	public Status setVolume(Volume volume) throws IOException {
		StandardResponse.StatusResponse status = sendStandard(
			"urn:x-cast:com.google.cast.receiver",
			StandardRequest.setVolume(volume),
			DEFAULT_RECEIVER_ID
		);
		return status == null ? null : status.getStatus();
	}

	public MediaStatus getMediaStatus(String destinationId) throws IOException {
		startSession(destinationId);
		StandardResponse.MediaStatusResponse status = sendStandard(
			"urn:x-cast:com.google.cast.media",
			StandardRequest.status(),
			destinationId
		);
		return status == null || status.getStatuses().isEmpty() ? null : status.getStatuses().get(0);
	}

	public <T extends Response> T sendGenericRequest(
		String destinationId,
		String namespace,
		Request request,
		Class<T> responseClass
	) throws IOException {
		startSession(destinationId);
		return send(namespace, request, destinationId, responseClass);
	}

	@Override
	public void close() throws IOException {
		synchronized (closedSync) {
			if (closed) {
				throw new ChromeCastException("Channel already closed.");
			} else {
				closed = true;
				notifyListenerOfConnectionEvent(false);
				if (pingTimer != null) {
					pingTimer.cancel();
				}
				if (reader != null) {
					reader.stop = true;
				}
				if (socket != null) {
					socket.close();
				}
			}
		}
	}

	public boolean isClosed() {
		return closed;
	}

	public void setRequestTimeout(long requestTimeout) {
		this.requestTimeout = requestTimeout;
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
		return !parsedMessage.has("requestId");
	}

	protected class PingTask extends TimerTask {

		private final String messageString;
		private final CastMessage message;

		public PingTask() {
			try {
				messageString = jsonMapper.writeValueAsString(StandardMessage.ping());
			} catch (JsonProcessingException e) {
				throw new AssertionError("Couldn't generate JSON for 'ping' message");
			}
			message = CastMessage.newBuilder()
				.setProtocolVersion(CastMessage.ProtocolVersion.CASTV2_1_0)
				.setSourceId(name)
				.setDestinationId(DEFAULT_RECEIVER_ID)
				.setNamespace("urn:x-cast:com.google.cast.tp.heartbeat")
				.setPayloadType(CastMessage.PayloadType.STRING)
				.setPayloadUtf8(messageString)
				.build();
		}

		@Override
		public void run() {
			if (LOGGER.isDebugEnabled(CHROMECAST_API_MARKER)) {
				LOGGER.debug(CHROMECAST_API_MARKER, " --> {}", messageString);
			}
			try {
				write(message);
			} catch (IOException e) {
				LOGGER.warn(CHROMECAST_API_MARKER, "An error occurred while sending ChromeCast 'PING': {}", e.getMessage());
				LOGGER.trace(CHROMECAST_API_MARKER, "", e);
			}
		}
	}

	protected static class InputProcessor extends Thread {

		@Nonnull
		private final InputStream is;

		@Nonnull
		private final ObjectMapper jsonMapper; //TODO: (Nad) Needed?

		public InputProcessor(@Nonnull InputStream inputStream, @Nonnull ObjectMapper jsonMapper) {
			if (inputStream == null) {
				throw new IllegalArgumentException("inputStream cannot be null");
			}
			if (jsonMapper == null) {
				throw new IllegalArgumentException("jsonMapper cannot be null");
			}
			this.is = inputStream;
			this.jsonMapper = jsonMapper;
		}

		@Override
		public void run() {
			JsonNode parsedMessage;
			String jsonMessage;
			CastMessage message;
			PayloadType payloadType;
			final Thread currentThread = Thread.currentThread();
			while (!currentThread.isInterrupted()) { //TODO: (Nad) Figure out
				try {
					message = readMessage();
					if (message != null && (payloadType = message.getPayloadType()) != null) {
						switch (payloadType) {
							case BINARY:
								//TODO: (Nad) Log received package
								break;
							case STRING:
								jsonMessage = message.getPayloadUtf8();
								if (Util.isBlank(jsonMessage)) {
//									LOGGER.warn(CHROMECAST_API_MARKER, " <-- Received empty message. Ignore."); //TODO: (Nad) Log
									continue;
								}
								//TODO: (Nad) Log received package
								jsonMessage = jsonMessage.replaceFirst("\"type\"", "\"responseType\"");
								try {
									parsedMessage = jsonMapper.readTree(jsonMessage);
								} catch (JsonProcessingException e) {
									//TODO: (Nad) Log
									continue;
								}
								break;
							default:
								//TODO: (Nad) Handle...
								break;

						}
					} else if (message != null) {
						//TODO: (Nad) Log missing payload type
					} else {
						//TODO: (Nad) Log null message
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(); //TODO: (Nad) Figure out how to handle exceptions
				}
			}
		}

		@Nullable
		protected CastMessage readMessage() throws IOException {
			int size = readB32Int(is);
			byte[] buf = new byte[size];
			int read = 0;
			while (read < size) {
				int readNow = is.read(buf, read, buf.length - read);
				if (readNow == -1) {
					throw new ChromeCastException("Incomplete message, ended after reading " + read + " of " + size + " bytes");
				}
				read += readNow;
			}

			return CastMessage.parseFrom(buf);
		}
	}

	protected class StringMessageHandler implements Runnable { //TODO: (Nad) Temp test, name, move etc

		@Nonnull
		protected final String jsonMessage;

		public StringMessageHandler(@Nonnull String jsonMessage) {
			if (Util.isBlank(jsonMessage)) {
				throw new IllegalArgumentException("jsonMessage cannot be blank");
			}
			this.jsonMessage = jsonMessage;
		}

		@Override
		public void run() {
			try {
				JsonNode parsedMessage = jsonMapper.readTree(jsonMessage);
				if (parsedMessage == null || isCustomMessage(parsedMessage)) {
					//TODO: (Nad) Send "custom" event
				} else {

				}
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block // TODO: (Nad) Log
				e.printStackTrace();
			}

		}

	}

	protected class ReadThread extends Thread {

		private volatile boolean stop;

		@Override
		public void run() {
			while (!stop) {
				JsonNode parsed = null;
				String jsonMSG = null;
				CastMessage message = null;

				try {
					message = read();
					if (message.getPayloadType() == CastMessage.PayloadType.STRING) {
						LOGGER.debug(CHROMECAST_API_MARKER, " <-- {}", message.getPayloadUtf8());
						jsonMSG = message.getPayloadUtf8().replaceFirst("\"type\"", "\"responseType\"");
						if (jsonMSG == null || jsonMSG.isEmpty()) {
							LOGGER.warn(CHROMECAST_API_MARKER, " <-- Received empty message. Ignore.");
							continue;
						}

						// Determine whether the message belongs to cast
						// protocol or is a custom
						// message from the receiver app
						parsed = jsonMapper.readTree(jsonMSG);
					} else {
						LOGGER.warn(CHROMECAST_API_MARKER, "Received unexpected {} message", message.getPayloadType());
					}
				} catch (InvalidProtocolBufferException e) {
					warn("Error while processing protobuf", e);
				} catch (JsonProcessingException e) {
					warn("Error while processing json", e);
				} catch (IOException e) {
					if (stop) {
						LOGGER.debug(CHROMECAST_API_MARKER, "Got IOException while reading due to stream being closed (stop=true)", e);
						continue;
					}
					warn("Error while reading", e);
					String temp;
					if (message != null && message.getPayloadUtf8() != null) {
						temp = message.getPayloadUtf8();
					} else {
						temp = " null payload in message ";
					}
					LOGGER.warn(CHROMECAST_API_MARKER, " <-- {}", temp);
					try {
						close();
					} catch (IOException ex) {
						warn("Error while closing channel", ex);
					}
				} catch (Exception e) {
					warn("Unknown error while reading", e);
					continue;
				}

				try {
					if (message == null) {
						continue;
					}

					if (parsed == null || isCustomMessage(parsed)) {
						AppEvent event = new AppEvent(message.getNamespace(), message.getPayloadUtf8());
						notifyListenersAppEvent(event);
					} else {
						if (parsed.has("requestId")) {
							Long requestId = parsed.get("requestId").asLong();
							final ResultProcessor<? extends Response> rp = requests.remove(requestId);
							if (rp != null) {
								rp.put(jsonMSG);
							} else {
								notifyListenersOfSpontaneousEvent(parsed);
							}
						} else if (parsed.has("responseType") && parsed.get("responseType").asText().equals("MEDIA_STATUS")) {
							notifyListenersOfSpontaneousEvent(parsed);
						} else if (parsed.has("responseType") && parsed.get("responseType").asText().equals("PING")) {
							write("urn:x-cast:com.google.cast.tp.heartbeat", StandardMessage.pong(), DEFAULT_RECEIVER_ID);
						} else if (parsed.has("responseType") && parsed.get("responseType").asText().equals("CLOSE")) {
							notifyListenersOfSpontaneousEvent(parsed);
						}
					}
				} catch (Exception e) {
					warn("Error while handling", e);
				}
			}
		}
	}

	private class ResultProcessor<T extends Response> {

		private final Class<T> responseClass;
		private T result;

		private ResultProcessor(Class<T> responseClass) {
			if (responseClass == null) {
				throw new NullPointerException();
			}
			this.responseClass = responseClass;
		}

		public void put(String jsonMSG) throws JsonMappingException, JsonProcessingException {
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
