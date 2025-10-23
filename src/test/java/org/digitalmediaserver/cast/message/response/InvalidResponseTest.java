package org.digitalmediaserver.cast.message.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import org.digitalmediaserver.cast.message.enumeration.ErrorReason;
import org.digitalmediaserver.cast.message.enumeration.ErrorType;
import org.digitalmediaserver.cast.util.JacksonHelper;

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
