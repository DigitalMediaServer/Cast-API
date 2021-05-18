/*
 * Copyright (C) 2021 Digital Media Server developers.
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

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.digitalmediaserver.cast.CastChannel.CastMessage;
import org.digitalmediaserver.cast.CastChannel.CastMessage.PayloadType;
import org.digitalmediaserver.cast.CastChannel.CastMessage.ProtocolVersion;
import org.digitalmediaserver.cast.CastEvent.CastEventListener;
import org.digitalmediaserver.cast.CastEvent.CastEventListenerList;
import org.digitalmediaserver.cast.CastEvent.CastEventType;
import org.digitalmediaserver.cast.CastEvent.SimpleCastEventListenerList;
import org.digitalmediaserver.cast.Channel.InputHandler;
import org.digitalmediaserver.cast.Media.MetadataType;
import org.digitalmediaserver.cast.Media.StreamType;
import org.digitalmediaserver.cast.MediaStatus.IdleReason;
import org.digitalmediaserver.cast.MediaStatus.PlayerState;
import org.digitalmediaserver.cast.MediaStatus.RepeatMode;
import org.digitalmediaserver.cast.StandardResponse.MediaStatusResponse;
import org.digitalmediaserver.cast.StandardResponse.ReceiverStatusResponse;
import org.digitalmediaserver.cast.Volume.VolumeControlType;
import org.junit.Test;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChannelTest {

	@SuppressWarnings("unchecked")
	@Test
	public void StringMessageHandlerTest() throws Exception {
		CastEventListenerList listeners = new SimpleCastEventListenerList("Mocked device");
		final List<CastEvent<?>> events = new ArrayList<>();
		listeners.add(new CastEventListener() {

			@Override
			public void onEvent(CastEvent<?> event) {
				events.add(event);
			}
		});
		Channel channel = new Channel("localhost", "test", listeners);
		CastMessage message = CastMessage.newBuilder()
			.setProtocolVersion(ProtocolVersion.CASTV2_1_0)
			.setSourceId("receiver-0")
			.setDestinationId("sender-0")
			.setNamespace("namespace")
			.setPayloadType(PayloadType.STRING)
			.setPayloadUtf8(
				FixtureHelper.fixtureAsString("/mediaStatus-single.json").replaceFirst("\"type\"", "\"responseType\"")
			)
			.build();
		InputHandler handler = channel.new InputHandler(new ByteArrayInputStream(new byte[0]));
		handler.processStringMessage(message, message.getPayloadUtf8());
		assertEquals(1, events.size());
		CastEvent<?> event = events.get(0);
		assertEquals(CastEventType.MEDIA_STATUS, event.getEventType());
		MediaStatusResponse response = event.getData(MediaStatusResponse.class);
		assertEquals(CastEventType.MEDIA_STATUS, response.getEventType());
		assertEquals(-1L, response.getRequestId());
		assertEquals(1, response.getStatuses().size());
		MediaStatus mediaStatus = response.getStatuses().get(0);
		assertNotNull(mediaStatus);
		assertTrue(mediaStatus.getActiveTrackIds().isEmpty());
		assertNull(mediaStatus.getCurrentItemId());
		assertEquals(0.0, mediaStatus.getCurrentTime(), 0.0);
		assertTrue(mediaStatus.getCustomData().isEmpty());
		assertNull(mediaStatus.getIdleReason());
		assertTrue(mediaStatus.getItems().isEmpty());
		assertNull(mediaStatus.getLoadingItemId());
		assertNotNull(mediaStatus.getMedia());
		assertEquals(0, mediaStatus.getMediaSessionId());
		assertEquals(1f, mediaStatus.getPlaybackRate(), 0f);
		assertEquals(PlayerState.IDLE, mediaStatus.getPlayerState());
		assertNull(mediaStatus.getPreloadedItemId());
		assertNull(mediaStatus.getRepeatMode());
		assertEquals(0, mediaStatus.getSupportedMediaCommands());
		assertNotNull(mediaStatus.getVolume());
		Media media = mediaStatus.getMedia();
		assertEquals("", media.getContentType());
		assertTrue(media.getCustomData().isEmpty());
		assertEquals(null, media.getDuration());
		assertNotNull(media.getMetadata());
		assertEquals(MetadataType.GENERIC, media.getMetadataType());
		assertEquals(StreamType.NONE, media.getStreamType());
		assertNull(media.getTextTrackStyle());
		assertTrue(media.getTracks().isEmpty());
		assertEquals("", media.getUrl());
		Map<String, Object> metadata = media.getMetadata();
		assertEquals(3, metadata.size());
		assertEquals("We Take Your Calls", metadata.get("title"));
		ArrayList<Map<String, String>> images = (ArrayList<Map<String, String>>) metadata.get("images");
		assertEquals("", images.get(0).get("url"));
		MediaVolume volume = mediaStatus.getVolume();
		assertNull(volume.getLevel());
		assertNull(volume.getMuted());

		message = message.toBuilder().setPayloadUtf8(
			FixtureHelper
			.fixtureAsString("/mediaStatus-audio-with-extraStatus.json")
			.replaceFirst("\"type\"", "\"responseType\"")
		).build();
		handler.processStringMessage(message, message.getPayloadUtf8());
		assertEquals(2, events.size());
		event = events.get(1);
		assertEquals(CastEventType.MEDIA_STATUS, event.getEventType());
		response = (MediaStatusResponse) event.getData();
		assertEquals(CastEventType.MEDIA_STATUS, response.getEventType());
		assertEquals(0L, response.getRequestId());
		assertEquals(1, response.getStatuses().size());
		mediaStatus = response.getStatuses().get(0);
		assertNotNull(mediaStatus);
		assertTrue(mediaStatus.getActiveTrackIds().isEmpty());
		assertEquals(1, mediaStatus.getCurrentItemId().intValue());
		assertEquals(0.0, mediaStatus.getCurrentTime(), 0.0);
		assertTrue(mediaStatus.getCustomData().isEmpty());
		assertNull(mediaStatus.getIdleReason());
		assertTrue(mediaStatus.getItems().isEmpty());
		assertNull(mediaStatus.getLoadingItemId());
		assertNotNull(mediaStatus.getMedia());
		assertEquals(1, mediaStatus.getMediaSessionId());
		assertEquals(1f, mediaStatus.getPlaybackRate(), 0f);
		assertEquals(PlayerState.IDLE, mediaStatus.getPlayerState());
		assertNull(mediaStatus.getPreloadedItemId());
		assertEquals(RepeatMode.REPEAT_OFF, mediaStatus.getRepeatMode());
		assertEquals(15, mediaStatus.getSupportedMediaCommands());
		assertNotNull(mediaStatus.getVolume());
		media = mediaStatus.getMedia();
		assertNull(media.getContentType());
		assertTrue(media.getCustomData().isEmpty());
		assertEquals(null, media.getDuration());
		assertNotNull(media.getMetadata());
		assertEquals(MetadataType.GENERIC, media.getMetadataType());
		assertNull(media.getStreamType());
		assertNull(media.getTextTrackStyle());
		assertTrue(media.getTracks().isEmpty());
		assertEquals("http://192.168.2.139:9080/audio/99fd6998-aa4d-4764-9b41-c6869dcfc85f.mp3", media.getUrl());
		assertTrue(media.getMetadata().isEmpty());
		volume = mediaStatus.getVolume();
		assertEquals(1d, volume.getLevel().doubleValue(), 0d);
		assertFalse(volume.getMuted().booleanValue());

		message = message.toBuilder().setPayloadUtf8(
			FixtureHelper.fixtureAsString("/mediaStatus-chromecast-audio.json").replaceFirst("\"type\"", "\"responseType\"")
		).build();
		handler.processStringMessage(message, message.getPayloadUtf8());
		assertEquals(3, events.size());
		event = events.get(2);
		assertEquals(CastEventType.MEDIA_STATUS, event.getEventType());
		response = (MediaStatusResponse) event.getData();
		assertEquals(CastEventType.MEDIA_STATUS, response.getEventType());
		assertEquals(3L, response.getRequestId());
		assertEquals(1, response.getStatuses().size());
		mediaStatus = response.getStatuses().get(0);
		assertNotNull(mediaStatus);
		assertTrue(mediaStatus.getActiveTrackIds().isEmpty());
		assertEquals(1, mediaStatus.getCurrentItemId().intValue());
		assertEquals(0.0, mediaStatus.getCurrentTime(), 0.0);
		assertTrue(mediaStatus.getCustomData().isEmpty());
		assertNull(mediaStatus.getIdleReason());
		List<QueueItem> items = mediaStatus.getItems();
		assertEquals(1, items.size());
		assertTrue(items.get(0).getAutoplay().booleanValue());
		Map<String, Object> data = items.get(0).getCustomData();
		assertEquals(1, data.size());
		data = (Map<String, Object>) data.get("payload");
		assertNull(data.get("thumb"));
		assertEquals(null, data.get("title"));
		assertEquals(1, items.get(0).getItemId().intValue());
		media = items.get(0).getMedia();
		assertEquals("audio/mpeg", media.getContentType());
		assertTrue(media.getCustomData().isEmpty());
		assertEquals(389.355102, media.getDuration().doubleValue(), 0.0);
		assertTrue(media.getMetadata().isEmpty());
		assertEquals(MetadataType.GENERIC, media.getMetadataType());
		assertEquals(StreamType.BUFFERED, media.getStreamType());
		assertNull(media.getTextTrackStyle());
		assertTrue(media.getTracks().isEmpty());
		assertEquals("http://192.168.1.6:8192/audio-123-mp3", media.getUrl());
		assertNull(mediaStatus.getLoadingItemId());
		media=mediaStatus.getMedia();
		assertEquals("audio/mpeg", media.getContentType());
		assertTrue(media.getCustomData().isEmpty());
		assertEquals(389.355102, media.getDuration().doubleValue(), 0.0);
		assertTrue(media.getMetadata().isEmpty());
		assertEquals(MetadataType.GENERIC, media.getMetadataType());
		assertEquals(StreamType.BUFFERED, media.getStreamType());
		assertNull(media.getTextTrackStyle());
		assertTrue(media.getTracks().isEmpty());
		assertEquals("http://192.168.1.6:8192/audio-123-mp3", media.getUrl());
		assertEquals(1, mediaStatus.getMediaSessionId());
		assertEquals(1f, mediaStatus.getPlaybackRate(), 0f);
		assertEquals(PlayerState.BUFFERING, mediaStatus.getPlayerState());
		assertNull(mediaStatus.getPreloadedItemId());
		assertEquals(RepeatMode.REPEAT_OFF, mediaStatus.getRepeatMode());
		assertEquals(15, mediaStatus.getSupportedMediaCommands());
		assertNotNull(mediaStatus.getVolume());
		volume = mediaStatus.getVolume();
		assertEquals(1d, volume.getLevel().doubleValue(), 0d);
		assertFalse(volume.getMuted().booleanValue());

		message = message.toBuilder().setPayloadUtf8(
			FixtureHelper.fixtureAsString("/mediaStatus-no-metadataType.json").replaceFirst("\"type\"", "\"responseType\"")
		).build();
		handler.processStringMessage(message, message.getPayloadUtf8());
		assertEquals(4, events.size());
		event = events.get(3);
		assertEquals(CastEventType.MEDIA_STATUS, event.getEventType());
		response = (MediaStatusResponse) event.getData();
		assertEquals(CastEventType.MEDIA_STATUS, response.getEventType());
		assertEquals(1L, response.getRequestId());
		assertEquals(1, response.getStatuses().size());
		mediaStatus = response.getStatuses().get(0);
		assertNotNull(mediaStatus);
		assertTrue(mediaStatus.getActiveTrackIds().isEmpty());
		assertNull(mediaStatus.getCurrentItemId());
		assertEquals(16.0, mediaStatus.getCurrentTime(), 0.0);
		assertTrue(mediaStatus.getCustomData().isEmpty());
		assertNull(mediaStatus.getIdleReason());
		assertTrue(mediaStatus.getItems().isEmpty());
		assertNull(mediaStatus.getLoadingItemId());
		assertNotNull(mediaStatus.getMedia());
		assertEquals(7, mediaStatus.getMediaSessionId());
		assertEquals(1f, mediaStatus.getPlaybackRate(), 0f);
		assertEquals(PlayerState.PLAYING, mediaStatus.getPlayerState());
		assertNull(mediaStatus.getPreloadedItemId());
		assertNull(mediaStatus.getRepeatMode());
		assertEquals(29, mediaStatus.getSupportedMediaCommands());
		assertNotNull(mediaStatus.getVolume());
		media = mediaStatus.getMedia();
		assertEquals("BUFFERED", media.getContentType());
		Map<String, Object> customData = media.getCustomData();
		data = (Map<String, Object>) customData.get("status");
		assertEquals(2, data.get("state"));
		assertEquals("2308730880869724752", data.get("content_id"));
		assertEquals(16, data.get("position"));
		assertEquals(16, data.get("current_time"));
		assertEquals(16, data.get("current"));
		assertEquals(246, data.get("duration"));
		data = (Map<String, Object>) data.get("content_info");
		assertEquals("Macklemore & Ryan Lewis Radio", data.get("stationName"));
		assertEquals("2308730880869724752", data.get("stationId"));
		assertEquals("2308730880869724752", data.get("stationToken"));
		assertEquals(Boolean.FALSE, data.get("isQuickMix"));
		assertEquals(Boolean.TRUE, data.get("supportImpressionTargeting"));
		assertEquals(Boolean.FALSE, data.get("onePlaylist"));
		assertEquals("70709840", data.get("userId"));
		assertEquals("Steven Feldman", data.get("casterName"));
		assertEquals("And We Danced", data.get("songName"));
		assertEquals("Macklemore", data.get("artistName"));
		assertEquals("The Unplanned Mixtape", data.get("albumName"));
		assertEquals("http://art.jpg", data.get("artUrl"));
		assertEquals("ttt", data.get("trackToken"));
		assertEquals(0, data.get("songRating"));
		assertEquals("http://songDetail", data.get("songDetailUrl"));
		assertEquals(Boolean.TRUE, data.get("allowFeedback"));
		assertEquals("http://artist", data.get("artistExplorerUrl"));
		assertEquals("http://audio", ((Map<String, Object>) ((Map<String, Object>) data.get("audioUrlMap")).get("highQuality")).get("audioUrl"));
		data = ((Map<String, Object>) ((Map<String, Object>) customData.get("status")).get("volume"));
		assertEquals(0.6999999284744263, ((Double) data.get("level")).doubleValue(), 0.000001);
		assertEquals(Boolean.FALSE, data.get("muted"));
		assertEquals(0.05, ((Double) data.get("increment")).doubleValue(), 0.0);
		assertEquals(246d, media.getDuration().doubleValue(), 0.0);
		metadata = media.getMetadata();
		assertEquals("The Album", metadata.get("albumName"));
		assertEquals("And We Danced", metadata.get("title"));
		assertEquals("The Artist", metadata.get("albumArtist"));
		assertEquals("The Artist", metadata.get("artist"));
		assertEquals("1994-11-05T13:15:30Z", metadata.get("releaseDate"));
		images = (ArrayList<Map<String, String>>) metadata.get("images");
		assertEquals("http://lh3.googleusercontent.com/UirYk5XiPVHW2HHRtoVlvHF10_Of8VtYU9DL18qwFsFodXd3hXo60yX1BfV5up5ClCKhgZvLPUY", images.get(0).get("url"));
		assertEquals(MetadataType.GENERIC, media.getMetadataType());
		assertEquals(StreamType.BUFFERED, media.getStreamType());
		assertNull(media.getTextTrackStyle());
		assertTrue(media.getTracks().isEmpty());
		assertEquals("http://audioURL", media.getUrl());
		volume = mediaStatus.getVolume();
		assertEquals(0.69999999d, volume.getLevel().doubleValue(), 0.000001d);
		assertFalse(volume.getMuted().booleanValue());

		message = message.toBuilder().setPayloadUtf8(
			FixtureHelper.fixtureAsString("/mediaStatus-pandora.json").replaceFirst("\"type\"", "\"responseType\"")
		).build();
		handler.processStringMessage(message, message.getPayloadUtf8());
		assertEquals(5, events.size());
		event = events.get(4);
		assertEquals(CastEventType.MEDIA_STATUS, event.getEventType());
		response = (MediaStatusResponse) event.getData();
		assertEquals(CastEventType.MEDIA_STATUS, response.getEventType());
		assertEquals(1L, response.getRequestId());
		assertEquals(1, response.getStatuses().size());
		mediaStatus = response.getStatuses().get(0);
		assertNotNull(mediaStatus);
		assertTrue(mediaStatus.getActiveTrackIds().isEmpty());
		assertNull(mediaStatus.getCurrentItemId());
		assertEquals(16.0, mediaStatus.getCurrentTime(), 0.0);
		assertTrue(mediaStatus.getCustomData().isEmpty());
		assertNull(mediaStatus.getIdleReason());
		assertTrue(mediaStatus.getItems().isEmpty());
		assertNull(mediaStatus.getLoadingItemId());
		assertNotNull(mediaStatus.getMedia());
		assertEquals(7, mediaStatus.getMediaSessionId());
		assertEquals(1f, mediaStatus.getPlaybackRate(), 0f);
		assertEquals(PlayerState.PLAYING, mediaStatus.getPlayerState());
		assertNull(mediaStatus.getPreloadedItemId());
		assertNull(mediaStatus.getRepeatMode());
		assertEquals(29, mediaStatus.getSupportedMediaCommands());
		assertNotNull(mediaStatus.getVolume());
		media = mediaStatus.getMedia();
		assertEquals("BUFFERED", media.getContentType());
		customData = media.getCustomData();
		data = (Map<String, Object>) customData.get("status");
		assertEquals(2, data.get("state"));
		assertEquals("2308730880869724752", data.get("content_id"));
		assertEquals(16, data.get("position"));
		assertEquals(16, data.get("current_time"));
		assertEquals(16, data.get("current"));
		assertEquals(246, data.get("duration"));
		data = (Map<String, Object>) data.get("content_info");
		assertEquals("Macklemore & Ryan Lewis Radio", data.get("stationName"));
		assertEquals("2308730880869724752", data.get("stationId"));
		assertEquals("2308730880869724752", data.get("stationToken"));
		assertEquals(Boolean.FALSE, data.get("isQuickMix"));
		assertEquals(Boolean.TRUE, data.get("supportImpressionTargeting"));
		assertEquals(Boolean.FALSE, data.get("onePlaylist"));
		assertEquals("70709840", data.get("userId"));
		assertEquals("Steven Feldman", data.get("casterName"));
		assertEquals("And We Danced", data.get("songName"));
		assertEquals("Macklemore", data.get("artistName"));
		assertEquals("The Unplanned Mixtape", data.get("albumName"));
		assertEquals("http://art.jpg", data.get("artUrl"));
		assertEquals("ttt", data.get("trackToken"));
		assertEquals(0, data.get("songRating"));
		assertEquals("http://songDetail", data.get("songDetailUrl"));
		assertEquals(Boolean.TRUE, data.get("allowFeedback"));
		assertEquals("http://artist", data.get("artistExplorerUrl"));
		assertEquals("http://audio", ((Map<String, Object>) ((Map<String, Object>) data.get("audioUrlMap")).get("highQuality")).get("audioUrl"));
		data = ((Map<String, Object>) ((Map<String, Object>) customData.get("status")).get("volume"));
		assertEquals(0.6999999284744263, ((Double) data.get("level")).doubleValue(), 0.000001);
		assertEquals(Boolean.FALSE, data.get("muted"));
		assertEquals(0.05, ((Double) data.get("increment")).doubleValue(), 0.0);
		assertEquals(246d, media.getDuration().doubleValue(), 0.0);
		metadata = media.getMetadata();
		assertEquals("The Album", metadata.get("albumName"));
		assertEquals("And We Danced", metadata.get("title"));
		assertEquals("The Artist", metadata.get("albumArtist"));
		assertEquals("The Artist", metadata.get("artist"));
		assertEquals("1994-11-05T13:15:30Z", metadata.get("releaseDate"));
		images = (ArrayList<Map<String, String>>) metadata.get("images");
		assertEquals("http://lh3.googleusercontent.com/UirYk5XiPVHW2HHRtoVlvHF10_Of8VtYU9DL18qwFsFodXd3hXo60yX1BfV5up5ClCKhgZvLPUY", images.get(0).get("url"));
		assertEquals(MetadataType.MUSIC_TRACK, media.getMetadataType());
		assertEquals(StreamType.BUFFERED, media.getStreamType());
		assertNull(media.getTextTrackStyle());
		assertTrue(media.getTracks().isEmpty());
		assertEquals("http://audioURL", media.getUrl());
		volume = mediaStatus.getVolume();
		assertEquals(0.69999999d, volume.getLevel().doubleValue(), 0.000001d);
		assertFalse(volume.getMuted().booleanValue());

		message = message.toBuilder().setPayloadUtf8(
			FixtureHelper.fixtureAsString("/mediaStatus-unknown-metadataType.json").replaceFirst("\"type\"", "\"responseType\"")
		).build();
		handler.processStringMessage(message, message.getPayloadUtf8());
		assertEquals(6, events.size());
		event = events.get(5);
		assertEquals(CastEventType.MEDIA_STATUS, event.getEventType());
		response = (MediaStatusResponse) event.getData();
		assertEquals(CastEventType.MEDIA_STATUS, response.getEventType());
		assertEquals(1L, response.getRequestId());
		assertEquals(1, response.getStatuses().size());
		mediaStatus = response.getStatuses().get(0);
		assertNotNull(mediaStatus);
		assertTrue(mediaStatus.getActiveTrackIds().isEmpty());
		assertNull(mediaStatus.getCurrentItemId());
		assertEquals(16.0, mediaStatus.getCurrentTime(), 0.0);
		assertTrue(mediaStatus.getCustomData().isEmpty());
		assertNull(mediaStatus.getIdleReason());
		assertTrue(mediaStatus.getItems().isEmpty());
		assertNull(mediaStatus.getLoadingItemId());
		assertNotNull(mediaStatus.getMedia());
		assertEquals(7, mediaStatus.getMediaSessionId());
		assertEquals(1f, mediaStatus.getPlaybackRate(), 0f);
		assertEquals(PlayerState.PLAYING, mediaStatus.getPlayerState());
		assertNull(mediaStatus.getPreloadedItemId());
		assertNull(mediaStatus.getRepeatMode());
		assertEquals(29, mediaStatus.getSupportedMediaCommands());
		assertNotNull(mediaStatus.getVolume());
		media = mediaStatus.getMedia();
		assertEquals("BUFFERED", media.getContentType());
		customData = media.getCustomData();
		data = (Map<String, Object>) customData.get("status");
		assertEquals(2, data.get("state"));
		assertEquals("2308730880869724752", data.get("content_id"));
		assertEquals(16, data.get("position"));
		assertEquals(16, data.get("current_time"));
		assertEquals(16, data.get("current"));
		assertEquals(246, data.get("duration"));
		data = (Map<String, Object>) data.get("content_info");
		assertEquals("Macklemore & Ryan Lewis Radio", data.get("stationName"));
		assertEquals("2308730880869724752", data.get("stationId"));
		assertEquals("2308730880869724752", data.get("stationToken"));
		assertEquals(Boolean.FALSE, data.get("isQuickMix"));
		assertEquals(Boolean.TRUE, data.get("supportImpressionTargeting"));
		assertEquals(Boolean.FALSE, data.get("onePlaylist"));
		assertEquals("70709840", data.get("userId"));
		assertEquals("Steven Feldman", data.get("casterName"));
		assertEquals("And We Danced", data.get("songName"));
		assertEquals("Macklemore", data.get("artistName"));
		assertEquals("The Unplanned Mixtape", data.get("albumName"));
		assertEquals("http://art.jpg", data.get("artUrl"));
		assertEquals("ttt", data.get("trackToken"));
		assertEquals(0, data.get("songRating"));
		assertEquals("http://songDetail", data.get("songDetailUrl"));
		assertEquals(Boolean.TRUE, data.get("allowFeedback"));
		assertEquals("http://artist", data.get("artistExplorerUrl"));
		assertEquals("http://audio", ((Map<String, Object>) ((Map<String, Object>) data.get("audioUrlMap")).get("highQuality")).get("audioUrl"));
		data = ((Map<String, Object>) ((Map<String, Object>) customData.get("status")).get("volume"));
		assertEquals(0.6999999284744263, ((Double) data.get("level")).doubleValue(), 0.000001);
		assertEquals(Boolean.FALSE, data.get("muted"));
		assertEquals(0.05, ((Double) data.get("increment")).doubleValue(), 0.0);
		assertEquals(246d, media.getDuration().doubleValue(), 0.0);
		metadata = media.getMetadata();
		assertEquals("The Album", metadata.get("albumName"));
		assertEquals("And We Danced", metadata.get("title"));
		assertEquals("The Artist", metadata.get("albumArtist"));
		assertEquals("The Artist", metadata.get("artist"));
		assertEquals("1994-11-05T13:15:30Z", metadata.get("releaseDate"));
		images = (ArrayList<Map<String, String>>) metadata.get("images");
		assertEquals("http://lh3.googleusercontent.com/UirYk5XiPVHW2HHRtoVlvHF10_Of8VtYU9DL18qwFsFodXd3hXo60yX1BfV5up5ClCKhgZvLPUY", images.get(0).get("url"));
		assertEquals(MetadataType.GENERIC, media.getMetadataType());
		assertEquals(StreamType.BUFFERED, media.getStreamType());
		assertNull(media.getTextTrackStyle());
		assertTrue(media.getTracks().isEmpty());
		assertEquals("http://audioURL", media.getUrl());
		volume = mediaStatus.getVolume();
		assertEquals(0.69999999d, volume.getLevel().doubleValue(), 0.000001d);
		assertFalse(volume.getMuted().booleanValue());

		message = message.toBuilder().setPayloadUtf8(
			FixtureHelper.fixtureAsString("/mediaStatus-with-idleReason.json").replaceFirst("\"type\"", "\"responseType\"")
		).build();
		handler.processStringMessage(message, message.getPayloadUtf8());
		assertEquals(7, events.size());
		event = events.get(6);
		assertEquals(CastEventType.MEDIA_STATUS, event.getEventType());
		response = (MediaStatusResponse) event.getData();
		assertEquals(CastEventType.MEDIA_STATUS, response.getEventType());
		assertEquals(28L, response.getRequestId());
		assertEquals(1, response.getStatuses().size());
		mediaStatus = response.getStatuses().get(0);
		assertNotNull(mediaStatus);
		assertTrue(mediaStatus.getActiveTrackIds().isEmpty());
		assertNull(mediaStatus.getCurrentItemId());
		assertEquals(0.0, mediaStatus.getCurrentTime(), 0.0);
		assertTrue(mediaStatus.getCustomData().isEmpty());
		assertEquals(IdleReason.ERROR, mediaStatus.getIdleReason());
		assertTrue(mediaStatus.getItems().isEmpty());
		assertNull(mediaStatus.getLoadingItemId());
		assertNotNull(mediaStatus.getMedia());
		assertEquals(1, mediaStatus.getMediaSessionId());
		assertEquals(1f, mediaStatus.getPlaybackRate(), 0f);
		assertEquals(PlayerState.IDLE, mediaStatus.getPlayerState());
		assertNull(mediaStatus.getPreloadedItemId());
		assertNull(mediaStatus.getRepeatMode());
		assertEquals(15, mediaStatus.getSupportedMediaCommands());
		assertNotNull(mediaStatus.getVolume());
		media = mediaStatus.getMedia();
		assertEquals("video/transcode", media.getContentType());
		assertTrue(media.getCustomData().isEmpty());
		assertNull(media.getDuration());
		assertTrue(media.getMetadata().isEmpty());
		assertEquals(MetadataType.GENERIC, media.getMetadataType());
		assertEquals(StreamType.BUFFERED, media.getStreamType());
		assertNull(media.getTextTrackStyle());
		assertTrue(media.getTracks().isEmpty());
		assertEquals("/public/Videos/Movies/FileB.mp4", media.getUrl());
		volume = mediaStatus.getVolume();
		assertEquals(1d, volume.getLevel().doubleValue(), 0.000001d);
		assertFalse(volume.getMuted().booleanValue());

		message = message.toBuilder().setPayloadUtf8(
			FixtureHelper.fixtureAsString("/mediaStatus-without-idleReason.json").replaceFirst("\"type\"", "\"responseType\"")
		).build();
		handler.processStringMessage(message, message.getPayloadUtf8());
		assertEquals(8, events.size());
		event = events.get(7);
		assertEquals(CastEventType.MEDIA_STATUS, event.getEventType());
		response = (MediaStatusResponse) event.getData();
		assertEquals(CastEventType.MEDIA_STATUS, response.getEventType());
		assertEquals(28L, response.getRequestId());
		assertEquals(1, response.getStatuses().size());
		mediaStatus = response.getStatuses().get(0);
		assertNotNull(mediaStatus);
		assertTrue(mediaStatus.getActiveTrackIds().isEmpty());
		assertNull(mediaStatus.getCurrentItemId());
		assertEquals(0.0, mediaStatus.getCurrentTime(), 0.0);
		assertTrue(mediaStatus.getCustomData().isEmpty());
		assertNull(mediaStatus.getIdleReason());
		assertTrue(mediaStatus.getItems().isEmpty());
		assertNull(mediaStatus.getLoadingItemId());
		assertNotNull(mediaStatus.getMedia());
		assertEquals(1, mediaStatus.getMediaSessionId());
		assertEquals(1f, mediaStatus.getPlaybackRate(), 0f);
		assertEquals(PlayerState.IDLE, mediaStatus.getPlayerState());
		assertNull(mediaStatus.getPreloadedItemId());
		assertNull(mediaStatus.getRepeatMode());
		assertEquals(15, mediaStatus.getSupportedMediaCommands());
		assertNotNull(mediaStatus.getVolume());
		media = mediaStatus.getMedia();
		assertEquals("video/transcode", media.getContentType());
		assertTrue(media.getCustomData().isEmpty());
		assertNull(media.getDuration());
		assertTrue(media.getMetadata().isEmpty());
		assertEquals(MetadataType.GENERIC, media.getMetadataType());
		assertEquals(StreamType.BUFFERED, media.getStreamType());
		assertNull(media.getTextTrackStyle());
		assertTrue(media.getTracks().isEmpty());
		assertEquals("/public/Videos/Movies/FileB.mp4", media.getUrl());
		volume = mediaStatus.getVolume();
		assertEquals(1d, volume.getLevel().doubleValue(), 0.000001d);
		assertFalse(volume.getMuted().booleanValue());

		message = message.toBuilder().setPayloadUtf8(
			FixtureHelper.fixtureAsString("/mediaStatus-with-videoinfo.json").replaceFirst("\"type\"", "\"responseType\"")
		).build();
		handler.processStringMessage(message, message.getPayloadUtf8());
		assertEquals(9, events.size());
		event = events.get(8);
		assertEquals(CastEventType.MEDIA_STATUS, event.getEventType());
		response = (MediaStatusResponse) event.getData();
		assertEquals(CastEventType.MEDIA_STATUS, response.getEventType());
		assertEquals(7L, response.getRequestId());
		assertEquals(1, response.getStatuses().size());
		mediaStatus = response.getStatuses().get(0);
		assertNotNull(mediaStatus);
		assertTrue(mediaStatus.getActiveTrackIds().isEmpty());
		assertEquals(1, mediaStatus.getCurrentItemId().intValue());
		assertEquals(0.0, mediaStatus.getCurrentTime(), 0.0);
		assertTrue(mediaStatus.getCustomData().isEmpty());
		assertNull(mediaStatus.getIdleReason());
		items = mediaStatus.getItems();
		assertEquals(1, items.get(0).getItemId().intValue());
		assertTrue(items.get(0).getAutoplay().booleanValue());
		media = items.get(0).getMedia();
		assertEquals("video/mp4", media.getContentType());
		assertTrue(media.getCustomData().isEmpty());
		assertEquals(596.474195, media.getDuration().doubleValue(), 0.0);
		assertTrue(media.getMetadata().isEmpty());
		assertEquals(MetadataType.GENERIC, media.getMetadataType());
		assertNull(media.getStreamType());
		assertNull(media.getTextTrackStyle());
		assertTrue(media.getTracks().isEmpty());
		assertEquals("http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", media.getUrl());
		customData = items.get(0).getCustomData();
		data = (Map<String, Object>) customData.get("payload");
		assertEquals("Big Buck Bunny", data.get("title:"));
		assertEquals("images/BigBuckBunny.jpg", data.get("thumb"));
		assertNull(mediaStatus.getLoadingItemId());
		assertNotNull(mediaStatus.getMedia());
		assertEquals(1, mediaStatus.getMediaSessionId());
		assertEquals(1f, mediaStatus.getPlaybackRate(), 0f);
		assertEquals(PlayerState.BUFFERING, mediaStatus.getPlayerState());
		assertNull(mediaStatus.getPreloadedItemId());
		assertEquals(RepeatMode.REPEAT_OFF, mediaStatus.getRepeatMode());
		assertEquals(15, mediaStatus.getSupportedMediaCommands());
		assertNotNull(mediaStatus.getVolume());
		volume = mediaStatus.getVolume();
		assertEquals(1d, volume.getLevel().doubleValue(), 0.000001d);
		assertFalse(volume.getMuted().booleanValue());

		message = message.toBuilder().setPayloadUtf8(
			FixtureHelper.fixtureAsString("/mediaStatuses.json").replaceFirst("\"type\"", "\"responseType\"")
		).build();
		handler.processStringMessage(message, message.getPayloadUtf8());
		assertEquals(10, events.size());
		event = events.get(9);
		assertEquals(CastEventType.MEDIA_STATUS, event.getEventType());
		response = (MediaStatusResponse) event.getData();
		assertEquals(CastEventType.MEDIA_STATUS, response.getEventType());
		assertEquals(2395L, response.getRequestId());
		assertEquals(2, response.getStatuses().size());
		mediaStatus = response.getStatuses().get(0);
		assertNotNull(mediaStatus);
		assertTrue(mediaStatus.getActiveTrackIds().isEmpty());
		assertEquals(1, mediaStatus.getCurrentItemId().intValue());
		assertEquals(0.0, mediaStatus.getCurrentTime(), 0.0);
		assertTrue(mediaStatus.getCustomData().isEmpty());
		assertNull(mediaStatus.getIdleReason());
		assertTrue(mediaStatus.getItems().isEmpty());
		assertNull(mediaStatus.getLoadingItemId());
		assertNotNull(mediaStatus.getMedia());
		assertEquals(1, mediaStatus.getMediaSessionId());
		assertEquals(1f, mediaStatus.getPlaybackRate(), 0f);
		assertEquals(PlayerState.IDLE, mediaStatus.getPlayerState());
		assertNull(mediaStatus.getPreloadedItemId());
		assertEquals(RepeatMode.REPEAT_OFF, mediaStatus.getRepeatMode());
		assertEquals(15, mediaStatus.getSupportedMediaCommands());
		assertNotNull(mediaStatus.getVolume());
		media = mediaStatus.getMedia();
		assertNull(media.getContentType());
		assertTrue(media.getCustomData().isEmpty());
		assertEquals(null, media.getDuration());
		assertNotNull(media.getMetadata());
		assertEquals(MetadataType.GENERIC, media.getMetadataType());
		assertNull(media.getStreamType());
		assertNull(media.getTextTrackStyle());
		assertTrue(media.getTracks().isEmpty());
		assertEquals("http://192.168.2.139:9080/audio/99fd6998-aa4d-4764-9b41-c6869dcfc85f.mp3", media.getUrl());
		assertTrue(media.getMetadata().isEmpty());
		volume = mediaStatus.getVolume();
		assertEquals(1d, volume.getLevel().doubleValue(), 0d);
		assertFalse(volume.getMuted().booleanValue());
		mediaStatus = response.getStatuses().get(1);
		assertNotNull(mediaStatus);
		assertTrue(mediaStatus.getActiveTrackIds().isEmpty());
		assertNull(mediaStatus.getCurrentItemId());
		assertEquals(16.0, mediaStatus.getCurrentTime(), 0.0);
		assertTrue(mediaStatus.getCustomData().isEmpty());
		assertNull(mediaStatus.getIdleReason());
		assertTrue(mediaStatus.getItems().isEmpty());
		assertNull(mediaStatus.getLoadingItemId());
		assertNotNull(mediaStatus.getMedia());
		assertEquals(7, mediaStatus.getMediaSessionId());
		assertEquals(1f, mediaStatus.getPlaybackRate(), 0f);
		assertEquals(PlayerState.PLAYING, mediaStatus.getPlayerState());
		assertNull(mediaStatus.getPreloadedItemId());
		assertNull(mediaStatus.getRepeatMode());
		assertEquals(29, mediaStatus.getSupportedMediaCommands());
		assertNotNull(mediaStatus.getVolume());
		media = mediaStatus.getMedia();
		assertEquals("BUFFERED", media.getContentType());
		assertTrue(media.getCustomData().isEmpty());
		assertEquals(246d, media.getDuration().doubleValue(), 0.0);
		metadata = media.getMetadata();
		assertEquals("The Album", metadata.get("albumName"));
		assertEquals("And We Danced", metadata.get("title"));
		assertEquals("The Artist", metadata.get("albumArtist"));
		assertEquals("The Artist", metadata.get("artist"));
		assertEquals("1994-11-05T13:15:30Z", metadata.get("releaseDate"));
		images = (ArrayList<Map<String, String>>) metadata.get("images");
		assertEquals("http://lh3.googleusercontent.com/UirYk5XiPVHW2HHRtoVlvHF10_Of8VtYU9DL18qwFsFodXd3hXo60yX1BfV5up5ClCKhgZvLPUY", images.get(0).get("url"));
		assertEquals(MetadataType.MUSIC_TRACK, media.getMetadataType());
		assertEquals(StreamType.BUFFERED, media.getStreamType());
		assertNull(media.getTextTrackStyle());
		assertTrue(media.getTracks().isEmpty());
		assertEquals("http://audioURL", media.getUrl());
		volume = mediaStatus.getVolume();
		assertEquals(0.69999999d, volume.getLevel().doubleValue(), 0.000001d);
		assertFalse(volume.getMuted().booleanValue());

		message = message.toBuilder().setPayloadUtf8(
			FixtureHelper.fixtureAsString("/timetick.json").replaceFirst("\"type\"", "\"responseType\"")
		).build();
		handler.processStringMessage(message, message.getPayloadUtf8());
		assertEquals(11, events.size());
		event = events.get(10);
		assertEquals(CastEventType.CUSTOM_MESSAGE, event.getEventType());
		CustomMessageEvent custom = event.getData(CustomMessageEvent.class);
		assertEquals("namespace", custom.getNamespace());
		assertEquals(PayloadType.STRING, custom.getPayloadType());
		assertNull(custom.getBinaryPayload());

		CustomMessage customMessage = new CustomMessage();
		customMessage.requestId = 9023L;
		customMessage.responseType = "CUST_STATUS";
		customMessage.content = "Test message";
		ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();
		message = message.toBuilder()
			.setNamespace("urn:x-cast:com.example.app")
			.setPayloadUtf8(jsonMapper.writeValueAsString(customMessage))
			.build();
		handler.processStringMessage(message, message.getPayloadUtf8());
		assertEquals(12, events.size());
		event = events.get(11);
		assertEquals(CastEventType.CUSTOM_MESSAGE, event.getEventType());
		custom = event.getData(CustomMessageEvent.class);
		assertEquals("urn:x-cast:com.example.app", custom.getNamespace());
		assertEquals(PayloadType.STRING, custom.getPayloadType());
		assertEquals("{\"responseType\":\"CUST_STATUS\",\"requestId\":9023,\"content\":\"Test message\"}", custom.getStringPayload());
		assertNull(custom.getBinaryPayload());

		Volume deviceVolume = new Volume(VolumeControlType.ATTENUATION, 123d, false, 0.05);
		ReceiverStatusResponse receiverStatus = new ReceiverStatusResponse(0L, new ReceiverStatus(deviceVolume, null, false, false));
		message = message.toBuilder().setPayloadUtf8(jsonMapper.writeValueAsString(receiverStatus)).build();
		handler.processStringMessage(message, message.getPayloadUtf8());
		assertEquals(13, events.size());
		event = events.get(12);
		assertEquals(CastEventType.RECEIVER_STATUS, event.getEventType());
		assertEquals(deviceVolume, event.getData(ReceiverStatusResponse.class).getStatus().getVolume());

		channel.close();
	}

	@Test
	public void liveTest() throws Exception {
		MockedChromeCast mock = new MockedChromeCast();
		CastDevice cc = new CastDevice(
			"Mock",
			"localhost",
			null,
			null,
			"unique",
			EnumSet.of(CastDeviceCapability.AUDIO_OUT, CastDeviceCapability.VIDEO_OUT),
			"Mocked ChromeCast",
			"Mock",
			1,
			null,
			true
		);
		final List<CastEvent<?>> events = new CopyOnWriteArrayList<>();
		final CyclicBarrier barrier = new CyclicBarrier(2);
		cc.addEventListener(new CastEventListener() {

			@Override
			public void onEvent(CastEvent<?> event) {
				events.add(event);
				try {
					barrier.await();
				} catch (InterruptedException | BrokenBarrierException e) {
				}
			}
		});
		cc.connect();
		barrier.await(15, TimeUnit.SECONDS);
		barrier.reset();
		assertEquals(1, events.size());
		assertEquals(CastEventType.CONNECTED, events.get(0).getEventType());
		assertEquals(Boolean.TRUE, events.get(0).getData());
		cc.disconnect();
		mock.close();
	}

	private static class CustomMessage {

		@JsonProperty
		public String responseType;
		@JsonProperty
		public long requestId;
		@JsonProperty
		public String content;

//		@SuppressWarnings("unused")
//		CustomAppEvent() {
//		}
//
//		CustomAppEvent(
//			@JsonProperty("responseType") String responseType,
//			@JsonProperty("requestId") long requestId,
//			@JsonProperty("content") String content
//		) {
//			this.responseType = responseType;
//			this.requestId = requestId;
//			this.content = content;
//		}

		@Override
		public int hashCode() {
			return Objects.hash(content, requestId, responseType);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof CustomMessage)) {
				return false;
			}
			CustomMessage other = (CustomMessage) obj;
			return Objects.equals(content, other.content) && requestId == other.requestId
				&& Objects.equals(responseType, other.responseType);
		}
	}
}
