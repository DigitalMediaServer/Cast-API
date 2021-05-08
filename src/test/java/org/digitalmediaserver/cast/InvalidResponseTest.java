package org.digitalmediaserver.cast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.digitalmediaserver.cast.StandardResponse.InvalidResponse;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InvalidResponseTest {

	final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();

	@Test
	public void deserializationTest() throws JsonProcessingException {
		InvalidResponse source = new InvalidResponse(442L, "Couldn't find my wallet");
		String json = jsonMapper.writeValueAsString(source);
		InvalidResponse response = (InvalidResponse) jsonMapper.readValue(json, StandardResponse.class);
		assertEquals(442L, response.getRequestId());
		assertEquals("Couldn't find my wallet", response.getReason());
	}
}