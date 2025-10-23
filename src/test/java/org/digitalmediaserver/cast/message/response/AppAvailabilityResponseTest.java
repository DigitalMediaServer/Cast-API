package org.digitalmediaserver.cast.message.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.digitalmediaserver.cast.util.JacksonHelper;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AppAvailabilityResponseTest {

	final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();

	@Test
	public void deserializationTest() throws JsonProcessingException {
		Map<String, String> sourceMap = new HashMap<>();
		sourceMap.put("key1", "value1");
		sourceMap.put("key2", "value2");
		sourceMap.put("key3", "value3");
		AppAvailabilityResponse source = new AppAvailabilityResponse(22391L, sourceMap);

		String json = jsonMapper.writeValueAsString(source);
		AppAvailabilityResponse response = (AppAvailabilityResponse) jsonMapper.readValue(json, StandardResponse.class);
		assertEquals(22391L, response.getRequestId());
		assertNotNull(response.getAvailability());
		assertEquals(3, response.getAvailability().size());
		assertEquals("value1", response.getAvailability().get("key1"));
		assertEquals("value2", response.getAvailability().get("key2"));
		assertEquals("value3", response.getAvailability().get("key3"));
	}
}
