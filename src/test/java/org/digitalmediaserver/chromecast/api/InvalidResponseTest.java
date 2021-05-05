package org.digitalmediaserver.chromecast.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.digitalmediaserver.chromecast.api.StandardResponse.InvalidResponse;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InvalidResponseTest {

	final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();

	@Test
	public void deserializationTest() throws JsonProcessingException {
		InvalidResponse source = new InvalidResponse("Couldn't find my wallet");
		source.setRequestId(442L);

		String json = jsonMapper.writeValueAsString(source);
		InvalidResponse response = (InvalidResponse) jsonMapper.readValue(json, StandardResponse.class);
		assertEquals(442L, response.getRequestId());
		assertEquals("Couldn't find my wallet", response.getReason());
	}
}
