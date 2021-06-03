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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.digitalmediaserver.chromecast.api.Media.MediaBuilder;
import org.digitalmediaserver.chromecast.api.Media.MediaCategory;
import org.digitalmediaserver.chromecast.api.Media.StreamType;
import org.digitalmediaserver.chromecast.api.Metadata.Image;
import org.junit.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;

public class MediaTest {

	final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();

	@Test
	public void itIncludesOptionalFieldsWhenSet() throws Exception {
		Map<String, Object> customData = new HashMap<>();
		customData.put("a", "b");
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("1", "2");
		Media m = new Media(
			null,
			null,
			null,
			customData,
			123.456d,
			null,
			null,
			null,
			null,
			metadata,
			null,
			StreamType.BUFFERED,
			null,
			null
		);

		String json = jsonMapper.writeValueAsString(m);

		assertThat(json, containsString("\"duration\":123.456"));
		assertThat(json, containsString("\"streamType\":\"BUFFERED\""));
		assertThat(json, containsString("\"customData\":{\"a\":\"b\"}"));
		assertThat(json, containsString("\"metadata\":{\"1\":\"2\"}"));
	}

	@Test
	public void itDoesNotContainOptionalFieldsWhenNotSet() throws Exception {
		Media m = new Media(
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null
		);

		String json = jsonMapper.writeValueAsString(m);

		assertThat(json, containsString("contentId"));
		assertThat(json, containsString("contentType"));
		assertThat(json, containsString("streamType"));
		assertThat(json, not(containsString("duration")));
		assertThat(json, not(containsString("customData")));
		assertThat(json, not(containsString("metadata")));
		assertThat(json, not(containsString("metadataType")));
	}

	@Test
	public void metadataTest() throws Exception {
		MediaBuilder builder = Media.builder("http://somevideo.mpg", "video/mpeg", StreamType.BUFFERED)
			.duration(Double.valueOf(120d)).mediaCategory(MediaCategory.VIDEO);
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put(Metadata.Generic.TITLE, "Title");
		metadata.put(Metadata.Generic.SUBTITLE, "Subtitle");
		List<Image> images = new ArrayList<>();
		images.add(new Image("http://somevideo.png", 1024, 768));
		images.add(new Image("http://someVideo_thumb.jpg"));
		Metadata.setImages(metadata, images);
		builder.metadata(metadata);

		Media source = builder.build();
		String json = jsonMapper.writeValueAsString(source);
		Media result = jsonMapper.readValue(json, Media.class);
		assertEquals(source, result);
		List<Image> resultImages = result.getImages();
		assertEquals(images, resultImages);
	}
}
