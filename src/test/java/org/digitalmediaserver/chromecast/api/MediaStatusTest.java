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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.digitalmediaserver.chromecast.api.MediaStatus.PlayerState;
import org.digitalmediaserver.chromecast.api.MediaStatus.RepeatMode;
import org.junit.Test;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MediaStatusTest {

	final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();

	@Test
	public void testDeserializationWithIdleReason() throws Exception {
		final StandardResponse.MediaStatusResponse response = (StandardResponse.MediaStatusResponse) jsonMapper
			.readValue(getClass().getResourceAsStream("/mediaStatus-with-idleReason.json"), StandardResponse.class);
		assertEquals(1, response.getStatuses().size());
		MediaStatus mediaStatus = response.getStatuses().get(0);
		assertEquals(MediaStatus.IdleReason.ERROR, mediaStatus.getIdleReason());
	}

	@Test
	public void testDeserializationWithoutIdleReason() throws Exception {
		final StandardResponse.MediaStatusResponse response = (StandardResponse.MediaStatusResponse) jsonMapper
			.readValue(getClass().getResourceAsStream("/mediaStatus-without-idleReason.json"), StandardResponse.class);
		assertEquals(1, response.getStatuses().size());
		MediaStatus mediaStatus = response.getStatuses().get(0);
		assertNull(mediaStatus.getIdleReason());
	}

	@Test
	public void testDeserializationWithChromeCastAudioFixture() throws Exception {
		final String jsonMSG = FixtureHelper.fixtureAsString("/mediaStatus-chromecast-audio.json").replaceFirst("\"type\"",
			"\"responseType\"");
		final StandardResponse.MediaStatusResponse response = (StandardResponse.MediaStatusResponse) jsonMapper.readValue(jsonMSG, StandardResponse.class);
		assertEquals(1, response.getStatuses().size());
		final MediaStatus mediaStatus = response.getStatuses().get(0);
		assertEquals((Integer) 1, mediaStatus.getCurrentItemId());
		assertEquals(0f, mediaStatus.getCurrentTime(), 0f);

		final Media media = new Media("http://192.168.1.6:8192/audio-123-mp3", "audio/mpeg", 389.355102d, Media.StreamType.BUFFERED);

		final Map<String, String> payload = new HashMap<>();
		payload.put("thumb", null);
		payload.put("title", "Example Track Title");
		final Map<String, Object> customData = new HashMap<>();
		customData.put("payload", payload);
		assertEquals(Collections.singletonList(new Item(true, customData, 1, media)), mediaStatus.getItems());

		assertEquals(media, mediaStatus.getMedia());
		assertEquals(Media.MetadataType.GENERIC, media.getMetadataType());
		assertEquals(1, mediaStatus.getMediaSessionId());
		assertEquals(1, mediaStatus.getPlaybackRate(), 0f);
		assertEquals(PlayerState.BUFFERING, mediaStatus.getPlayerState());
		assertEquals(RepeatMode.REPEAT_OFF, mediaStatus.getRepeatMode());
		assertEquals(15, mediaStatus.getSupportedMediaCommands());
		assertEquals(new Volume(1f, false, Volume.DEFAULT_INCREMENT, Volume.DEFAULT_INCREMENT.doubleValue(), Volume.DEFAULT_CONTROL_TYPE),
			mediaStatus.getVolume());
	}

	@Test
	public void testDeserializationPandora() throws IOException {
		final StandardResponse.MediaStatusResponse response = (StandardResponse.MediaStatusResponse) jsonMapper
			.readValue(getClass().getResourceAsStream("/mediaStatus-pandora.json"), StandardResponse.class);

		assertEquals(1, response.getStatuses().size());
		final MediaStatus mediaStatus = response.getStatuses().get(0);
		assertNull(mediaStatus.getCurrentItemId());
		assertEquals(16d, mediaStatus.getCurrentTime(), 0.1);
		assertEquals(7, mediaStatus.getMediaSessionId());
		assertEquals(1, mediaStatus.getPlaybackRate(), 0f);
		assertEquals(PlayerState.PLAYING, mediaStatus.getPlayerState());
		assertTrue(mediaStatus.getCustomData().isEmpty());
		assertTrue(mediaStatus.getItems().isEmpty());
		assertNull(mediaStatus.getPreloadedItemId());

		assertEquals(new Volume(0.6999999f, false, 0.05f, null, null), mediaStatus.getVolume());

		assertNotNull(mediaStatus.getMedia());
		Media media = mediaStatus.getMedia();
		assertEquals(7, media.getMetadata().size());
		assertEquals(Media.MetadataType.MUSIC_TRACK, media.getMetadataType());
		assertEquals("http://audioURL", media.getUrl());
		assertEquals(246d, media.getDuration(), 0.1);
		assertEquals(Media.StreamType.BUFFERED, media.getStreamType());
		assertEquals("BUFFERED", media.getContentType());
		assertTrue(media.getTextTrackStyle().isEmpty());
		assertTrue(media.getTracks().isEmpty());
		assertEquals(1, media.getCustomData().size());
		assertNotNull(media.getCustomData().get("status"));
		Map<?, ?> status = (Map<?, ?>) media.getCustomData().get("status");

		assertEquals(8, status.size());
		assertEquals(2, status.get("state"));
	}

	@Test
	public void testDeserializationNoMetadataType() throws IOException {
		final StandardResponse.MediaStatusResponse response = (StandardResponse.MediaStatusResponse) jsonMapper
			.readValue(getClass().getResourceAsStream("/mediaStatus-no-metadataType.json"), StandardResponse.class);

		final MediaStatus mediaStatus = response.getStatuses().get(0);
		Media media = mediaStatus.getMedia();
		assertEquals(Media.MetadataType.GENERIC, media.getMetadataType());
	}

	@Test
	public void testDeserializationUnknownMetadataType() throws IOException {
		final StandardResponse.MediaStatusResponse response = (StandardResponse.MediaStatusResponse) jsonMapper
			.readValue(getClass().getResourceAsStream("/mediaStatus-unknown-metadataType.json"), StandardResponse.class);

		final MediaStatus mediaStatus = response.getStatuses().get(0);
		Media media = mediaStatus.getMedia();
		assertEquals(Media.MetadataType.GENERIC, media.getMetadataType());
	}

	@Test
	public void testDeserializationWithVideoInfo() throws IOException {
		final String jsonMSG = FixtureHelper.fixtureAsString("/mediaStatus-with-videoinfo.json").replaceFirst("\"type\"",
			"\"responseType\"");
		final StandardResponse.MediaStatusResponse response = (StandardResponse.MediaStatusResponse) jsonMapper.readValue(jsonMSG, StandardResponse.class);
		assertEquals(1, response.getStatuses().size());
	}

	@Test
	public void testDeserializationAudioWithExtraStatus() throws IOException {
		final String jsonMSG = FixtureHelper.fixtureAsString("/mediaStatus-audio-with-extraStatus.json").replaceFirst("\"type\"",
			"\"responseType\"");
		final StandardResponse.MediaStatusResponse response = (StandardResponse.MediaStatusResponse) jsonMapper.readValue(jsonMSG, StandardResponse.class);
		assertEquals(1, response.getStatuses().size());
	}
}
