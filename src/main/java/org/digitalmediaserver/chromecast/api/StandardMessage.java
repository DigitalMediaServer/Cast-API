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

//static const EnumTable<CastMessageType> kInstance( //TODO: (Nad) Temp
//    {
//        {CastMessageType::kPing, "PING"},
//        {CastMessageType::kPong, "PONG"},
//        {CastMessageType::kRpc, "RPC"},
//        {CastMessageType::kGetAppAvailability, "GET_APP_AVAILABILITY"},
//        {CastMessageType::kGetStatus, "GET_STATUS"},
//        {CastMessageType::kConnect, "CONNECT"},
//        {CastMessageType::kCloseConnection, "CLOSE"},
//        {CastMessageType::kBroadcast, "APPLICATION_BROADCAST"},
//        {CastMessageType::kLaunch, "LAUNCH"},
//        {CastMessageType::kStop, "STOP"},
//        {CastMessageType::kReceiverStatus, "RECEIVER_STATUS"},
//        {CastMessageType::kMediaStatus, "MEDIA_STATUS"},
//        {CastMessageType::kLaunchError, "LAUNCH_ERROR"},
//        {CastMessageType::kOffer, "OFFER"},
//        {CastMessageType::kAnswer, "ANSWER"},
//        {CastMessageType::kCapabilitiesResponse, "CAPABILITIES_RESPONSE"},
//        {CastMessageType::kStatusResponse, "STATUS_RESPONSE"},
//        {CastMessageType::kMultizoneStatus, "MULTIZONE_STATUS"},
//        {CastMessageType::kInvalidPlayerState, "INVALID_PLAYER_STATE"},
//        {CastMessageType::kLoadFailed, "LOAD_FAILED"},
//        {CastMessageType::kLoadCancelled, "LOAD_CANCELLED"},
//        {CastMessageType::kInvalidRequest, "INVALID_REQUEST"},
//        {CastMessageType::kPresentation, "PRESENTATION"},
//        {CastMessageType::kGetCapabilities, "GET_CAPABILITIES"},
//        {CastMessageType::kOther},
//    },
//    CastMessageType::kMaxValue);
//
//static const EnumTable<cast_channel::V2MessageType> kInstance(
//    {
//        {cast_channel::V2MessageType::kEditTracksInfo, "EDIT_TRACKS_INFO"},
//        {cast_channel::V2MessageType::kGetStatus, "GET_STATUS"},
//        {cast_channel::V2MessageType::kLoad, "LOAD"},
//        {cast_channel::V2MessageType::kMediaGetStatus, "MEDIA_GET_STATUS"},
//        {cast_channel::V2MessageType::kMediaSetVolume, "MEDIA_SET_VOLUME"},
//        {cast_channel::V2MessageType::kPause, "PAUSE"},
//        {cast_channel::V2MessageType::kPlay, "PLAY"},
//        {cast_channel::V2MessageType::kPrecache, "PRECACHE"},
//        {cast_channel::V2MessageType::kQueueInsert, "QUEUE_INSERT"},
//        {cast_channel::V2MessageType::kQueueLoad, "QUEUE_LOAD"},
//        {cast_channel::V2MessageType::kQueueRemove, "QUEUE_REMOVE"},
//        {cast_channel::V2MessageType::kQueueReorder, "QUEUE_REORDER"},
//        {cast_channel::V2MessageType::kQueueUpdate, "QUEUE_UPDATE"},
//        {cast_channel::V2MessageType::kQueueNext, "QUEUE_NEXT"},
//        {cast_channel::V2MessageType::kQueuePrev, "QUEUE_PREV"},
//        {cast_channel::V2MessageType::kSeek, "SEEK"},
//        {cast_channel::V2MessageType::kSetVolume, "SET_VOLUME"},
//        {cast_channel::V2MessageType::kStop, "STOP"},
//        {cast_channel::V2MessageType::kStopMedia, "STOP_MEDIA"},
//        {cast_channel::V2MessageType::kOther},
//    },
//    cast_channel::V2MessageType::kMaxValue);



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
		protected final Origin origin = new Origin();

		@JsonCreator
		public Connect(
			@JsonProperty("userAgent") @Nullable String userAgent,
			@JsonProperty("connType") @Nullable VirtualConnectionType connectionType) {
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

	public static Ping ping() {
		return new Ping();
	}

	public static Pong pong() {
		return new Pong();
	}

	public static Connect connect(@Nullable String userAgent, @Nullable VirtualConnectionType connectionType) {
		return new Connect(userAgent, connectionType);
	}

	public static CloseConnection closeConnection() {
		return new CloseConnection();
	}
}
