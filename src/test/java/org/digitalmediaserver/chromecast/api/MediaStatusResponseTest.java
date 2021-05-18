package org.digitalmediaserver.chromecast.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.digitalmediaserver.chromecast.api.ContainerMetadata.ContainerType;
import org.digitalmediaserver.chromecast.api.ExtendedMediaStatus.ExtendedPlayerState;
import org.digitalmediaserver.chromecast.api.Media.StreamType;
import org.digitalmediaserver.chromecast.api.MediaStatus.IdleReason;
import org.digitalmediaserver.chromecast.api.MediaStatus.PlayerState;
import org.digitalmediaserver.chromecast.api.MediaStatus.RepeatMode;
import org.digitalmediaserver.chromecast.api.QueueData.QueueType;
import org.digitalmediaserver.chromecast.api.StandardResponse.MediaStatusResponse;
import org.digitalmediaserver.chromecast.api.VideoInformation.HdrType;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MediaStatusResponseTest {

	final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();

	@Test
	public void deserializationTest() throws JsonProcessingException {
		MediaStatus[] sourceArray = new MediaStatus[3];
		sourceArray[0] = new MediaStatus(
			new ArrayList<Integer>(),
			4,
			30.0,
			new HashMap<String, Object>(),
			new ExtendedMediaStatus(
				ExtendedPlayerState.LOADING,
				Media.builder("http://no.where", "none/empty", StreamType.NONE).build(),
				Integer.valueOf(14)
			),
			IdleReason.FINISHED,
			new ArrayList<QueueItem>(),
			new LiveSeekableRange(0d, 20d, false, true),
			Integer.valueOf(4),
			Media.builder("someURL", "mime", StreamType.NONE).build(),
			93485,
			0.5f,
			PlayerState.IDLE,
			Integer.valueOf(6),
			new QueueData(
				new ContainerMetadata(20.0, null, ContainerType.GENERIC_CONTAINER, "Container title"),
				"Container description",
				"entity",
				"Container 0",
				null,
				"Noname",
				QueueType.PODCAST_SERIES,
				RepeatMode.REPEAT_ALL_AND_SHUFFLE,
				Boolean.FALSE,
				Integer.valueOf(0),
				Double.valueOf(20d)
			),
			RepeatMode.REPEAT_ALL,
			Integer.valueOf(0),
			new VideoInformation(HdrType.DV, 2160, 3840),
			new MediaVolume(0.8, false)
		);
		sourceArray[1] = new MediaStatus(
			Arrays.asList(new Integer[] {2, 3, 8}),
			4,
			5.34,
			new HashMap<String, Object>(),
			null,
			IdleReason.CANCELLED,
			Arrays.asList(new QueueItem[] {
					new QueueItem(
						null,
						Boolean.FALSE,
						new HashMap<String, Object>(),
						Integer.valueOf(5),
						Media.builder("someUrl", "mimeType", StreamType.NONE).build(),
						null,
						null,
						null
					)
				}),
			null,
			Integer.valueOf(19),
			Media.builder("someURL", "mime", StreamType.NONE).build(),
			340234,
			4f,
			PlayerState.PLAYING,
			Integer.valueOf(3),
			null,
			RepeatMode.REPEAT_OFF,
			19,
			null,
			new MediaVolume(0.5d, false)
		);
		sourceArray[2] = new MediaStatus(
			Arrays.asList(new Integer[] {5, 11, 18}),
			1,
			19.93,
			new HashMap<String, Object>(),
			null,
			IdleReason.COMPLETED,
			Arrays.asList(new QueueItem[] {
					new QueueItem(
						null,
						Boolean.TRUE,
						new HashMap<String, Object>(),
						Integer.valueOf(18),
						Media.builder("someOtherUrl", "mimeType", StreamType.LIVE).build(),
						9,
						5.2,
						1.3
					)
				}),
			null,
			Integer.valueOf(11),
			Media.builder("someURL", "mime", StreamType.NONE).build(),
			39854,
			1f,
			PlayerState.PAUSED,
			Integer.valueOf(5),
			null,
			RepeatMode.REPEAT_SINGLE,
			Integer.valueOf(11),
			new VideoInformation(HdrType.HDR, 1080, 1920),
			new MediaVolume(1d, true)
		);
		MediaStatusResponse source = new MediaStatusResponse(12691L, sourceArray);

		String json = jsonMapper.writeValueAsString(source);
		MediaStatusResponse response = (MediaStatusResponse) jsonMapper.readValue(json, StandardResponse.class);
		assertEquals(12691L, response.getRequestId());
		assertNotNull(response.getStatuses());
		assertEquals(3, response.getStatuses().size());

		MediaStatus status = response.getStatuses().get(0);
		assertTrue(status.getActiveTrackIds().isEmpty());
		assertEquals(93485, status.getMediaSessionId());
		assertEquals(0.5f, status.getPlaybackRate(), 0f);
		assertEquals(PlayerState.IDLE, status.getPlayerState());
		assertEquals(30.0, status.getCurrentTime(), 0.0);
		assertTrue(status.getCustomData().isEmpty());
		ExtendedMediaStatus extendedStatus = status.getExtendedStatus();
		Media media = extendedStatus.getMedia();
		assertNotNull(media);
		assertEquals("", media.getContentId());
		assertEquals("none/empty", media.getContentType());
		assertEquals("http://no.where", media.getContentUrl());
		assertEquals("http://no.where", media.getUrl());
		assertTrue(media.getCustomData().isEmpty());
		assertNull(media.getDuration());
		assertNull(media.getEntity());
		assertNull(media.getHlsSegmentFormat());
		assertNull(media.getHlsVideoSegmentFormat());
		assertNull(media.getMediaCategory());
		assertTrue(media.getMetadata().isEmpty());
		assertNull(media.getStartAbsoluteTime());
		assertEquals(StreamType.NONE, media.getStreamType());
		assertNull(media.getTextTrackStyle());
		assertTrue(media.getTracks().isEmpty());
		assertEquals(14, extendedStatus.getMediaSessionId().intValue());
		assertEquals(ExtendedPlayerState.LOADING, extendedStatus.getPlayerState());
		assertTrue(status.getItems().isEmpty());
		LiveSeekableRange seekRange = status.getLiveSeekableRange();
		assertEquals(0d, seekRange.getStart().doubleValue(), 0d);
		assertEquals(20d, seekRange.getEnd().doubleValue(), 0d);
		assertFalse(seekRange.getIsLiveDone().booleanValue());
		assertTrue(seekRange.getIsMovingWindow().booleanValue());
		assertEquals(4, status.getLoadingItemId().intValue());
		QueueData queueData = status.getQueueData();
		ContainerMetadata containerMetadata = queueData.getContainerMetadata();
		assertEquals(20d, containerMetadata.getContainerDuration().doubleValue(), 0d);
		assertTrue(containerMetadata.getContainerImages().isEmpty());
		assertEquals(ContainerType.GENERIC_CONTAINER, containerMetadata.getContainerType());
		assertEquals("Container title", containerMetadata.getTitle());
		assertEquals("Container description", queueData.getDescription());
		assertEquals("entity", queueData.getEntity());
		assertEquals("Container 0", queueData.getId());
		assertTrue(queueData.getItems().isEmpty());
		assertEquals("Noname", queueData.getName());
		assertEquals(QueueType.PODCAST_SERIES, queueData.getQueueType());
		assertEquals(RepeatMode.REPEAT_ALL_AND_SHUFFLE, queueData.getRepeatMode());
		assertEquals(Boolean.FALSE, queueData.getShuffle());
		assertEquals(0, queueData.getStartIndex().intValue());
		assertEquals(20d, queueData.getStartTime().doubleValue(), 0d);
		VideoInformation videoInfo = status.getVideoInfo();
		assertEquals(HdrType.DV, videoInfo.getHdrType());
		assertEquals(2160, videoInfo.getHeight());
		assertEquals(3840, videoInfo.getWidth());
		MediaVolume volume = status.getVolume();
		assertNotNull(volume);
		assertEquals(0.8d, volume.getLevel().doubleValue(), 0d);
		assertFalse(volume.getMuted().booleanValue());
		media = status.getMedia();
		assertNotNull(media);
		assertEquals("someURL", media.getUrl());
		assertEquals("mime", media.getContentType());
		assertEquals(RepeatMode.REPEAT_ALL, status.getRepeatMode());
		assertEquals(IdleReason.FINISHED, status.getIdleReason());

		status = response.getStatuses().get(1);
		List<Integer> intList = status.getActiveTrackIds();
		assertNotNull(intList);
		assertEquals(3, intList.size());
		assertEquals(8, intList.get(2).intValue());
		assertEquals(340234, status.getMediaSessionId());
		assertEquals(4f, status.getPlaybackRate(), 0f);
		assertEquals(PlayerState.PLAYING, status.getPlayerState());
		assertEquals(5.34, status.getCurrentTime(), 0.0);
		assertTrue(status.getCustomData().isEmpty());
		assertEquals(19, status.getLoadingItemId().intValue());
		QueueItem item = status.getItems().get(0);
		assertTrue(item.getActiveTrackIds().isEmpty());
		assertEquals(Boolean.FALSE, item.getAutoplay());
		assertTrue(item.getCustomData().isEmpty());
		assertEquals(5, item.getItemId().intValue());
		media = item.getMedia();
		assertEquals("", media.getContentId());
		assertEquals("mimeType", media.getContentType());
		assertEquals("someUrl", media.getContentUrl());
		assertEquals(StreamType.NONE, media.getStreamType());
		assertNull(item.getOrderId());
		assertNull(item.getPreloadTime());
		assertNull(item.getStartTime());
		volume = status.getVolume();
		assertNotNull(volume);
		assertEquals(0.5d, volume.getLevel().doubleValue(), 0d);
		assertFalse(volume.getMuted().booleanValue());
		media = status.getMedia();
		assertNotNull(media);
		assertEquals("someURL", media.getUrl());
		assertEquals("mime", media.getContentType());
		assertEquals(RepeatMode.REPEAT_OFF, status.getRepeatMode());
		assertEquals(IdleReason.CANCELLED, status.getIdleReason());

		status = response.getStatuses().get(2);
		intList = status.getActiveTrackIds();
		assertNotNull(intList);
		assertEquals(3, intList.size());
		assertEquals(11, intList.get(1).intValue());
		assertEquals(39854, status.getMediaSessionId());
		assertEquals(1f, status.getPlaybackRate(), 0f);
		assertEquals(PlayerState.PAUSED, status.getPlayerState());
		assertEquals(19.93, status.getCurrentTime(), 0.0);
		assertTrue(status.getCustomData().isEmpty());
		assertEquals(11, status.getLoadingItemId().intValue());
		item = status.getItems().get(0);
		assertTrue(item.getActiveTrackIds().isEmpty());
		assertEquals(Boolean.TRUE, item.getAutoplay());
		assertTrue(item.getCustomData().isEmpty());
		assertEquals(18, item.getItemId().intValue());
		media = item.getMedia();
		assertEquals("", media.getContentId());
		assertEquals("mimeType", media.getContentType());
		assertEquals("someOtherUrl", media.getContentUrl());
		assertEquals(StreamType.LIVE, media.getStreamType());
		assertEquals(9, item.getOrderId().intValue());
		assertEquals(5.2, item.getPreloadTime().doubleValue(), 0d);
		assertEquals(1.3, item.getStartTime().doubleValue(), 0d);
		volume = status.getVolume();
		assertNotNull(volume);
		assertEquals(1d, volume.getLevel().doubleValue(), 0d);
		assertTrue(volume.getMuted().booleanValue());
		media = status.getMedia();
		assertNotNull(media);
		assertEquals("someURL", media.getUrl());
		assertEquals("mime", media.getContentType());
		assertEquals(RepeatMode.REPEAT_SINGLE, status.getRepeatMode());
		assertEquals(IdleReason.COMPLETED, status.getIdleReason());
		videoInfo = status.getVideoInfo();
		assertEquals(HdrType.HDR, videoInfo.getHdrType());
		assertEquals(1080, videoInfo.getHeight());
		assertEquals(1920, videoInfo.getWidth());
	}
}
