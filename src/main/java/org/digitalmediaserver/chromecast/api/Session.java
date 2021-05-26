package org.digitalmediaserver.chromecast.api;

import java.io.IOException;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;


//TODO: (Nad) Header + JavaDocs
@ThreadSafe
public class Session {

	@Nonnull
	protected final String senderId;

	@Nonnull
	protected final String id;

	@Nonnull
	protected final String destinationId;

	@Nonnull
	protected final Channel channel;

	@Nonnull
	protected final Object listenerLock = new Object();

	@Nullable
	@GuardedBy("listenerLock")
	protected ClosedByPeerListener listener;

	public Session(
		@Nonnull String senderId,
		@Nonnull String sessionId,
		@Nonnull String destinationId,
		@Nonnull Channel channel
	) {
		Util.requireNotBlank(senderId, "senderId");
		Util.requireNotBlank(sessionId, "sessionId");
		Util.requireNotBlank(destinationId, "destinationId");
		Util.requireNotNull(channel, "channel");
		this.senderId = senderId;
		this.id = sessionId;
		this.destinationId = destinationId;
		this.channel = channel;
	}

	@Nonnull
	public String getSenderId() {
		return senderId;
	}

	@Nonnull
	public String getId() {
		return id;
	}

	@Nonnull
	public String getDestinationId() {
		return destinationId;
	}

	public boolean isClosed() {
		return channel.isSessionClosed(this);
	}

	public boolean close() throws IOException {
		return channel.closeSession(this);
	}

	@Nullable
	public ClosedByPeerListener getClosedByPeerListener() {
		synchronized (listenerLock) {
			return listener;
		}
	}

	public void setClosedByPeerListener(@Nullable ClosedByPeerListener listener) {
		synchronized (listenerLock) {
			this.listener = listener;
		}
	}

	@Nullable
	public MediaStatus load(Media media, boolean autoplay, double currentTime, Map<String, String> customData) throws IOException {
		return channel.load(senderId, destinationId, id, media, autoplay, currentTime, customData);
	}

	@Nullable
	public MediaStatus play(long mediaSessionId) throws IOException {
		return channel.play(senderId, destinationId, id, mediaSessionId);
	}

	@Nullable
	public MediaStatus pause(long mediaSessionId) throws IOException {
		return channel.pause(senderId, destinationId, id, mediaSessionId);
	}

	@Nullable
	public MediaStatus seek(long mediaSessionId, double currentTime) throws IOException {
		return channel.seek(senderId, destinationId, id, mediaSessionId, currentTime);
	}

	@Nullable
	public MediaStatus getMediaStatus() throws IOException {
		return channel.getMediaStatus(senderId, destinationId);
	}

	public <T extends Response> T sendGenericRequest(
		String namespace,
		Request request,
		Class<T> responseClass
	) throws IOException {
		return channel.sendGenericRequest(senderId, destinationId, namespace, request, responseClass);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		if (senderId != null) {
			builder.append("senderId=").append(senderId).append(", ");
		}
		if (id != null) {
			builder.append("id=").append(id).append(", ");
		}
		if (destinationId != null) {
			builder.append("destinationId=").append(destinationId).append(", ");
		}
		builder.append("closed=").append(isClosed()).append("]");
		return builder.toString();
	}

	public interface ClosedByPeerListener {

		void closed(@Nonnull Session session);
	}
}
