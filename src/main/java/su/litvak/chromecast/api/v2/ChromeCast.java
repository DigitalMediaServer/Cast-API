package su.litvak.chromecast.api.v2;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class ChromeCast {
    public final static String SERVICE_TYPE = "_googlecast._tcp.local.";

    private String name;
    private final String address;
    private final int port;
    private String appsURL;
    private String application;
    private Channel channel;

    public ChromeCast(JmDNS mDNS, String name) {
        this.name = name;
        ServiceInfo serviceInfo = mDNS.getServiceInfo(SERVICE_TYPE, name);
        this.address = serviceInfo.getInet4Addresses()[0].getHostAddress();
        this.port = serviceInfo.getPort();
        this.appsURL = serviceInfo.getURLs().length == 0 ? null : serviceInfo.getURLs()[0];
        this.application = serviceInfo.getApplication();
    }

    public ChromeCast(String address) {
        this(address, 8009);
    }

    public ChromeCast(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getAppsURL() {
        return appsURL;
    }

    public void setAppsURL(String appsURL) {
        this.appsURL = appsURL;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public synchronized void connect() throws IOException, GeneralSecurityException {
        if (channel == null) {
            channel = new Channel(getAddress(), getPort());
        }
    }

    public synchronized void disconnect() throws IOException {
        if (channel == null) {
            return;
        }

        channel.close();
        channel = null;
    }

    /**
     * @return  current chromecast status - volume, running applications, etc.
     * @throws IOException
     */
    public Status getStatus() throws IOException {
        return channel.getStatus();
    }

    /**
     * @return  descriptor of currently running application
     * @throws IOException
     */
    public Application getRunningApp() throws IOException {
        Status status = getStatus();
        return status.getRunningApp();
    }

    /**
     * @param appId    application identifier
     * @return  true if application is available to this chromecast device, false otherwise
     * @throws IOException
     */
    public boolean isAppAvailable(String appId) throws IOException {
        return channel.isAppAvailable(appId);
    }

    /**
     * @param appId application identifier
     * @return  true if application with specified identifier is running now
     * @throws IOException
     */
    public boolean isAppRunning(String appId) throws IOException {
        Status status = getStatus();
        return status != null && status.getRunningApp() != null && appId.equals(status.getRunningApp().id);
    }

    /**
     * @param appId    application identifier
     * @return  application descriptor if app successfully launched, null otherwise
     * @throws IOException
     */
    public Application launchApp(String appId) throws IOException {
        Status status = channel.launch(appId);
        return status == null ? null : status.getRunningApp();
    }

    /**
     * Stops currently running application
     *
     * @throws IOException
     */
    public void stopApp() throws IOException {
        channel.stop(getRunningApp().sessionId);
    }

    /**
     * @param level volume level from 0 to 1 to set
     */
    public void setVolume(float level) throws IOException {
        channel.setVolume(new Volume(level, false));
    }

    /**
     * @return  current media status, state, time, playback rate, etc.
     * @throws IOException
     */
    public MediaStatus getMediaStatus() throws IOException {
        return channel.getMediaStatus(getRunningApp().transportId);
    }

    /**
     * Resume paused media playback
     *
     * @throws IOException
     */
    public void play() throws IOException {
        Status status = getStatus();
        MediaStatus mediaStatus = channel.getMediaStatus(status.getRunningApp().transportId);
        channel.play(status.getRunningApp().transportId, status.getRunningApp().sessionId, mediaStatus.mediaSessionId);
    }

    /**
     * Pause current playback
     *
     * @throws IOException
     */
    public void pause() throws IOException {
        Status status = getStatus();
        MediaStatus mediaStatus = channel.getMediaStatus(status.getRunningApp().transportId);
        channel.pause(status.getRunningApp().transportId, status.getRunningApp().sessionId, mediaStatus.mediaSessionId);
    }

    /**
     * Moves current playback time point to specified value
     *
     * @param time    time point between zero and media duration
     * @throws IOException
     */
    public void seek(double time) throws IOException {
        Status status = getStatus();
        MediaStatus mediaStatus = channel.getMediaStatus(status.getRunningApp().transportId);
        channel.seek(status.getRunningApp().transportId, status.getRunningApp().sessionId, mediaStatus.mediaSessionId, time);
    }
}