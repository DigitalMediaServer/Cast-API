package org.digitalmediaserver.cast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.digitalmediaserver.cast.StandardResponse.LaunchErrorResponse;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LaunchErrorResponseTest {

	final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();

	@Test
	public void deserializationTest() throws JsonProcessingException {
		LaunchErrorResponse source = new LaunchErrorResponse(92342L, "Couldn't ignite main engine");

		String json = jsonMapper.writeValueAsString(source);
		LaunchErrorResponse response = (LaunchErrorResponse) jsonMapper.readValue(json, StandardResponse.class);
		assertEquals(92342L, response.getRequestId());
		assertEquals("Couldn't ignite main engine", response.getReason());
	}
}
