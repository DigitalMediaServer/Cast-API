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
package org.digitalmediaserver.cast;

import static org.digitalmediaserver.cast.Util.getContentType;
import static org.digitalmediaserver.cast.Util.getMediaTitle;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
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
import org.digitalmediaserver.cast.CastEvent.CastEventListener;
import org.digitalmediaserver.cast.CastEvent.CastEventListenerList;
import org.digitalmediaserver.cast.CastEvent.CastEventType;
import org.digitalmediaserver.cast.CastEvent.ThreadedCastEventListenerList;

/**
 * ChromeCast device - main object used for interaction with ChromeCast dongle.
 */
public class CastDevice {

	public static final String SERVICE_TYPE = "_googlecast._tcp.local.";

	/** The {@link ExecutorService} that is used for asynchronous operations */
	@Nonnull
	protected static final ExecutorService EXECUTOR = createExecutor();

	/** The currently registered {@link CastEventListener}s */
	@Nonnull
	protected final CastEventListenerList listeners;

	protected String name;
	protected final String address;
	protected final int port;
	protected String appsURL;
	protected String application;
	protected Channel channel;
	protected boolean autoReconnect = true;

	protected String title;
	protected String appTitle;
	protected String model;

	@Nonnull
	protected final String displayName;

	@Nonnull
	protected final String sourceId;

	public CastDevice(JmDNS mDNS, String name, @Nullable String sourceId) {
		this.name = name;
		ServiceInfo serviceInfo = mDNS.getServiceInfo(SERVICE_TYPE, name);
		this.address = serviceInfo.getInet4Addresses()[0].getHostAddress();
		this.port = serviceInfo.getPort();
		this.appsURL = serviceInfo.getURLs().length == 0 ? null : serviceInfo.getURLs()[0];
		this.application = serviceInfo.getApplication();

		this.title = serviceInfo.getPropertyString("fn");
		this.appTitle = serviceInfo.getPropertyString("rs");
		this.model = serviceInfo.getPropertyString("md");
		this.displayName = generateDisplayName();
		this.listeners = new ThreadedCastEventListenerList(EXECUTOR, displayName);
		this.sourceId = Util.isBlank(sourceId) ? "sender-" + new RandomString(10).nextString() : sourceId;
	}

	public CastDevice(String address, @Nullable String sourceId) {
		this(address, 8009, sourceId);
	}

	public CastDevice(String address, int port, @Nullable String sourceId) {
		this.address = address;
		this.port = port;
		this.displayName = generateDisplayName();
		this.listeners = new ThreadedCastEventListenerList(EXECUTOR, displayName);
		this.sourceId = Util.isBlank(sourceId) ? "sender-" + new RandomString(10).nextString() : sourceId;
	}

	/**
	 * @return The technical name of the device. Usually something like
	 *         Chromecast-e28835678bc02247abcdef112341278f.
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return The IP address of the device.
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * @return The TCP port number that the device is listening to.
	 */
	public int getPort() {
		return port;
	}

	public String getAppsURL() {
		return appsURL;
	}

	public void setAppsURL(String appsURL) {
		this.appsURL = appsURL;
	}

	/**
	 * @return The mDNS service name. Usually "googlecast".
	 *
	 * @see #getRunningApp()
	 */
	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	/**
	 * @return The name of the device as entered by the person who installed it.
	 *         Usually something like "Living Room Chromecast".
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return The title of the app that is currently running, or empty string
	 *         in case of the backdrop. Usually something like "YouTube" or
	 *         "Spotify", but could also be, say, the URL of a web page being
	 *         mirrored.
	 */
	public String getAppTitle() {
		return appTitle;
	}

	/**
	 * @return The model of the device. Usually "Chromecast" or, if Chromecast
	 *         is built into your TV, the model of your TV.
	 */
	public String getModel() {
		return model;
	}

	/**
	 * Returns the {@link #channel}. May open it if <code>autoReconnect</code>
	 * is set to "true" (default value) and it's not yet or no longer open.
	 *
	 * @return an open channel.
	 */
	protected synchronized Channel channel() throws IOException {
		if (autoReconnect) {
			try {
				connect();
			} catch (GeneralSecurityException e) {
				throw new IOException("Security error: " + e.getMessage(), e);
			}
		}

		return channel;
	}

	protected String getTransportId(Application runningApp) {
		return runningApp.getTransportId() == null ? runningApp.getSessionId() : runningApp.getTransportId();
	}

	public synchronized void connect() throws IOException, KeyManagementException, NoSuchAlgorithmException {
		if (channel == null || channel.isClosed()) {
			channel = new Channel(address, port, displayName, sourceId, listeners);
			channel.connect();
		}
	}

	public synchronized void disconnect() throws IOException {
		if (channel == null) {
			return;
		}

		channel.close();
		channel = null;
	}

	public boolean isConnected() {
		return channel != null && !channel.isClosed();
	}

	/**
	 * Changes behaviour for opening/closing of connection with ChromeCast
	 * device. If set to "true" (default value) then connection will be
	 * re-established on every request in case it is not present yet, or has
	 * been lost. "false" value means manual control over connection with
	 * ChromeCast device, i.e. calling <code>connect()</code> or
	 * <code>disconnect()</code> methods when needed.
	 *
	 * @param autoReconnect true means controlling connection with ChromeCast
	 *            device automatically, false - manually
	 * @see #connect()
	 * @see #disconnect()
	 */
	public void setAutoReconnect(boolean autoReconnect) {
		this.autoReconnect = autoReconnect;
	}

	/**
	 * @return current value of <code>autoReconnect</code> setting, which
	 *         controls opening/closing of connection with ChromeCast device
	 *
	 * @see #setAutoReconnect(boolean)
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
	public void setRequestTimeout(long requestTimeout) {
		channel.setRequestTimeout(requestTimeout);
	}

	/**
	 * @return current chromecast status - volume, running applications, etc.
	 * @throws IOException
	 */
	public ReceiverStatus getStatus() throws IOException {
		return channel().getReceiverStatus();
	}

	/**
	 * @return descriptor of currently running application
	 * @throws IOException
	 */
	public Application getRunningApp() throws IOException {
		ReceiverStatus status = getStatus();
		return status.getRunningApp();
	}

	/**
	 * @param appId application identifier
	 * @return true if application is available to this chromecast device, false
	 *         otherwise
	 * @throws IOException
	 */
	public boolean isAppAvailable(String appId) throws IOException {
		return channel().isApplicationAvailable(appId);
	}

	/**
	 * @param appId application identifier
	 * @return true if application with specified identifier is running now
	 * @throws IOException
	 */
	public boolean isAppRunning(String appId) throws IOException {
		ReceiverStatus status = getStatus();
		return status.getRunningApp() != null && appId.equals(status.getRunningApp().getAppId());
	}

	/**
	 * @param appId application identifier
	 * @return application descriptor if app successfully launched, null
	 *         otherwise
	 * @throws IOException
	 */
	public Application launchApp(String appId) throws IOException {
		ReceiverStatus status = channel().launch(appId);
		return status == null ? null : status.getRunningApp();
	}

	/**
	 * <p>
	 * Stops currently running application
	 * </p>
	 *
	 * <p>
	 * If no application is running at the moment then exception is thrown.
	 * </p>
	 *
	 * @throws IOException
	 */
	public void stopApp() throws IOException {
		Application runningApp = getRunningApp();
		if (runningApp == null) {
			throw new CastException("No application is running in ChromeCast");
		}
		channel().stop(runningApp.getSessionId());
	}

	/**
	 * <p>
	 * Stops the session with the given identifier.
	 * </p>
	 *
	 * @param sessionId session identifier
	 * @throws IOException
	 */
	public void stopSession(String sessionId) throws IOException {
		channel().stop(sessionId);
	}

	/**
	 * @param level volume level from 0 to 1 to set
	 */
	public void setVolume(float level) throws IOException {
		channel().setVolume(new Volume(
			level,
			false,
			Volume.DEFAULT_INCREMENT,
			Volume.DEFAULT_INCREMENT.doubleValue(),
			Volume.DEFAULT_CONTROL_TYPE
		));
	}

	/**
	 * ChromeCast does not allow you to jump levels too quickly to avoid blowing
	 * speakers. Setting by increment allows us to easily get the level we want
	 *
	 * @param level volume level from 0 to 1 to set
	 * @throws IOException
	 * @see <a href=
	 *      "https://developers.google.com/cast/docs/design_checklist/sender#sender-control-volume">sender</a>
	 */
	public void setVolumeByIncrement(float level) throws IOException {
		Volume volume = this.getStatus().getVolume();
		float total = volume.getLevel();

		if (volume.getIncrement() <= 0f) {
			throw new CastException("Volume.increment is <= 0");
		}

		// With floating points we always have minor decimal variations, using
		// the Math.min/max
		// works around this issue
		// Increase volume
		if (level > total) {
			while (total < level) {
				total = Math.min(total + volume.getIncrement(), level);
				setVolume(total);
			}
			// Decrease Volume
		} else if (level < total) {
			while (total > level) {
				total = Math.max(total - volume.getIncrement(), level);
				setVolume(total);
			}
		}
	}

	/**
	 * @param muted is to mute or not
	 */
	public void setMuted(boolean muted) throws IOException {
		channel().setVolume(new Volume(
			null,
			muted,
			Volume.DEFAULT_INCREMENT,
			Volume.DEFAULT_INCREMENT.doubleValue(),
			Volume.DEFAULT_CONTROL_TYPE
		));
	}

	/**
	 * <p>
	 * If no application is running at the moment then exception is thrown.
	 * </p>
	 *
	 * @return current media status, state, time, playback rate, etc.
	 * @throws IOException
	 */
	public MediaStatus getMediaStatus() throws IOException {
		Application runningApp = getRunningApp();
		if (runningApp == null) {
			throw new CastException("No application is running in ChromeCast");
		}
		return channel().getMediaStatus(getTransportId(runningApp));
	}

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
	public void play() throws IOException {
		ReceiverStatus status = getStatus();
		Application runningApp = status.getRunningApp();
		if (runningApp == null) {
			throw new CastException("No application is running in ChromeCast");
		}
		MediaStatus mediaStatus = channel().getMediaStatus(getTransportId(runningApp));
		if (mediaStatus == null) {
			throw new CastException("ChromeCast has invalid state to resume media playback");
		}
		channel().play(getTransportId(runningApp), runningApp.getSessionId(), mediaStatus.getMediaSessionId());
	}

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
	public void pause() throws IOException {
		ReceiverStatus status = getStatus();
		Application runningApp = status.getRunningApp();
		if (runningApp == null) {
			throw new CastException("No application is running in ChromeCast");
		}
		MediaStatus mediaStatus = channel().getMediaStatus(getTransportId(runningApp));
		if (mediaStatus == null) {
			throw new CastException("ChromeCast has invalid state to pause media playback");
		}
		channel().pause(getTransportId(runningApp), runningApp.getSessionId(), mediaStatus.getMediaSessionId());
	}

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
	public void seek(double time) throws IOException {
		ReceiverStatus status = getStatus();
		Application runningApp = status.getRunningApp();
		if (runningApp == null) {
			throw new CastException("No application is running in ChromeCast");
		}
		MediaStatus mediaStatus = channel().getMediaStatus(getTransportId(runningApp));
		if (mediaStatus == null) {
			throw new CastException("ChromeCast has invalid state to seek media playback");
		}
		channel().seek(getTransportId(runningApp), runningApp.getSessionId(), mediaStatus.getMediaSessionId(), time);
	}

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
	public MediaStatus load(String url) throws IOException {
		return load(getMediaTitle(url), null, url, getContentType(url));
	}

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
	public MediaStatus load(String mediaTitle, String thumb, String url, String contentType) throws IOException {
		ReceiverStatus status = getStatus();
		Application runningApp = status.getRunningApp();
		if (runningApp == null) {
			throw new CastException("No application is running in ChromeCast");
		}
		Map<String, Object> metadata = new HashMap<>(2);
		metadata.put("title", mediaTitle);
		metadata.put("thumb", thumb);
		return channel().load(
			getTransportId(runningApp),
			runningApp.getSessionId(),
			new Media(
				url,
				contentType == null ? getContentType(url) : contentType,
				null,
				null,
				null,
				metadata,
				null,
				null
			),
			true,
			0d,
			null
		);
	}

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
	public MediaStatus load(final Media media) throws IOException {
		ReceiverStatus status = getStatus();
		Application runningApp = status.getRunningApp();
		if (runningApp == null) {
			throw new CastException("No application is running in ChromeCast");
		}
		Media mediaToPlay;
		if (media.getContentType() == null) {
			mediaToPlay = new Media(
				media.getUrl(),
				getContentType(media.getUrl()),
				media.getDuration(),
				media.getStreamType(),
				media.getCustomData(),
				media.getMetadata(),
				media.getTextTrackStyle(),
				media.getTracks()
			);
		} else {
			mediaToPlay = media;
		}
		return channel().load(getTransportId(runningApp), runningApp.getSessionId(), mediaToPlay, true, 0d, null);
	}

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
	public <T extends Response> T send(String namespace, Request request, Class<T> responseClass) throws IOException {
		ReceiverStatus status = getStatus();
		Application runningApp = status.getRunningApp();
		if (runningApp == null) {
			throw new CastException("No application is running in ChromeCast");
		}
		return channel().sendGenericRequest(getTransportId(runningApp), namespace, request, responseClass);
	}

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
	public void send(String namespace, Request request) throws IOException {
		send(namespace, request, null);
	}

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
			this.name,
			this.title,
			this.model,
			this.address,
			this.port
		);
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
		if (!Util.isBlank(title)) {
			sb.append(title);
		} else if (!Util.isBlank(name)) {
			Pattern pattern = Pattern.compile("\\s*([^\\s-]+)-[A-Fa-f0-9]*\\s*");
			Matcher matcher = pattern.matcher(name);
			if (matcher.find()) {
				sb.append(matcher.group(1));
			}
		}
		if (sb.length() == 0) {
			sb.append("Unidentified cast device");
		}

		if (!Util.isBlank(model) && !model.equals(sb.toString())) {
			sb.append(" (").append(model).append(')');
		}
		return sb.toString();
	}
}
