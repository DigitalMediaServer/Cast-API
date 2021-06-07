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

import java.io.IOException;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
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
import org.digitalmediaserver.chromecast.api.Volume.VolumeBuilder;

/**
 * ChromeCast device - main object used for interaction with ChromeCast dongle.
 */
public class ChromeCast {

	public static final String SERVICE_TYPE = "_googlecast._tcp.local.";

	@Nonnull
	protected final CastEventListenerList listeners = new SimpleCastEventListenerList();

	@Nonnull
	protected final String dnsName;

	@Nonnull
	protected final String address;
	protected final int port;

	@Nullable
	protected final String applicationsURL;

	@Nullable
	protected final String serviceName;

	@Nullable
	protected final String uniqueId;

	@Nonnull
	protected final Set<CastDeviceCapability> capabilities;

	@Nullable
	protected final String friendlyName;

	@Nullable
	protected final String applicationTitle;

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

	public ChromeCast(@Nonnull JmDNS mDNS, @Nonnull String dnsName, boolean autoReconnect) {
		this(mDNS.getServiceInfo(SERVICE_TYPE, dnsName), autoReconnect);
	}

	public ChromeCast(@Nonnull ServiceInfo serviceInfo, boolean autoReconnect) {
		this.dnsName = serviceInfo.getName();
		if (serviceInfo.getInet4Addresses().length > 0) {
			this.address = serviceInfo.getInet4Addresses()[0].getHostAddress();
		} else if (serviceInfo.getInet6Addresses().length > 0) {
			this.address = serviceInfo.getInet6Addresses()[0].getHostAddress();
		} else {
			throw new IllegalArgumentException("No address found for cast device");
		}
		this.autoReconnect = autoReconnect;
		this.port = serviceInfo.getPort();
		this.applicationsURL = serviceInfo.getURLs().length == 0 ? null : serviceInfo.getURLs()[0];
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
		this.applicationTitle = serviceInfo.getPropertyString("rs");
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
		this.channel = new Channel(address, port, displayName, listeners);
	}

	public ChromeCast(
		@Nonnull String dnsName,
		@Nonnull String address,
		@Nullable String applicationsURL,
		@Nullable String serviceName,
		@Nullable String uniqueId,
		@Nullable Set<CastDeviceCapability> capabilities,
		@Nullable String friendlyName,
		@Nullable String applicationTitle,
		@Nullable String modelName,
		int protocolVersion,
		@Nullable String iconPath,
		boolean autoReconnect
	) {
		this(
			dnsName,
			address,
			8009,
			applicationsURL,
			serviceName,
			uniqueId,
			capabilities,
			friendlyName,
			applicationTitle,
			modelName,
			protocolVersion,
			iconPath,
			autoReconnect
		);
	}

	public ChromeCast(
		@Nonnull String dnsName,
		@Nonnull String address,
		int port,
		@Nullable String applicationsURL,
		@Nullable String serviceName,
		@Nullable String uniqueId,
		@Nullable Set<CastDeviceCapability> capabilities,
		@Nullable String friendlyName,
		@Nullable String applicationTitle,
		@Nullable String modelName,
		int protocolVersion,
		@Nullable String iconPath,
		boolean autoReconnect
	) {
		if (Util.isBlank(dnsName)) {
			throw new IllegalArgumentException("dnsName cannot be blank");
		}
		if (Util.isBlank(address)) {
			throw new IllegalArgumentException("address cannot be blank");
		}
		this.autoReconnect = autoReconnect;
		this.dnsName = dnsName;
		this.address = address;
		this.port = port;
		this.applicationsURL = applicationsURL;
		this.serviceName = serviceName;
		this.uniqueId = uniqueId;
		this.capabilities = capabilities == null || capabilities.isEmpty() ?
			Collections.singleton(CastDeviceCapability.NONE) :
			Collections.unmodifiableSet(EnumSet.copyOf(capabilities));
		this.friendlyName = friendlyName;
		this.applicationTitle = applicationTitle;
		this.modelName = modelName;
		this.protocolVersion = protocolVersion;
		this.iconPath = iconPath;
		this.displayName = generateDisplayName();
		this.channel = new Channel(address, port, displayName, listeners);
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
	 * @return The IP address or host name of the device.
	 */
	@Nonnull
	public String getAddress() {
		return address;
	}

	/**
	 * @return The TCP port number that the device is listening to.
	 */
	public int getPort() {
		return port;
	}

	@Nullable
	public String getApplicationsURL() {
		return applicationsURL;
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
	 * @return The title of the app that is currently running, or empty string
	 *         in case of the backdrop. Usually something like "YouTube" or
	 *         "Spotify", but could also be, say, the URL of a web page being
	 *         mirrored. //TODO: (Nad) Fix JavaDoc
	 */
	@Nullable
	public String getApplicationTitle() {
		return applicationTitle;
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
	 * Set up how much time to wait until request is processed (in
	 * milliseconds).
	 *
	 * @param requestTimeout value in milliseconds until request times out
	 *            waiting for response
	 */
	public void setRequestTimeout(long requestTimeout) { //TODO: (Nad) Figure out, pointless as Channel's are cycled
		channel.setRequestTimeout(requestTimeout);
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
	 * Sets the {@link Volume} for the cast device. The {@link Volume} instance
	 * contains both the volume level and the mute status, so both can be set
	 * using this method.
	 * <p>
	 * To use this method, a {@link Volume} instance should first be acquired
	 * from the device. A {@link VolumeBuilder} can then be derived using
	 * {@link Volume#modify()} and the values modified. Finally, a new
	 * {@link Volume} instance should be created using
	 * {@link VolumeBuilder#build()}. This is the instance that should be
	 * specified to this method.
	 *
	 * @param volume the {@link Volume} instance to set.
	 * @throws IOException If an error occurs during the operation.
	 */
	@Nullable
	public void setVolume(@Nullable Volume volume) throws IOException {
		if (volume == null) {
			return;
		}
		channel.setVolume(volume);
	}

	//TODO: (Nad) JavaDocs
	public void setVolumeLevel(double level) throws IOException { //TODO: (Nad) Move above "main" setVolume
		if (level < 0.0) {
			level = 0.0;
		} else if (level > 1.0) {
			level = 1.0;
		}
		channel.setVolume(new Volume(null, Double.valueOf(level), null, null));
	}

	//TODO: (Nad) JavaDocs
	public void setMuteState(boolean muteState) throws IOException {
		channel.setVolume(new Volume(null, null, Boolean.valueOf(muteState), null));
	}

//	/**
//	 * @param level volume level from 0 to 1 to set
//	 */
//	@Nullable
//	public ReceiverStatus setVolume(float level, boolean synchronous) throws IOException {
//		return channel().setVolume(new Volume(
//			level,
//			false,
//			Volume.DEFAULT_INCREMENT, //TODO: (Nad) THis is flawed, should use info from device
//			Volume.DEFAULT_INCREMENT.doubleValue(),
//			Volume.DEFAULT_CONTROL_TYPE
//		), synchronous);
//	}
//
//	/**
//	 * ChromeCast does not allow you to jump levels too quickly to avoid blowing
//	 * speakers. Setting by increment allows us to easily get the level we want
//	 *
//	 * @param level volume level from 0 to 1 to set
//	 * @throws IOException
//	 * @see <a href=
//	 *      "https://developers.google.com/cast/docs/design_checklist/sender#sender-control-volume">sender</a>
//	 */
//	public void setVolumeByIncrement(float level) throws IOException { //TODO: (Nad) Look into this
//		Volume volume = this.getReceiverStatus().getVolume(); //TODO: (Nad) Make gradual
//		float total = volume.getLevel();
//
//		if (volume.getIncrement() <= 0f) {
//			throw new ChromeCastException("Volume.increment is <= 0");
//		}
//
//		// With floating points we always have minor decimal variations, using
//		// the Math.min/max
//		// works around this issue
//		// Increase volume
//		if (level > total) {
//			while (total < level) {
//				total = Math.min(total + volume.getIncrement(), level);
//				setVolume(total, false); //TODO: (Nad) Make proper "incremental" solution
//			}
//			// Decrease Volume
//		} else if (level < total) {
//			while (total > level) {
//				total = Math.max(total - volume.getIncrement(), level);
//				setVolume(total, false);
//			}
//		}
//	}
//
//	/**
//	 * @param muted is to mute or not
//	 */
//	public ReceiverStatus setMuted(boolean muted, boolean synchronous) throws IOException {
//		return channel().setVolume(new Volume(
//			null,
//			muted,
//			Volume.DEFAULT_INCREMENT, //TODO: (Nad) Use proper..
//			Volume.DEFAULT_INCREMENT.doubleValue(),
//			Volume.DEFAULT_CONTROL_TYPE
//		), synchronous);
//	}

	/**
	 * <p>
	 * If no application is running at the moment then exception is thrown.
	 * </p>
	 *
	 * @return current media status, state, time, playback rate, etc.
	 * @throws IOException
	 */
//	public MediaStatus getMediaStatus() throws IOException {
//		Application runningApp = getRunningApplication();
//		if (runningApp == null) {
//			throw new ChromeCastException("No application is running in ChromeCast");
//		}
//		return channel().getMediaStatus(getTransportId(runningApp));
//	}

	/**
	 * <p>
	 * Resume paused media playback
	 * </p>
	 *
	 * <p>
	 * If no application is running at the moment then exception is thrown.
	 * </p>
	 *
	 * @throws IOException
	 */
//	public void play() throws IOException {
//		ReceiverStatus status = getReceiverStatus();
//		Application runningApp = status.getRunningApp();
//		if (runningApp == null) {
//			throw new ChromeCastException("No application is running in ChromeCast");
//		}
//		MediaStatus mediaStatus = channel().getMediaStatus(getTransportId(runningApp));
//		if (mediaStatus == null) {
//			throw new ChromeCastException("ChromeCast has invalid state to resume media playback");
//		}
//		channel().play(getTransportId(runningApp), runningApp.getSessionId(), mediaStatus.getMediaSessionId());
//	}

	/**
	 * <p>
	 * Pause current playback
	 * </p>
	 *
	 * <p>
	 * If no application is running at the moment then exception is thrown.
	 * </p>
	 *
	 * @throws IOException
	 */
//	public void pause() throws IOException {
//		ReceiverStatus status = getReceiverStatus();
//		Application runningApp = status.getRunningApp();
//		if (runningApp == null) {
//			throw new ChromeCastException("No application is running in ChromeCast");
//		}
//		MediaStatus mediaStatus = channel().getMediaStatus(getTransportId(runningApp));
//		if (mediaStatus == null) {
//			throw new ChromeCastException("ChromeCast has invalid state to pause media playback");
//		}
//		channel().pause(getTransportId(runningApp), runningApp.getSessionId(), mediaStatus.getMediaSessionId());
//	}

	/**
	 * <p>
	 * Moves current playback time point to specified value
	 * </p>
	 *
	 * <p>
	 * If no application is running at the moment then exception is thrown.
	 * </p>
	 *
	 * @param time time point between zero and media duration
	 * @throws IOException
	 */
//	public void seek(double time) throws IOException {
//		ReceiverStatus status = getReceiverStatus();
//		Application runningApp = status.getRunningApp();
//		if (runningApp == null) {
//			throw new ChromeCastException("No application is running in ChromeCast");
//		}
//		MediaStatus mediaStatus = channel().getMediaStatus(getTransportId(runningApp));
//		if (mediaStatus == null) {
//			throw new ChromeCastException("ChromeCast has invalid state to seek media playback");
//		}
//		channel().seek(getTransportId(runningApp), runningApp.getSessionId(), mediaStatus.getMediaSessionId(), time);
//	}

	/**
	 * <p>
	 * Loads and starts playing media in specified URL
	 * </p>
	 *
	 * <p>
	 * If no application is running at the moment then exception is thrown.
	 * </p>
	 *
	 * @param url media url
	 * @return The new media status that resulted from loading the media.
	 * @throws IOException
	 */
//	public MediaStatus load(String url) throws IOException {
//		return load(getMediaTitle(url), null, url, getContentType(url));
//	}

	/**
	 * <p>
	 * Loads and starts playing specified media
	 * </p>
	 *
	 * <p>
	 * If no application is running at the moment then exception is thrown.
	 * </p>
	 *
	 * @param mediaTitle name to be displayed
	 * @param thumb url of video thumbnail to be displayed, relative to media
	 *            url
	 * @param url media url
	 * @param contentType MIME content type
	 * @return The new media status that resulted from loading the media.
	 * @throws IOException
	 */
//	public MediaStatus load(String mediaTitle, String thumb, String url, String contentType) throws IOException {
//		ReceiverStatus status = getReceiverStatus();
//		Application runningApp = status.getRunningApp();
//		if (runningApp == null) {
//			throw new ChromeCastException("No application is running in ChromeCast");
//		}
//		Map<String, Object> metadata = new HashMap<>(2); //TODO: (Nad) This
//		metadata.put("title", mediaTitle);
//		metadata.put("thumb", thumb);
//		return channel().load(
//			getTransportId(runningApp),
//			runningApp.getSessionId(),
//			new Media(
//				url,
//				contentType == null ? getContentType(url) : contentType,
//				null,
//				null,
//				null,
//				metadata,
//				null,
//				null
//			),
//			true,
//			0d,
//			null
//		);
//	}

	/**
	 * <p>
	 * Loads and starts playing specified media
	 * </p>
	 *
	 * <p>
	 * If no application is running at the moment then exception is thrown.
	 * </p>
	 *
	 * @param media The media to load and play. See <a href=
	 *            "https://developers.google.com/cast/docs/reference/messages#Load">
	 *            https://developers.google.com/cast/docs/reference/messages#Load</a>
	 *            for more details.
	 * @return The new media status that resulted from loading the media.
	 * @throws IOException
	 */
//	public MediaStatus load(final Media media) throws IOException {
//		ReceiverStatus status = getReceiverStatus();
//		Application runningApp = status.getRunningApp();
//		if (runningApp == null) {
//			throw new ChromeCastException("No application is running in ChromeCast");
//		}
//		Media mediaToPlay;
//		if (media.getContentType() == null) {
//			mediaToPlay = new Media(
//				media.getUrl(),
//				getContentType(media.getUrl()),
//				media.getDuration(),
//				media.getStreamType(),
//				media.getCustomData(),
//				media.getMetadata(),
//				media.getTextTrackStyle(),
//				media.getTracks()
//			);
//		} else {
//			mediaToPlay = media;
//		}
//		return channel().load(getTransportId(runningApp), runningApp.getSessionId(), mediaToPlay, true, 0d, null);
//	}

	/**
	 * <p>
	 * Sends some generic request to the currently running application.
	 * </p>
	 *
	 * <p>
	 * If no application is running at the moment then exception is thrown.
	 * </p>
	 *
	 * @param namespace request namespace
	 * @param request request object
	 * @param responseClass class of the response for proper deserialization
	 * @param <T> type of response
	 * @return deserialized response
	 * @throws IOException
	 */
//	public <T extends Response> T send(String namespace, Request request, Class<T> responseClass) throws IOException {
//		ReceiverStatus status = getReceiverStatus();
//		Application runningApp = status.getRunningApp();
//		if (runningApp == null) {
//			throw new ChromeCastException("No application is running in ChromeCast");
//		}
//		return channel().sendGenericRequest(getTransportId(runningApp), namespace, request, responseClass);
//	}

	/**
	 * <p>
	 * Sends some generic request to the currently running application. No
	 * response is expected as a result of this call.
	 * </p>
	 *
	 * <p>
	 * If no application is running at the moment then exception is thrown.
	 * </p>
	 *
	 * @param namespace request namespace
	 * @param request request object
	 * @throws IOException
	 */
//	public void send(String namespace, Request request) throws IOException {
//		send(namespace, request, null);
//	}

	public boolean addEventListener(@Nullable CastEventListener listener, CastEventType... eventTypes) {
		return listeners.add(listener, eventTypes);
	}

	public boolean removeEventListener(@Nullable CastEventListener listener) {
		return listeners.remove(listener);
	}

	@Override
	public String toString() {
		return String.format(
			"ChromeCast{name: %s, title: %s, model: %s, address: %s, port: %d}",
			this.dnsName,
			this.friendlyName,
			this.modelName,
			this.address,
			this.port
		);
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
