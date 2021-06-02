package org.digitalmediaserver.cast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.digitalmediaserver.cast.StandardResponse.ErrorReason;
import org.digitalmediaserver.cast.StandardResponse.ErrorResponse;
import org.digitalmediaserver.cast.StandardResponse.ErrorType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InvalidResponseTest {

	final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();

	@Test
	public void deserializationTest() throws JsonProcessingException {
		ErrorResponse source = new ErrorResponse(null, null, null, ErrorReason.INVALID_COMMAND, 442L, ErrorType.INVALID_REQUEST);
		String json = jsonMapper.writeValueAsString(source);
		ErrorResponse response = (ErrorResponse) jsonMapper.readValue(json, StandardResponse.class);
		assertEquals(ErrorType.INVALID_REQUEST, response.getType());
		assertEquals(442L, response.getRequestId());
		assertEquals(ErrorReason.INVALID_COMMAND, response.getReason());
	}
}
