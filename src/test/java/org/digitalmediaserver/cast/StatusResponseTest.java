package org.digitalmediaserver.cast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.digitalmediaserver.cast.message.entity.Application;
import org.digitalmediaserver.cast.message.entity.Namespace;
import org.digitalmediaserver.cast.message.entity.ReceiverStatus;
import org.digitalmediaserver.cast.message.entity.Volume;
import org.digitalmediaserver.cast.message.enumeration.VolumeControlType;
import org.digitalmediaserver.cast.message.response.ReceiverStatusResponse;
import org.digitalmediaserver.cast.message.response.StandardResponse;
import org.digitalmediaserver.cast.util.JacksonHelper;
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
		applications.add(new Application(
			"appId",
			"appName",
			"iconURL",
			Boolean.TRUE,
			Boolean.FALSE,
			nameSpaces,
			"jkl34d",
			"single",
			"55",
			"universalAppId"
		));
		ReceiverStatus status = new ReceiverStatus(new Volume(VolumeControlType.ATTENUATION, 0.55, true, 0.01), applications, false, true);
		ReceiverStatusResponse source = new ReceiverStatusResponse(3591L, status);

		String json = jsonMapper.writeValueAsString(source);
		ReceiverStatusResponse response = (ReceiverStatusResponse) jsonMapper.readValue(json, StandardResponse.class);
		assertEquals(3591L, response.getRequestId());
		status = response.getStatus();
		Volume volume = status.getVolume();
		assertNotNull(volume);
		assertEquals(0.55, volume.getLevel(), 0d);
		assertTrue(volume.getMuted());
		assertEquals(0.01, volume.getStepInterval(), 0.0);
		assertEquals(VolumeControlType.ATTENUATION, volume.getControlType());
		assertFalse(status.getApplications().isEmpty());
		applications = status.getApplications();
		assertNotNull(applications);
		Application app = applications.get(0);
		assertEquals("appId", app.getAppId());
		assertEquals("iconURL", app.getIconUrl());
		assertEquals("appName", app.getDisplayName());
		assertEquals("jkl34d", app.getSessionId());
		assertEquals("single", app.getStatusText());
		assertTrue(app.isIdleScreen());
		assertFalse(app.isLaunchedFromCloud());
		assertEquals("55", app.getTransportId());
		nameSpaces = app.getNamespaces();
		assertNotNull(nameSpaces);
		assertFalse(nameSpaces.isEmpty());
		assertEquals(2, nameSpaces.size());
		assertEquals("some.name.space", nameSpaces.get(0).getName());
		assertEquals("some.other.name.space", nameSpaces.get(1).getName());
		assertFalse(status.isActiveInput());
		assertTrue(status.isStandBy());
	}
}
