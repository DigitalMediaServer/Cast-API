package org.digitalmediaserver.chromecast.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.digitalmediaserver.chromecast.api.MediaStatus.IdleReason;
import org.digitalmediaserver.chromecast.api.MediaStatus.PlayerState;
import org.digitalmediaserver.chromecast.api.MediaStatus.RepeatMode;
import org.digitalmediaserver.chromecast.api.StandardResponse.MediaStatusResponse;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MediaStatusResponseTest {

	final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();

	@Test
	public void deserializationTest() throws JsonProcessingException {
		MediaStatus[] sourceArray = new MediaStatus[3];
		sourceArray[0] = new MediaStatus(
			new ArrayList<Integer>(),
			93485L,
			0.5f,
			PlayerState.IDLE,
			Integer.valueOf(6),
			30.0,
			new HashMap<String, Object>(),
			Integer.valueOf(4),
			new ArrayList<Item>(),
			Integer.valueOf(0),
			4,
			new Volume(0.8f, false, 0.1f, 0.02, "sometype"),
			new Media("someURL", "mime"),
			RepeatMode.REPEAT_ALL,
			IdleReason.FINISHED
		);
		sourceArray[1] = new MediaStatus(
			Arrays.asList(new Integer[] {2, 3, 8}),
			340234L,
			4,
			PlayerState.PLAYING,
			Integer.valueOf(3),
			5.34,
			new HashMap<String, Object>(),
			Integer.valueOf(19),
			Arrays.asList(new Item[] {new Item(false, new HashMap<String, Object>(), 5L, new Media("someUrl", "mimeType"))}),
			Integer.valueOf(2),
			19,
			new Volume(0.5f, false, 0.1f, 0.02, "sometype"),
			new Media("someURL", "mime"),
			RepeatMode.REPEAT_OFF,
			IdleReason.CANCELLED
		);
		sourceArray[2] = new MediaStatus(
			Arrays.asList(new Integer[] {5, 11, 18}),
			39854L,
			1,
			PlayerState.PAUSED,
			Integer.valueOf(11),
			19.93,
			new HashMap<String, Object>(),
			Integer.valueOf(11),
			Arrays.asList(new Item[] {new Item(true, new HashMap<String, Object>(), 18L, new Media("someUrl", "mimeType"))}),
			Integer.valueOf(5),
			32,
			new Volume(1f, true, 0.1f, 0.02, "sometype"),
			new Media("someURL", "mime"),
			RepeatMode.REPEAT_SINGLE,
			IdleReason.COMPLETED
		);
		MediaStatusResponse source = new MediaStatusResponse(sourceArray);
		source.setRequestId(12691L);

		String json = jsonMapper.writeValueAsString(source);
		MediaStatusResponse response = (MediaStatusResponse) jsonMapper.readValue(json, StandardResponse.class);
		assertEquals(12691L, response.getRequestId());
		assertNotNull(response.getStatuses());
		assertEquals(3, response.getStatuses().size());

		MediaStatus status = response.getStatuses().get(0);
		assertTrue(status.activeTrackIds.isEmpty());
		assertEquals(93485L, status.mediaSessionId);
		assertEquals(0.5f, status.playbackRate, 0f);
		assertEquals(PlayerState.IDLE, status.playerState);
		assertEquals(30.0, status.currentTime, 0.0);
		assertTrue(status.customData.isEmpty());
		assertEquals(4, status.loadingItemId.intValue());
		Volume volume = status.volume;
		assertNotNull(volume);
		assertEquals(0.8f, volume.level, 0f);
		assertFalse(volume.muted);
		assertEquals(0.1f, volume.increment, 0f);
		assertEquals(0.02, volume.stepInterval, 0.0);
		assertEquals("sometype", volume.controlType);
		Media media = status.media;
		assertNotNull(media);
		assertEquals("someURL", media.url);
		assertEquals("mime", media.contentType);
		assertEquals(RepeatMode.REPEAT_ALL, status.repeatMode);
		assertEquals(IdleReason.FINISHED, status.idleReason);

		status = response.getStatuses().get(1);
		List<Integer> intList = status.activeTrackIds;
		assertNotNull(intList);
		assertEquals(3, intList.size());
		assertEquals(8, intList.get(2).intValue());
		assertEquals(340234L, status.mediaSessionId);
		assertEquals(4f, status.playbackRate, 0f);
		assertEquals(PlayerState.PLAYING, status.playerState);
		assertEquals(5.34, status.currentTime, 0.0);
		assertTrue(status.customData.isEmpty());
		assertEquals(19, status.loadingItemId.intValue());
		volume = status.volume;
		assertNotNull(volume);
		assertEquals(0.5f, volume.level, 0f);
		assertFalse(volume.muted);
		assertEquals(0.1f, volume.increment, 0f);
		assertEquals(0.02, volume.stepInterval, 0.0);
		assertEquals("sometype", volume.controlType);
		media = status.media;
		assertNotNull(media);
		assertEquals("someURL", media.url);
		assertEquals("mime", media.contentType);
		assertEquals(RepeatMode.REPEAT_OFF, status.repeatMode);
		assertEquals(IdleReason.CANCELLED, status.idleReason);

		status = response.getStatuses().get(2);
		intList = status.activeTrackIds;
		assertNotNull(intList);
		assertEquals(3, intList.size());
		assertEquals(11, intList.get(1).intValue());
		assertEquals(39854L, status.mediaSessionId);
		assertEquals(1f, status.playbackRate, 0f);
		assertEquals(PlayerState.PAUSED, status.playerState);
		assertEquals(19.93, status.currentTime, 0.0);
		assertTrue(status.customData.isEmpty());
		assertEquals(11, status.loadingItemId.intValue());
		volume = status.volume;
		assertNotNull(volume);
		assertEquals(1f, volume.level, 0f);
		assertTrue(volume.muted);
		assertEquals(0.1f, volume.increment, 0f);
		assertEquals(0.02, volume.stepInterval, 0.0);
		assertEquals("sometype", volume.controlType);
		media = status.media;
		assertNotNull(media);
		assertEquals("someURL", media.url);
		assertEquals("mime", media.contentType);
		assertEquals(RepeatMode.REPEAT_SINGLE, status.repeatMode);
		assertEquals(IdleReason.COMPLETED, status.idleReason);
	}
}
