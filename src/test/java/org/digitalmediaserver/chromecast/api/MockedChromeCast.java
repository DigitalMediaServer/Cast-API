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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.MessageLite;
import org.digitalmediaserver.chromecast.api.CastChannel.CastMessage;
import org.digitalmediaserver.chromecast.api.CastChannel.DeviceAuthMessage;
import org.digitalmediaserver.chromecast.api.CastChannel.CastMessage.PayloadType;
import org.digitalmediaserver.chromecast.api.Volume.VolumeControlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import static org.digitalmediaserver.chromecast.api.Util.intFromB32Bytes;
import static org.digitalmediaserver.chromecast.api.Util.intToB32Bytes;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MockedChromeCast {

	private final Logger logger = LoggerFactory.getLogger(MockedChromeCast.class);

	public final ServerSocket socket;
	public final ClientThread clientThread;
	public List<Application> runningApplications = new ArrayList<>();
	public CustomHandler customHandler;

	public interface CustomHandler {
		Response handle(JsonNode json);
	}

	public MockedChromeCast() throws IOException, GeneralSecurityException {
		SSLContext sc = SSLContext.getInstance("SSL");
		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(getClass().getResourceAsStream("/keystore.jks"), "changeit".toCharArray());

		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keyStore, "changeit".toCharArray());

		sc.init(keyManagerFactory.getKeyManagers(), new TrustManager[] { new X509TrustAllManager() }, new SecureRandom());
		socket = sc.getServerSocketFactory().createServerSocket(8009);

		clientThread = new ClientThread();
		clientThread.start();
	}

	public class ClientThread extends Thread {

		public volatile boolean stop;
		public final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();

		@Override
		public void run() {
			Socket clientSocket = null;
			try {
				clientSocket = socket.accept();
				while (!stop) {
					handle(clientSocket, read(clientSocket));
				}
			} catch (IOException ioex) {
				logger.warn("Error while handling: {}", ioex.toString());
			} finally {
				if (clientSocket != null) {
					try {
						clientSocket.close();
					} catch (IOException ioex) {
						ioex.printStackTrace();
					}
				}
			}
		}

		public void handle(Socket socket, CastMessage message) throws IOException {
			logger.info("Received message: ");
			logger.info("   sourceId: " + message.getSourceId());
			logger.info("   destinationId: " + message.getDestinationId());
			logger.info("   namespace: " + message.getNamespace());
			logger.info("   payloadType: " + message.getPayloadType());
			if (message.getPayloadType() == PayloadType.STRING) {
				logger.info("   payload: " + message.getPayloadUtf8());
			}

			if (message.getPayloadType() == PayloadType.BINARY) {
				MessageLite response = handleBinary(DeviceAuthMessage.parseFrom(message.getPayloadBinary()));
				logger.info("Sending response message: ");
				logger.info("   sourceId: " + message.getDestinationId());
				logger.info("   destinationId: " + message.getSourceId());
				logger.info("   namespace: " + message.getNamespace());
				logger.info("   payloadType: " + PayloadType.BINARY);
				write(socket,
					CastMessage.newBuilder()
						.setProtocolVersion(message.getProtocolVersion())
						.setSourceId(message.getDestinationId())
						.setDestinationId(message.getSourceId())
						.setNamespace(message.getNamespace())
						.setPayloadType(PayloadType.BINARY)
						.setPayloadBinary(response.toByteString())
						.build()
					);
			} else {
				JsonNode json = jsonMapper.readTree(message.getPayloadUtf8());
				Response response = null;
				if (json.has("type")) {
					StandardMessage standardMessage = jsonMapper.readValue(message.getPayloadUtf8(), StandardMessage.class);
					response = handleJSON(standardMessage);
				} else {
					response = handleCustom(json);
				}

				if (response != null) {
					logger.info("Sending response message: ");
					logger.info("   sourceId: " + message.getDestinationId());
					logger.info("   destinationId: " + message.getSourceId());
					logger.info("   namespace: " + message.getNamespace());
					logger.info("   payloadType: " + CastMessage.PayloadType.STRING);
					logger.info("   payload: " + jsonMapper.writeValueAsString(response));
					write(socket,
						CastMessage.newBuilder()
							.setProtocolVersion(message.getProtocolVersion())
							.setSourceId(message.getDestinationId())
							.setDestinationId(message.getSourceId())
							.setNamespace(message.getNamespace())
							.setPayloadType(CastMessage.PayloadType.STRING)
							.setPayloadUtf8(jsonMapper.writeValueAsString(response))
							.build());
				}
			}
		}

		public MessageLite handleBinary(DeviceAuthMessage message) throws IOException {
			return message;
		}

		public Response handleJSON(Message message) {
			if (message instanceof StandardMessage.Ping) {
				return new StandardResponse.PongResponse();
			} else if (message instanceof StandardRequest.GetStatus) {
				return new StandardResponse.ReceiverStatusResponse(((StandardRequest.GetStatus) message).getRequestId(), status());
			} else if (message instanceof StandardRequest.Launch) {
				StandardRequest.Launch launch = (StandardRequest.Launch) message;
				String transportId = UUID.randomUUID().toString();
				runningApplications.add(new Application(
					launch.getAppId(),
					launch.getAppId(),
					"iconUrl",
					Boolean.FALSE,
					Boolean.FALSE,
					Collections.<Namespace> emptyList(),
					transportId,
					"",
					transportId,
					launch.getAppId()
				));
				StandardResponse response = new StandardResponse.ReceiverStatusResponse(launch.getRequestId(), status());
				return response;
			}
			return null;
		}

		public ReceiverStatus status() {
			return new ReceiverStatus(
				new Volume(
					VolumeControlType.ATTENUATION,
					1d,
					false,
					0.1
				),
				runningApplications,
				false,
				true
			);
		}

		public Response handleCustom(JsonNode json) {
			if (customHandler == null) {
				logger.info("No custom handler set");
				return null;
			} else {
				return customHandler.handle(json);
			}
		}

		public CastMessage read(Socket mySocket) throws IOException {
			InputStream is = mySocket.getInputStream();
			byte[] buf = new byte[4];

			int read = 0;
			while (read < buf.length) {
				int nextByte = is.read();
				if (nextByte == -1) {
					throw new CastException("Remote socket was closed");
				}
				buf[read++] = (byte) nextByte;
			}

			int size = intFromB32Bytes(buf);
			buf = new byte[size];
			read = 0;
			while (read < size) {
				int nowRead = is.read(buf, read, buf.length - read);
				if (nowRead == -1) {
					throw new CastException("Remote socket was closed");
				}
				read += nowRead;
			}

			return CastMessage.parseFrom(buf);
		}

		public void write(Socket mySocket, CastMessage message) throws IOException {
			mySocket.getOutputStream().write(intToB32Bytes(message.getSerializedSize()));
			message.writeTo(mySocket.getOutputStream());
		}
	}

	public void close() throws IOException {
		clientThread.stop = true;
		this.socket.close();
	}
}
