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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.digitalmediaserver.chromecast.api.ChromeCastSpontaneousEvent.SpontaneousEventType;

/**
 * Helper class for delivering spontaneous events to their listeners.
 */
class EventListenerHolder implements ChromeCastSpontaneousEventListener, ChromeCastConnectionEventListener {

	private final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();
	private final Set<ChromeCastSpontaneousEventListener> eventListeners = new CopyOnWriteArraySet<>();
	private final Set<ChromeCastConnectionEventListener> eventListenersConnection = new CopyOnWriteArraySet<>();

	public EventListenerHolder() {
	}

	public void registerListener(ChromeCastSpontaneousEventListener listener) {
		if (listener != null) {
			this.eventListeners.add(listener);
		}
	}

	public void unregisterListener(ChromeCastSpontaneousEventListener listener) {
		if (listener != null) {
			this.eventListeners.remove(listener);
		}
	}

	public void deliverEvent(JsonNode json) throws JsonProcessingException {
		if (json == null || this.eventListeners.isEmpty()) {
			return;
		}

		StandardResponse resp;
		if (json.has("responseType")) {
			try {
				resp = this.jsonMapper.treeToValue(json, StandardResponse.class);
			} catch (JsonMappingException jme) {
				resp = null;
			}
		} else {
			resp = null;
		}

		/*
		 * The documentation only mentions MEDIA_STATUS as being a possible
		 * spontaneous event. Though RECEIVER_STATUS has also been observed. If
		 * others are observed, they should be added here. see:
		 * https://developers.google.com/cast/docs/reference/messages#MediaMess
		 */
		if (resp instanceof StandardResponse.MediaStatusResponse) {
			StandardResponse.MediaStatusResponse mediaStatusResponse = (StandardResponse.MediaStatusResponse) resp;
			// it may be a single media status event
			if (mediaStatusResponse.getStatuses().isEmpty()) {
				if (json.has("media")) {
					try {
						MediaStatus ms = jsonMapper.treeToValue(json, MediaStatus.class);
						spontaneousEventReceived(new ChromeCastSpontaneousEvent(SpontaneousEventType.MEDIA_STATUS, ms));
					} catch (JsonMappingException jme) {
						// ignored
					}
				}
			} else {
				for (final MediaStatus ms : mediaStatusResponse.getStatuses()) {
					spontaneousEventReceived(new ChromeCastSpontaneousEvent(SpontaneousEventType.MEDIA_STATUS, ms));
				}
			}
		} else if (resp instanceof StandardResponse.StatusResponse) {
			spontaneousEventReceived(new ChromeCastSpontaneousEvent(SpontaneousEventType.STATUS, ((StandardResponse.StatusResponse) resp).getStatus()));
		} else if (resp instanceof StandardResponse.CloseResponse) {
			spontaneousEventReceived(new ChromeCastSpontaneousEvent(SpontaneousEventType.CLOSE, new Object()));
		} else {
			spontaneousEventReceived(new ChromeCastSpontaneousEvent(SpontaneousEventType.UNKNOWN, json));
		}
	}

	public void deliverAppEvent(CustomMessageEvent event) {
		spontaneousEventReceived(new ChromeCastSpontaneousEvent(SpontaneousEventType.APPEVENT, event));
	}

	@Override
	public void spontaneousEventReceived(ChromeCastSpontaneousEvent event) {
		for (ChromeCastSpontaneousEventListener listener : this.eventListeners) {
			listener.spontaneousEventReceived(event);
		}
	}

	public void registerConnectionListener(ChromeCastConnectionEventListener listener) {
		if (listener != null) {
			this.eventListenersConnection.add(listener);
		}
	}

	public void unregisterConnectionListener(ChromeCastConnectionEventListener listener) {
		if (listener != null) {
			this.eventListenersConnection.remove(listener);
		}
	}

	public void deliverConnectionEvent(boolean connected) {
		connectionEventReceived(new ChromeCastConnectionEvent(connected));
	}

	@Override
	public void connectionEventReceived(ChromeCastConnectionEvent event) {
		for (ChromeCastConnectionEventListener listener : this.eventListenersConnection) {
			listener.connectionEventReceived(event);
		}
	}
}
