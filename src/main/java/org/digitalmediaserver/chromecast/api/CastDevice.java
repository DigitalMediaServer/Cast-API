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

import static org.digitalmediaserver.chromecast.api.Util.requireNotBlank;
import static org.digitalmediaserver.chromecast.api.Util.requireNotNull;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import org.digitalmediaserver.chromecast.api.CastEvent.CastEventListener;
import org.digitalmediaserver.chromecast.api.CastEvent.CastEventListenerList;
import org.digitalmediaserver.chromecast.api.CastEvent.CastEventType;
import org.digitalmediaserver.chromecast.api.CastEvent.ThreadedCastEventListenerList;
import org.digitalmediaserver.chromecast.api.Volume.VolumeControlType;


/**
 * This class represents a Google cast device. It can be a ChromeCast, an
 * Android TV or any other device that adheres to the "cast protocol".
 */
public class CastDevice {

	/** The DNS-SD service type for cast devices */
	public static final String SERVICE_TYPE = "_googlecast._tcp.local.";

	/** The application ID for the "Default Media Receiver" application */
	public static final String DEFAULT_MEDIA_RECEIVER_APP_ID = "CC1AD845";

	/** The {@link ExecutorService} that is used for asynchronous operations */
	@Nonnull
	protected static final ExecutorService EXECUTOR = createExecutor();

	/** The currently registered {@link CastEventListener}s */
	@Nonnull
	protected final CastEventListenerList listeners;

	/** The mDNS name of the cast device */
	@Nonnull
	protected final String dnsName;

	/** The IP address and port number of the cast device */
	@Nonnull
	protected final InetSocketAddress socketAddress;

	/** The "base URL" of the cast device */
	@Nullable
	protected final String deviceURL;

	/** The {@code DNS-SD} service name */
	@Nullable
	protected final String serviceName;

	/** The unique ID of the cast device */
	@Nullable
	protected final String uniqueId;

	/** The {@link Set} of {@link CastDeviceCapability}s for the cast device */
	@Nonnull
	protected final Set<CastDeviceCapability> capabilities;

	/** The "friendly name" of the cast device */
	@Nullable
	protected final String friendlyName;

	/** The model name of the cast device */
	@Nullable
	protected final String modelName;

	/** The protocol version supported by the cast device */
	protected final int protocolVersion;

	/** The (device URL) relative path to its icon */
	@Nullable
	protected final String iconPath;

	/** A generated name intended to be suitable to present users */
	@Nonnull
	protected final String displayName;

	/** The {@link Channel} associated with this {@link CastDevice} */
	@Nonnull
	protected final Channel channel;

	/** Whether automatic {@link Channel} reconnection "on demand" is enabled */
	protected final boolean autoReconnect;

	/**
	 * Creates a new instance by extracting the required information from the
	 * specified {@link JmDNS} instance using the specified DNS name.
	 *
	 * @param mDNS the {@link JmDNS} instance.
	 * @param dnsName the DNS name.
	 * @param autoReconnect {@code true} to try to automatically reconnect "on
	 *            demand", {@code false} to handle connection "manually" by
	 *            listening to {@code CONNECTED} events.
	 * @throws NullPointerException if {@code mDNS} is {@code null}.
	 */
	public CastDevice(@Nonnull JmDNS mDNS, @Nonnull String dnsName, boolean autoReconnect) {
		this(mDNS.getServiceInfo(SERVICE_TYPE, dnsName), autoReconnect);
	}

	/**
	 * Creates a new instance by extracting the required information from the
	 * specified {@link ServiceInfo}.
	 *
	 * @param serviceInfo the {@link ServiceInfo} instance to extract the device
	 *            information from. It must be fully resolved.
	 * @param autoReconnect {@code true} to try to automatically reconnect "on
	 *            demand", {@code false} to handle connection "manually" by
	 *            listening to {@code CONNECTED} events.
	 * @throws NullPointerException if {@code serviceInfo} is {@code null}.
	 */
	public CastDevice(@Nonnull ServiceInfo serviceInfo, boolean autoReconnect) {
		this.dnsName = serviceInfo.getName();
		InetAddress address;
		if (serviceInfo.getInet4Addresses().length > 0) {
			address = serviceInfo.getInet4Addresses()[0];
		} else if (serviceInfo.getInet6Addresses().length > 0) {
			address = serviceInfo.getInet6Addresses()[0];
		} else {
			throw new IllegalArgumentException("No address found for the cast device: " + serviceInfo);
		}
		this.socketAddress = new InetSocketAddress(address, serviceInfo.getPort());
		this.autoReconnect = autoReconnect;
		this.deviceURL = serviceInfo.getURLs().length == 0 ? null : serviceInfo.getURLs()[0];
		this.serviceName = serviceInfo.getApplication();
		this.uniqueId = serviceInfo.getPropertyString("id");
		String s = serviceInfo.getPropertyString("ca");
		if (Util.isBlank(s)) {
			this.capabilities = Collections.unmodifiableSet(CastDeviceCapability.getCastDeviceCapabilities(0));
		} else {
			int value;
			try {
				value = Integer.parseInt(s);
			} catch (NumberFormatException e) {
				value = 0;
			}
			this.capabilities = Collections.unmodifiableSet(CastDeviceCapability.getCastDeviceCapabilities(value));
		}
		this.friendlyName = serviceInfo.getPropertyString("fn");
		this.modelName = serviceInfo.getPropertyString("md");
		s = serviceInfo.getPropertyString("ve");
		if (Util.isBlank(s)) {
			this.protocolVersion = -1;
		} else {
			int value;
			try {
				value = Integer.parseInt(s);
			} catch (NumberFormatException e) {
				value = -1;
			}
			this.protocolVersion = value;
		}
		this.iconPath = serviceInfo.getPropertyString("ic");
		this.displayName = generateDisplayName();
		this.listeners = new ThreadedCastEventListenerList(EXECUTOR, displayName);
		this.channel = new Channel(socketAddress, displayName, listeners);
	}

	/**
	 * Creates a new instance using the specified parameters and
	 * {@link Channel#STANDARD_DEVICE_PORT}.
	 *
	 * @param dnsName the DNS name used by the cast device.
	 * @param hostname the hostname/address of the cast device.
	 * @param deviceURL the "base URL" of the cast device.
	 * @param serviceName the {@code DNS-SD} service name, usually "googlecast".
	 * @param uniqueId the unique ID of the cast device.
	 * @param capabilities the {@link Set} of {@link CastDeviceCapability}s that
	 *            applies for the cast device.
	 * @param friendlyName the "friendly name" of the cast device.
	 * @param modelName the model name of the cast device.
	 * @param protocolVersion the protocol version supported by the cast device.
	 * @param iconPath the (device URL) relative path to its icon.
	 * @param autoReconnect {@code true} to try to automatically reconnect "on
	 *            demand", {@code false} to handle connection "manually" by
	 *            listening to {@code CONNECTED} events.
	 * @throws IllegalArgumentException If {@code dnsName} is blank or if
	 *             {@code hostname} is {@code null}.
	 */
	public CastDevice(
		@Nonnull String dnsName,
		@Nonnull String hostname,
		@Nullable String deviceURL,
		@Nullable String serviceName,
		@Nullable String uniqueId,
		@Nullable Set<CastDeviceCapability> capabilities,
		@Nullable String friendlyName,
		@Nullable String modelName,
		int protocolVersion,
		@Nullable String iconPath,
		boolean autoReconnect
	) {
		this(
			dnsName,
			hostname,
			Channel.STANDARD_DEVICE_PORT,
			deviceURL,
			serviceName,
			uniqueId,
			capabilities,
			friendlyName,
			modelName,
			protocolVersion,
			iconPath,
			autoReconnect
		);
	}

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param dnsName the DNS name used by the cast device.
	 * @param hostname the hostname/address of the cast device.
	 * @param port the port the cast device is listening to.
	 * @param deviceURL the "base URL" of the cast device.
	 * @param serviceName the {@code DNS-SD} service name, usually "googlecast".
	 * @param uniqueId the unique ID of the casst device.
	 * @param capabilities the {@link Set} of {@link CastDeviceCapability}s that
	 *            applies for the cast device.
	 * @param friendlyName the "friendly name" of the cast device.
	 * @param modelName the model name of the cast device.
	 * @param protocolVersion the protocol version supported by the cast device.
	 * @param iconPath the (device URL) relative path to its icon.
	 * @param autoReconnect {@code true} to try to automatically reconnect "on
	 *            demand", {@code false} to handle connection "manually" by
	 *            listening to {@code CONNECTED} events.
	 * @throws IllegalArgumentException If {@code dnsName} is blank, if
	 *             {@code port} is outside the range of valid port values or if
	 *             {@code hostname} is {@code null}.
	 */
	public CastDevice(
		@Nonnull String dnsName,
		@Nonnull String hostname,
		int port,
		@Nullable String deviceURL,
		@Nullable String serviceName,
		@Nullable String uniqueId,
		@Nullable Set<CastDeviceCapability> capabilities,
		@Nullable String friendlyName,
		@Nullable String modelName,
		int protocolVersion,
		@Nullable String iconPath,
		boolean autoReconnect
	) {
		requireNotBlank(dnsName, "dnsName");
		requireNotNull(hostname, "hostname");
		this.autoReconnect = autoReconnect;
		this.dnsName = dnsName;
		this.socketAddress = new InetSocketAddress(hostname, port);
		this.deviceURL = deviceURL;
		this.serviceName = serviceName;
		this.uniqueId = uniqueId;
		this.capabilities = capabilities == null || capabilities.isEmpty() ?
			Collections.singleton(CastDeviceCapability.NONE) :
			Collections.unmodifiableSet(EnumSet.copyOf(capabilities));
		this.friendlyName = friendlyName;
		this.modelName = modelName;
		this.protocolVersion = protocolVersion;
		this.iconPath = iconPath;
		this.displayName = generateDisplayName();
		this.listeners = new ThreadedCastEventListenerList(EXECUTOR, displayName);
		this.channel = new Channel(socketAddress, displayName, listeners);
	}

	/**
	 * Creates a new instance using the specified parameters and
	 * {@link Channel#STANDARD_DEVICE_PORT}.
	 *
	 * @param dnsName the DNS name used by the cast device.
	 * @param address the IP address of the cast device.
	 * @param deviceURL the "base URL" of the cast device.
	 * @param serviceName the {@code DNS-SD} service name, usually "googlecast".
	 * @param uniqueId the unique ID of the casst device.
	 * @param capabilities the {@link Set} of {@link CastDeviceCapability}s that
	 *            applies for the cast device.
	 * @param friendlyName the "friendly name" of the cast device.
	 * @param modelName the model name of the cast device.
	 * @param protocolVersion the protocol version supported by the cast device.
	 * @param iconPath the (device URL) relative path to its icon.
	 * @param autoReconnect {@code true} to try to automatically reconnect "on
	 *            demand", {@code false} to handle connection "manually" by
	 *            listening to {@code CONNECTED} events.
	 * @throws IllegalArgumentException If {@code dnsName} is blank, if
	 *             {@code port} is outside the range of valid port values or if
	 *             {@code address} is {@code null}.
	 */
	public CastDevice(
		@Nonnull String dnsName,
		@Nonnull InetAddress address,
		@Nullable String deviceURL,
		@Nullable String serviceName,
		@Nullable String uniqueId,
		@Nullable Set<CastDeviceCapability> capabilities,
		@Nullable String friendlyName,
		@Nullable String modelName,
		int protocolVersion,
		@Nullable String iconPath,
		boolean autoReconnect
	) {
		this(
			dnsName,
			address,
			Channel.STANDARD_DEVICE_PORT,
			deviceURL,
			serviceName,
			uniqueId,
			capabilities,
			friendlyName,
			modelName,
			protocolVersion,
			iconPath,
			autoReconnect
		);
	}

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param dnsName the DNS name used by the cast device.
	 * @param address the IP address of the cast device.
	 * @param port the port the cast device is listening to.
	 * @param deviceURL the "base URL" of the cast device.
	 * @param serviceName the {@code DNS-SD} service name, usually "googlecast".
	 * @param uniqueId the unique ID of the cast device.
	 * @param capabilities the {@link Set} of {@link CastDeviceCapability}s that
	 *            applies for the cast device.
	 * @param friendlyName the "friendly name" of the cast device.
	 * @param modelName the model name of the cast device.
	 * @param protocolVersion the protocol version supported by the cast device.
	 * @param iconPath the (device URL) relative path to its icon.
	 * @param autoReconnect {@code true} to try to automatically reconnect "on
	 *            demand", {@code false} to handle connection "manually" by
	 *            listening to {@code CONNECTED} events.
	 * @throws IllegalArgumentException If {@code dnsName} is blank, if
	 *             {@code port} is outside the range of valid port values or if
	 *             {@code address} is {@code null}.
	 */
	public CastDevice(
		@Nonnull String dnsName,
		@Nonnull InetAddress address,
		int port,
		@Nullable String deviceURL,
		@Nullable String serviceName,
		@Nullable String uniqueId,
		@Nullable Set<CastDeviceCapability> capabilities,
		@Nullable String friendlyName,
		@Nullable String modelName,
		int protocolVersion,
		@Nullable String iconPath,
		boolean autoReconnect
	) {
		requireNotBlank(dnsName, "dnsName");
		requireNotNull(address, "address");
		this.autoReconnect = autoReconnect;
		this.dnsName = dnsName;
		this.socketAddress = new InetSocketAddress(address, port);
		this.deviceURL = deviceURL;
		this.serviceName = serviceName;
		this.uniqueId = uniqueId;
		this.capabilities = capabilities == null || capabilities.isEmpty() ?
			Collections.singleton(CastDeviceCapability.NONE) :
			Collections.unmodifiableSet(EnumSet.copyOf(capabilities));
		this.friendlyName = friendlyName;
		this.modelName = modelName;
		this.protocolVersion = protocolVersion;
		this.iconPath = iconPath;
		this.displayName = generateDisplayName();
		this.listeners = new ThreadedCastEventListenerList(EXECUTOR, displayName);
		this.channel = new Channel(socketAddress, displayName, listeners);
	}

	/**
	 * @return The DNS name of the device. Usually something like
	 *         {@code Chromecast-e28835678bc02247abcdef112341278f}.
	 */
	@Nonnull
	public String getDNSName() {
		return dnsName;
	}

	/**
	 * @return The IP address of the cast device. It won't normally be
	 *         {@code null}, but there's a small chance that it can, if this
	 *         {@link CastDevice} was created using a hostname that could not be
	 *         resolved to an IP address.
	 */
	@Nullable
	public InetAddress getAddress() {
		return socketAddress.getAddress();
	}

	/**
	 * @return The IP address or hostname of the cast device, depending on how
	 *         this {@link CastDevice} was created.
	 */
	public String getHostname() {
		return socketAddress.getHostString();
	}

	/**
	 * @return The port number the cast device is listening to.
	 */
	public int getPort() {
		return socketAddress.getPort();
	}


	/**
	 * @return The address and port of the cast device as a
	 *         {@link InetSocketAddress}.
	 */
	@Nonnull
	public InetSocketAddress getSocketAddress() {
		return socketAddress;
	}

	/**
	 * @return The device URL.
	 */
	@Nullable
	public String getDeviceURL() {
		return deviceURL;
	}

	/**
	 * @return The {@code DNS-SD} service name, usually "googlecast".
	 */
	@Nullable
	public String getServiceName() {
		return serviceName;
	}

	/**
	 * @return The name of the cast device as entered by the person who
	 *         installed it. Usually something like "Living Room Chromecast".
	 */
	@Nullable
	public String getFriendlyName() {
		return friendlyName;
	}

	/**
	 * Returns the model name of the device. Example values are:
	 * <ul>
	 * <li>{@code Chromecast}</li>
	 * <li>{@code Chromecast Audio}</li>
	 * <li>{@code Chromecast Ultra}</li>
	 * <li>{@code Google Home}</li>
	 * <li>{@code Google Cast Group}</li>
	 * <li>{@code SHIELD Android TV}</li>
	 * </ul>
	 *
	 * @return The model name of the cast device.
	 */
	@Nullable
	public String getModelName() {
		return modelName;
	}

	/**
	 * @return A {@link Set} of {@link CastDeviceCapability} announced by the
	 *         cast device.
	 */
	@Nonnull
	public Set<CastDeviceCapability> getCapabilities() {
		return capabilities;
	}

	/**
	 * @return The unique identifier of the cast device.
	 */
	@Nullable
	public String getUniqueId() {
		return uniqueId;
	}

	/**
	 * @return The protocol version supported by the cast device.
	 */
	public int getProtocolVersion() {
		return protocolVersion;
	}

	/**
	 * @return The "path" part of the URL to the cast device icon.
	 */
	public String getIconPath() {
		return iconPath;
	}

	/**
	 * @return A generated name intended to be suitable to present users.
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns the associated {@link Channel} if it is open or if
	 * {@code autoReconnect} is {@code true} and reconnect succeeds.
	 * <p>
	 * If the {@link Channel} isn't open and {@code autoReconnect} is
	 * {@code false} or reconnection fails, an {@link IOException} is thrown.
	 *
	 * @return The open {@link Channel}.
	 * @throws SocketException If the {@link Channel} is closed and
	 *             {@code autoReconnect} is {@code false}.
	 * @throws IOException If an error occurs during reconnect.
	 */
	@Nonnull
	protected Channel channel() throws IOException {
		if (channel.isClosed()) {
			if (!autoReconnect) {
				throw new SocketException("Channel is closed");
			}
			try {
				connect();
			} catch (GeneralSecurityException e) {
				throw new IOException("Security error: " + e.getMessage(), e);
			}
		}
		return channel;
	}

	/**
	 * Establishes a connection to the remote cast device on the associated
	 * {@link Channel}.
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
	public boolean connect() throws IOException, KeyManagementException, NoSuchAlgorithmException {
		return channel.connect();
	}

	/**
	 * Closes this the associated {@link Channel} and any {@link Session}s
	 * belonging to it. If the {@link Channel} is already closed, this is a
	 * no-op.
	 *
	 * @throws IOException If an error occurs during the operation.
	 */
	public void disconnect() throws IOException {
		channel.close();
	}

	/**
	 * @return {@code true} if the associated {@link Channel} is closed,
	 *         {@code false} if it's open.
	 */
	public boolean isConnected() {
		return !channel.isClosed();
	}

	/**
	 * Returns whether or not an attempt will be made to connect to the cast
	 * device on demand. "Demand" is defined as that a method that requires an
	 * active connection is invoked and no connected currently exist.
	 *
	 * @return {@code true} if automatic connect is active, {@code false}
	 *         otherwise if this must be handled manually.
	 */
	public boolean isAutoReconnect() {
		return autoReconnect;
	}

	/**
	 * Requests a status from the cast device and returns the resulting
	 * {@link ReceiverStatus} if one is obtained, using
	 * {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the timeout.
	 * <p>
	 * This is a blocking call that waits for the response or times out.
	 *
	 * @return The resulting {@link ReceiverStatus}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public ReceiverStatus getReceiverStatus() throws IOException {
		return channel().getReceiverStatus();
	}

	/**
	 * Requests a status from the cast device and returns the resulting
	 * {@link ReceiverStatus} if one is obtained.
	 * <p>
	 * This is a blocking call that waits for the response or times out.
	 *
	 * @param responseTimeout the response timeout in milliseconds. If zero or
	 *            negative, {@link Channel#DEFAULT_RESPONSE_TIMEOUT} will be
	 *            used.
	 * @return The resulting {@link ReceiverStatus}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public ReceiverStatus getReceiverStatus(long responseTimeout) throws IOException {
		return channel().getReceiverStatus(responseTimeout);
	}

	/**
	 * This is a convenience method that calls {@link #getReceiverStatus()} and
	 * then {@link ReceiverStatus#getRunningApplication()}.
	 * <p>
	 * This is a blocking call that waits for the response or times out.
	 *
	 * @return The {@link Application} describing the current running
	 *         application, if any, or {@code null}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public Application getRunningApplication() throws IOException {
		ReceiverStatus status = getReceiverStatus();
		return status == null ? null : status.getRunningApplication();
	}

	/**
	 * Queries the cast device if the application represented by the specified
	 * application ID is available, using
	 * {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the timeout value.
	 *
	 * @param applicationId the application ID for which to query availability.
	 * @return {@code true} if the application is available, {@code false} if
	 *         it's not.
	 * @throws IOException If the response times out or if an error occurs
	 *             during the operation.
	 */
	public boolean isApplicationAvailable(String applicationId) throws IOException {
		return channel().isApplicationAvailable(applicationId);
	}

	/**
	 * This is a convenience method that calls {@link #getReceiverStatus()} and
	 * then compares the specified application ID with the result of
	 * {@link ReceiverStatus#getRunningApplication()}.
	 * <p>
	 * This is a blocking call that waits for the response or times out.
	 *
	 * @param applicationId application ID to check if is the "currently running
	 *            application".
	 * @return {@code true} if application with specified identifier is
	 *         "currently running", {@code false} otherwise.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	public boolean isApplicationRunning(String applicationId) throws IOException {
		ReceiverStatus status = getReceiverStatus();
		Application application = status == null ? null : status.getRunningApplication();
		return application == null ? false : applicationId.equals(application.getAppId());
	}

	/**
	 * Asks the cast device to launch the application represented by the
	 * specified application ID, using {@link Channel#DEFAULT_RESPONSE_TIMEOUT}
	 * as the timeout value.
	 *
	 * @param applicationId the application ID for the application to launch.
	 * @param synchronous {@code true} to make this call block until a response
	 *            is received or times out, {@code false} to make it return
	 *            immediately always returning {@code null}.
	 * @return The resulting {@link ReceiverStatus} or {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws SocketException If the {@link Channel} is closed and
	 *             {@code autoReconnect} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public ReceiverStatus launchApplication(String applicationId, boolean synchronous) throws IOException {
		return channel().launch(applicationId, synchronous);
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
	 *            {@link Channel#DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link ReceiverStatus} or {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws SocketException If the {@link Channel} is closed and
	 *             {@code autoReconnect} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public ReceiverStatus launch(String applicationId, boolean synchronous, long responseTimeout) throws IOException {
		return channel().launch(applicationId, synchronous, responseTimeout);
	}

	/**
	 * Asks the cast device to stop the specified {@link Application}, using
	 * {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the timeout value.
	 *
	 * @param application the {@link Application} to stop.
	 * @param synchronous if {@code true}, the method will block and wait for a
	 *            response which will be returned. If {@code false}, the method
	 *            will not block and {@code null} will always be returned.
	 * @return The resulting {@link ReceiverStatus} if {@code synchronous} is
	 *         {@code true} and one is returned from the cast device.
	 * @throws SocketException If the {@link Channel} is closed and
	 *             {@code autoReconnect} is {@code false}.
	 * @throws IOException If an error occurs during the operation.
	 */
	@Nullable
	public ReceiverStatus stopApplication(@Nullable Application application, boolean synchronous) throws IOException {
		if (application == null) {
			return null;
		}
		return channel().stopApplication(application, synchronous);
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
	 *            {@link Channel#DEFAULT_RESPONSE_TIMEOUT} will be used.
	 * @return The resulting {@link ReceiverStatus} or {@code null} if
	 *         {@code synchronous} is {@code false}.
	 * @throws SocketException If the {@link Channel} is closed and
	 *             {@code autoReconnect} is {@code false}.
	 * @throws IOException If the response times out or an error occurs during
	 *             the operation.
	 */
	@Nullable
	public ReceiverStatus stopApplication(
		@Nonnull Application application,
		boolean synchronous,
		long responseTimeout
	) throws IOException {
		return channel().stopApplication(application, synchronous, responseTimeout);
	}

	/**
	 * Establishes a {@link Session} with the specified {@link Application}
	 * unless one already exists, in which case the existing {@link Session} is
	 * returned.
	 *
	 * @param sourceId the source ID to use.
	 * @param application the {@link Application} to connect to.
	 * @return The existing or new {@link Session}.
	 * @throws SocketException If the {@link Channel} is closed and
	 *             {@code autoReconnect} is {@code false}.
	 * @throws IOException If an error occurs during the operation.
	 */
	public Session startSession(
		@Nonnull String sourceId,
		@Nonnull Application application
	) throws IOException {
		return channel().startSession(sourceId, application, null, VirtualConnectionType.STRONG);
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
	 * @throws SocketException If the {@link Channel} is closed and
	 *             {@code autoReconnect} is {@code false}.
	 * @throws IOException If an error occurs during the operation.
	 */
	public Session startSession(
		@Nonnull String sourceId,
		@Nonnull Application application,
		@Nullable String userAgent,
		@Nonnull VirtualConnectionType connectionType
	) throws IOException {
		return channel().startSession(sourceId, application, userAgent, connectionType);
	}

	/**
	 * Sets the volume level for the cast device to the specified volume level
	 * (the value must be in the range 0-1). This method will create a one-time
	 * {@link Volume} instance, so if both mute and volume level should be
	 * changed, it's better to use {@link #setVolume(Volume)}.
	 * <p>
	 * If the cast device has {@link VolumeControlType#MASTER} and the volume
	 * level changes more than that of the device specified "step interval", a
	 * {@link Timer} that will adjust the volume gradually until it reaches the
	 * target level will be started.
	 *
	 * @param level the new volume level.
	 * @throws SocketException If the {@link Channel} is closed and
	 *             {@code autoReconnect} is {@code false}.
	 * @throws IOException If the cast device has
	 *             {@link VolumeControlType#FIXED} or an error occurs during the
	 *             operation.
	 */
	public void setVolumeLevel(double level) throws IOException {
		if (level < 0.0) {
			level = 0.0;
		} else if (level > 1.0) {
			level = 1.0;
		}
		channel().setVolume(new Volume(null, Double.valueOf(level), null, null));
	}

	/**
	 * Sets the mute state for the cast device.This method will create a
	 * one-time {@link Volume} instance, so if both mute and volume level should
	 * be changed, it's better to use {@link #setVolume(Volume)}.
	 *
	 * @param muteState {@code true} to set muted state, {@code false} to set
	 *            unmuted state.
	 * @throws SocketException If the {@link Channel} is closed and
	 *             {@code autoReconnect} is {@code false}.
	 * @throws IOException If the cast device has
	 *             {@link VolumeControlType#FIXED} or an error occurs during the
	 *             operation.
	 */
	public void setMuteState(boolean muteState) throws IOException {
		channel().setVolume(new Volume(null, null, Boolean.valueOf(muteState), null));
	}

	/**
	 * Sets the {@link Volume} for the cast device. The {@link Volume} instance
	 * can contain both the volume level and the mute state, so both can be set
	 * using at once.
	 * <p>
	 * The {@link Volume} instance can be created with only field(s) that should
	 * be changed set.
	 * <p>
	 * If the cast device has {@link VolumeControlType#MASTER} and the volume
	 * level changes more than that of the device specified "step interval", a
	 * {@link Timer} that will adjust the volume gradually until it reaches the
	 * target level will be started.
	 *
	 * @param volume the {@link Volume} to set.
	 * @throws SocketException If the {@link Channel} is closed and
	 *             {@code autoReconnect} is {@code false}.
	 * @throws IOException If the cast device has
	 *             {@link VolumeControlType#FIXED} or an error occurs during the
	 *             operation.
	 */
	@Nullable
	public void setVolume(@Nullable Volume volume) throws IOException {
		if (volume == null) {
			return;
		}
		channel().setVolume(volume);
	}

	/**
	 * Sends the specified {@link Request} with the specified namespace using
	 * the specified source and destination IDs and
	 * {@link Channel#DEFAULT_RESPONSE_TIMEOUT} as the timeout value. This is
	 * for requests that aren't associated with a {@link Session}.
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
		return channel().sendGenericRequest(sourceId, destinationId, namespace, request, responseClass);
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
	 *            negative, {@link Channel#DEFAULT_RESPONSE_TIMEOUT} will be used.
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
		return channel().sendGenericRequest(
			sourceId,
			destinationId,
			namespace,
			request,
			responseClass,
			responseTimeout
		);
	}

	/**
	 * Registers the specified {@link CastEventListener} for the specified
	 * {@link CastEventType}s.
	 *
	 * @param listener the {@link CastEventListener} to register.
	 * @param eventTypes the event type(s) to listen to.
	 * @return {@code true} if the listener was registered, {@code false} if it
	 *         already was registered.
	 */
	public boolean addEventListener(@Nullable CastEventListener listener, CastEventType... eventTypes) {
		return listeners.add(listener, eventTypes);
	}

	/**
	 * Unregisters the specified {@link CastEventListener}.
	 *
	 * @param listener the {@link CastEventListener} to unregister.
	 * @return {@code true} if the listener was unregistered, {@code false} it
	 *         wasn't registered to begin with.
	 */
	public boolean removeEventListener(@Nullable CastEventListener listener) {
		return listeners.remove(listener);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			capabilities, deviceURL, displayName, dnsName, friendlyName,
			modelName, protocolVersion, serviceName, socketAddress, uniqueId
		);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof CastDevice)) {
			return false;
		}
		CastDevice other = (CastDevice) obj;
		return Objects.equals(capabilities, other.capabilities) && Objects.equals(deviceURL, other.deviceURL) &&
			Objects.equals(displayName, other.displayName) && Objects.equals(dnsName, other.dnsName) &&
			Objects.equals(friendlyName, other.friendlyName) && Objects.equals(modelName, other.modelName) &&
			protocolVersion == other.protocolVersion && Objects.equals(serviceName, other.serviceName) &&
			Objects.equals(socketAddress, other.socketAddress) && Objects.equals(uniqueId, other.uniqueId);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		if (dnsName != null) {
			builder.append("dnsName=").append(dnsName).append(", ");
		}
		if (socketAddress != null) {
			builder.append("Address=").append(socketAddress.getHostString())
				.append(':').append(socketAddress.getPort()).append(", ");
		}
		if (uniqueId != null) {
			builder.append("uniqueId=").append(uniqueId).append(", ");
		}
		if (capabilities != null) {
			builder.append("capabilities=").append(capabilities).append(", ");
		}
		if (friendlyName != null) {
			builder.append("friendlyName=").append(friendlyName).append(", ");
		}
		if (modelName != null) {
			builder.append("modelName=").append(modelName).append(", ");
		}
		if (displayName != null) {
			builder.append("displayName=").append(displayName).append(", ");
		}
		builder.append("autoReconnect=").append(autoReconnect).append("]");
		return builder.toString();
	}

	/**
	 * @return The new {@link ExecutorService}.
	 */
	protected static ExecutorService createExecutor() {
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
					return new Thread(r, "Cast API worker #" + threadNumber.getAndIncrement());
				}
			}
		);
		return result;
	}

	/**
	 * Tries to generate a suitable "display name" from the available device
	 * information.
	 *
	 * @return The generated display name.
	 */
	@Nonnull
	protected String generateDisplayName() {
		StringBuilder sb = new StringBuilder();
		if (!Util.isBlank(friendlyName)) {
			sb.append(friendlyName);
		} else if (!Util.isBlank(dnsName)) {
			Pattern pattern = Pattern.compile("\\s*([^\\s-]+)-[A-Fa-f0-9]*\\s*");
			Matcher matcher = pattern.matcher(dnsName);
			if (matcher.find()) {
				sb.append(matcher.group(1));
			}
		}
		if (sb.length() == 0) {
			sb.append("Unidentified cast device");
		}

		if (!Util.isBlank(modelName) && !modelName.equals(sb.toString())) {
			sb.append(" (").append(modelName).append(')');
		}
		return sb.toString();
	}
}
