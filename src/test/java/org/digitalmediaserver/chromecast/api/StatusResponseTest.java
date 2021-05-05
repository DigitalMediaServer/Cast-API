package org.digitalmediaserver.chromecast.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.digitalmediaserver.chromecast.api.StandardResponse.StatusResponse;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StatusResponseTest {

	final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();

	@Test
	public void deserializationTest() throws JsonProcessingException {
		List<Namespace> nameSpaces = new ArrayList<>();
		nameSpaces.add(new Namespace("some.name.space"));
		nameSpaces.add(new Namespace("some.other.name.space"));
		List<Application> applications = new ArrayList<>();
		applications.add(new Application("appId", "iconURL", "appName", "jkl34d", "single", true, false, "55", nameSpaces));
		Status status = new Status(new Volume(0.55f, true, 0.2f, 0.01, "wild"), applications, false, true);
		StatusResponse source = new StatusResponse(status);
		source.setRequestId(3591L);

		String json = jsonMapper.writeValueAsString(source);
		StatusResponse response = (StatusResponse) jsonMapper.readValue(json, StandardResponse.class);
		assertEquals(3591L, response.getRequestId());
		status = response.getStatus();
		Volume volume = status.volume;
		assertNotNull(volume);
		assertEquals(0.55f, volume.level, 0f);
		assertTrue(volume.muted);
		assertEquals(0.2f, volume.increment, 0f);
		assertEquals(0.01, volume.stepInterval, 0.0);
		assertEquals("wild", volume.controlType);
		assertFalse(status.applications.isEmpty());
		applications = status.applications;
		assertNotNull(applications);
		Application app = applications.get(0);
		assertEquals("appId", app.id);
		assertEquals("iconURL", app.iconUrl);
		assertEquals("appName", app.name);
		assertEquals("jkl34d", app.sessionId);
		assertEquals("single", app.statusText);
		assertTrue(app.isIdleScreen);
		assertFalse(app.launchedFromCloud);
		assertEquals("55", app.transportId);
		nameSpaces = app.namespaces;
		assertNotNull(nameSpaces);
		assertFalse(nameSpaces.isEmpty());
		assertEquals(2, nameSpaces.size());
		assertEquals("some.name.space", nameSpaces.get(0).name);
		assertEquals("some.other.name.space", nameSpaces.get(1).name);
		assertFalse(status.activeInput);
		assertTrue(status.standBy);
	}
}
