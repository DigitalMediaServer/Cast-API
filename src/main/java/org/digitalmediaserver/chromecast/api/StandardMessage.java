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

import javax.annotation.Nullable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Parent class for transport objects used to communicate with ChromeCast.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
	@JsonSubTypes.Type(name = "PING", value = StandardMessage.Ping.class),
	@JsonSubTypes.Type(name = "PONG", value = StandardMessage.Pong.class),
	@JsonSubTypes.Type(name = "CONNECT", value = StandardMessage.Connect.class),
	@JsonSubTypes.Type(name = "CLOSE", value = StandardMessage.CloseConnection.class),
	@JsonSubTypes.Type(name = "GET_STATUS", value = StandardRequest.GetStatus.class),
	@JsonSubTypes.Type(name = "GET_APP_AVAILABILITY", value = StandardRequest.GetAppAvailability.class),
	@JsonSubTypes.Type(name = "LAUNCH", value = StandardRequest.Launch.class),
	@JsonSubTypes.Type(name = "STOP", value = StandardRequest.Stop.class),
	@JsonSubTypes.Type(name = "LOAD", value = StandardRequest.Load.class),
	@JsonSubTypes.Type(name = "PLAY", value = StandardRequest.Play.class),
	@JsonSubTypes.Type(name = "PAUSE", value = StandardRequest.Pause.class),
	@JsonSubTypes.Type(name = "SET_VOLUME", value = StandardRequest.SetVolume.class),
	@JsonSubTypes.Type(name = "SEEK", value = StandardRequest.Seek.class)
})

public abstract class StandardMessage implements Message {

	/**
	 * Simple "Ping" message to request a reply with "Pong" message.
	 */
	public static class Ping extends StandardMessage {
	}

	/**
	 * Simple "Pong" message to reply to "Ping" message.
	 */
	public static class Pong extends StandardMessage {
	}

	/**
	 * Some "Origin" required to be sent with the "Connect" request.
	 */
	@JsonSerialize
	public static class Origin {
	}

	/**
	 * Establishes a virtual connection with either a cast device or an
	 * application.
	 */
	public static class Connect extends StandardMessage {

		@JsonProperty
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private final String userAgent;

		@JsonProperty
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private final VirtualConnectionType connType;

		@JsonProperty
		private final Origin origin = new Origin();

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param userAgent the user-agent to use, if any.
		 * @param connectionType the {@link VirtualConnectionType} to use.
		 */
		@JsonCreator
		public Connect(
			@JsonProperty("userAgent") @Nullable String userAgent,
			@JsonProperty("connType") @Nullable VirtualConnectionType connectionType
		) {
			this.userAgent = userAgent;
			this.connType = connectionType;
		}
	}

	/**
	 * Closes a virtual connection with an application.
	 */
	public static class CloseConnection extends StandardMessage {

		@JsonProperty
		private final Integer reasonCode = Integer.valueOf(5); // Closed gracefully by sender

	}
}
