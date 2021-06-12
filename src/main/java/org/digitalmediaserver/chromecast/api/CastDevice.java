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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import org.digitalmediaserver.chromecast.api.CastEvent.CastEventListener;
import org.digitalmediaserver.chromecast.api.CastEvent.CastEventListenerList;
import org.digitalmediaserver.chromecast.api.CastEvent.CastEventType;
import org.digitalmediaserver.chromecast.api.CastEvent.SimpleCastEventListenerList;
import org.digitalmediaserver.chromecast.api.Volume.VolumeControlType;


/**
 * This class represents a Google cast device. It can be a ChromeCast, an
 * Android TV or any other device that adheres to the "cast rules"
 */
public class CastDevice {

	/** The DNS-SD service type for cast devices */
	public static final String SERVICE_TYPE = "_googlecast._tcp.local.";

	/** The currently registered {@link CastEventListener}s */
	@Nonnull
	protected final CastEventListenerList listeners;

	/** The mDNS name of the cast device */
	@Nonnull
	protected final String dnsName;

	/** The IP address and port number of the cast device */
	@Nonnull
	protected final InetSocketAddress socketAddress;

	@Nullable
	protected final String deviceURL;

	@Nullable
	protected final String serviceName;

	@Nullable
	protected final String uniqueId;

	@Nonnull
	protected final Set<CastDeviceCapability> capabilities;

	@Nullable
	protected final String friendlyName;

	@Nullable
	protected final String modelName;

	protected final int protocolVersion;

	@Nullable
	protected final String iconPath;

	@Nonnull
	protected final String displayName;

	@Nonnull
	protected final Channel channel;
	protected final boolean autoReconnect;

	public CastDevice(@Nonnull JmDNS mDNS, @Nonnull String dnsName, boolean autoReconnect) {
		this(mDNS.getServiceInfo(SERVICE_TYPE, dnsName), autoReconnect);
	}

	/**
	 * Creates a new instance by extracting the required parameters from the
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
		this.listeners = new SimpleCastEventListenerList(displayName);
		this.channel = new Channel(socketAddress, displayName, listeners);
	}

	public CastDevice( //TODO: (Nad) JavaDocs....
		@Nonnull String dnsName,
		@Nonnull String address,
		@Nullable String applicationsURL,
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
			applicationsURL,
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
	 *
	 * @param dnsName
	 * @param hostname
	 * @param port
	 * @param applicationsURL
	 * @param serviceName
	 * @param uniqueId
	 * @param capabilities
	 * @param friendlyName
	 * @param modelName
	 * @param protocolVersion
	 * @param iconPath
	 * @param autoReconnect
	 * @throws IllegalArgumentException If {@code dnsName} is blank, if
	 *             {@code port} is outside the range of valid port values or if
	 *             {@code hostname} is {@code null}.
	 */
	public CastDevice(
		@Nonnull String dnsName,
		@Nonnull String hostname,
		int port,
		@Nullable String applicationsURL,
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
		this.deviceURL = applicationsURL;
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
		this.listeners = new SimpleCastEventListenerList(displayName);
		this.channel = new Channel(socketAddress, displayName, listeners);
	}

	/**
	 *
	 * @param dnsName
	 * @param address
	 * @param port
	 * @param applicationsURL
	 * @param serviceName
	 * @param uniqueId
	 * @param capabilities
	 * @param friendlyName
	 * @param modelName
	 * @param protocolVersion
	 * @param iconPath
	 * @param autoReconnect
	 * @throws IllegalArgumentException If {@code dnsName} is blank, if
	 *             {@code port} is outside the range of valid port values or if
	 *             {@code address} is {@code null}.
	 */
	public CastDevice(
		@Nonnull String dnsName,
		@Nonnull InetAddress address,
		int port,
		@Nullable String applicationsURL,
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
		this.deviceURL = applicationsURL;
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
		this.listeners = new SimpleCastEventListenerList(displayName);
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
	 * @return The {@code DNS-SD} service name. Usually "googlecast".
	 */
	@Nullable
	public String getServiceName() {
		return serviceName;
	}

	/**
	 * @return The name of the device as entered by the person who installed it.
	 *         Usually something like "Living Room Chromecast".
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
	 * @return The model name of the device.
	 */
	@Nullable
	public String getModelName() {
		return modelName;
	}

	/**
	 * @return A {@link Set} of {@link CastDeviceCapability} announced by the
	 *         device.
	 */
	@Nonnull
	public Set<CastDeviceCapability> getCapabilities() {
		return capabilities;
	}

	@Nullable
	public String getUniqueId() {
		return uniqueId;
	}

	public int getProtocolVersion() {
		return protocolVersion;
	}

	/**
	 * @return The "path" part of the URL to the device icon.
	 */
	public String getIconPath() {
		return iconPath;
	}

	/**
	 * @return A generated name intended to be suitable to present to the user.
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns the {@link #channel}. May open it if <code>autoReconnect</code>
	 * is set to "true" (default value) and it's not yet or no longer open.
	 *
	 * @return an open channel. //TODO: (Nad) Fix JavaDoc
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

	public void connect() throws IOException, KeyManagementException, NoSuchAlgorithmException {
		channel.connect();
	}

	public void disconnect() throws IOException {
		channel.close();
	}

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
	 * @return current chromecast status - volume, running applications, etc.
	 * @throws IOException
	 */
	public ReceiverStatus getReceiverStatus() throws IOException {
		return channel().getReceiverStatus();
	}

	/**
	 * @return descriptor of currently running application
	 * @throws IOException
	 */
	public Application getRunningApplication() throws IOException {
		ReceiverStatus status = getReceiverStatus();
		return status.getRunningApplication();
	}

	/**
	 * @param appId application identifier
	 * @return true if application is available to this chromecast device, false
	 *         otherwise
	 * @throws IOException
	 */
	public boolean isApplicationAvailable(String appId) throws IOException {
		return channel().isApplicationAvailable(appId);
	}

	/**
	 * @param appId application identifier
	 * @return true if application with specified identifier is running now
	 * @throws IOException
	 */
	public boolean isApplicationRunning(String appId) throws IOException {
		ReceiverStatus status = getReceiverStatus();
		return status.getRunningApplication() != null && appId.equals(status.getRunningApplication().getAppId());
	}

	/**
	 * @param appId application identifier
	 * @return application descriptor if app successfully launched, null
	 *         otherwise
	 * @throws IOException
	 */
	@Nullable
	public ReceiverStatus launchApplication(String appId, boolean synchronous) throws IOException {
		return channel().launch(appId, synchronous);
	}

	/**
	 * Stops specified {@link Application} if it is running.
	 *
	 * @param application the {@link Application} to stop.
	 * @param synchronous if {@code true}, the method will block and wait for a
	 *            response which will be returned. If {@code false}, the method
	 *            will not block and {@code null} will always be returned.
	 * @return The resulting {@link ReceiverStatus} if {@code synchronous} is
	 *         {@code true} and one is returned from the cast device.
	 * @throws IOException If an error occurs during the operation.
	 */
	@Nullable
	public ReceiverStatus stopApplication(@Nullable Application application, boolean synchronous) throws IOException {
		if (application == null) {
			return null;
		}
		return channel().stopApplication(application, synchronous);
	}

	public Session startSession(
		@Nonnull String senderId,
		@Nonnull Application application
	) throws IOException {
		return channel().startSession(senderId, application, null, VirtualConnectionType.STRONG);
	}

	public Session startSession(
		@Nonnull String senderId,
		@Nonnull Application application,
		@Nullable String userAgent,
		@Nonnull VirtualConnectionType connectionType
	) throws IOException {
		return channel().startSession(senderId, application, userAgent, connectionType);
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
		channel.setVolume(new Volume(null, Double.valueOf(level), null, null));
	}

	/**
	 * Sets the mute state for the cast device.This method will create a
	 * one-time {@link Volume} instance, so if both mute and volume level should
	 * be changed, it's better to use {@link #setVolume(Volume)}.
	 *
	 * @param muteState {@code true} to set muted state, {@code false} to set
	 *            unmuted state.
	 * @throws IOException If the cast device has
	 *             {@link VolumeControlType#FIXED} or an error occurs during the
	 *             operation.
	 */
	public void setMuteState(boolean muteState) throws IOException {
		channel.setVolume(new Volume(null, null, Boolean.valueOf(muteState), null));
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
	 * @throws IOException If the cast device has
	 *             {@link VolumeControlType#FIXED} or an error occurs during the
	 *             operation.
	 */
	@Nullable
	public void setVolume(@Nullable Volume volume) throws IOException {
		if (volume == null) {
			return;
		}
		channel.setVolume(volume);
	}

	public boolean addEventListener(@Nullable CastEventListener listener, CastEventType... eventTypes) {
		return listeners.add(listener, eventTypes);
	}

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
		return Objects.equals(capabilities, other.capabilities) && Objects.equals(deviceURL, other.deviceURL)
			&& Objects.equals(displayName, other.displayName) && Objects.equals(dnsName, other.dnsName)
			&& Objects.equals(friendlyName, other.friendlyName) && Objects.equals(modelName, other.modelName)
			&& protocolVersion == other.protocolVersion && Objects.equals(serviceName, other.serviceName)
			&& Objects.equals(socketAddress, other.socketAddress) && Objects.equals(uniqueId, other.uniqueId);
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
