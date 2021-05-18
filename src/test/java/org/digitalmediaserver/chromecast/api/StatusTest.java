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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.digitalmediaserver.chromecast.api.Volume.VolumeControlType;

public class StatusTest {

	final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();

	@Test
	public void testDeserializationBackdrop118() throws Exception {
		final String jsonMSG = FixtureHelper.fixtureAsString("/status-backdrop-1.18.json")
			.replaceFirst("\"type\"", "\"responseType\"");
		final StandardResponse.ReceiverStatusResponse response = jsonMapper.readValue(jsonMSG, StandardResponse.ReceiverStatusResponse.class);

		ReceiverStatus status = response.getStatus();
		assertNotNull(status);
		assertTrue(status.isActiveInput());
		assertFalse(status.isStandBy());

		assertEquals(1, status.getApplications().size());
		Application app = status.getRunningApplication();
		assertFalse(app.isIdleScreen());

		Volume volume = status.getVolume();
		assertNotNull(volume);
		assertEquals(1.0, volume.getLevel(), 0.1);
		assertFalse(volume.getMuted());
		assertNull(volume.getControlType());
		assertNull(volume.getStepInterval());
	}

	@Test
	public void testDeserializationBackdrop119() throws Exception {
		final String jsonMSG = FixtureHelper.fixtureAsString("/status-backdrop-1.19.json")
			.replaceFirst("\"type\"", "\"responseType\"");
		final StandardResponse.ReceiverStatusResponse response = jsonMapper.readValue(jsonMSG, StandardResponse.ReceiverStatusResponse.class);

		ReceiverStatus status = response.getStatus();
		assertNotNull(status);
		assertFalse(status.isActiveInput());
		assertFalse(status.isStandBy());

		assertEquals(1, status.getApplications().size());
		Application app = status.getRunningApplication();
		assertTrue(app.isIdleScreen());

		Volume volume = status.getVolume();
		assertNotNull(volume);
		assertEquals(1.0, volume.getLevel(), 0.1);
		assertFalse(volume.getMuted());
		assertEquals(VolumeControlType.ATTENUATION, volume.getControlType());
		assertEquals(0.04, volume.getStepInterval(), 0.001);
	}

	@Test
	public void testDeserializationBackdrop128() throws Exception {
		final String jsonMSG = FixtureHelper.fixtureAsString("/status-backdrop-1.28.json")
			.replaceFirst("\"type\"", "\"responseType\"");
		final StandardResponse.ReceiverStatusResponse response = jsonMapper.readValue(jsonMSG, StandardResponse.ReceiverStatusResponse.class);

		ReceiverStatus status = response.getStatus();
		assertNotNull(status);
		assertFalse(status.isActiveInput());
		assertFalse(status.isStandBy());

		assertEquals(1, status.getApplications().size());
		Application app = status.getRunningApplication();
		assertTrue(app.isIdleScreen());
		assertFalse(app.isLaunchedFromCloud());

		Volume volume = status.getVolume();
		assertNotNull(volume);
		assertEquals(1.0, volume.getLevel(), 0.1);
		assertFalse(volume.getMuted());
		assertEquals(VolumeControlType.ATTENUATION, volume.getControlType());
		assertEquals(0.05, volume.getStepInterval(), 0.001);
	}

	@Test
	public void testDeserializationChromeMirroring() throws Exception {
		final String jsonMSG = FixtureHelper.fixtureAsString("/status-chrome-mirroring-1.28.json")
			.replaceFirst("\"type\"", "\"responseType\"");
		final StandardResponse.ReceiverStatusResponse response = jsonMapper.readValue(jsonMSG, StandardResponse.ReceiverStatusResponse.class);

		ReceiverStatus status = response.getStatus();
		assertNotNull(status);
		assertFalse(status.isActiveInput());
		assertFalse(status.isStandBy());

		assertEquals(1, status.getApplications().size());
		Application app = status.getRunningApplication();
		assertFalse(app.isIdleScreen());
		assertFalse(app.isLaunchedFromCloud());

		Volume volume = status.getVolume();
		assertNotNull(volume);
		assertEquals(1.0, volume.getLevel(), 0.1);
		assertFalse(volume.getMuted());
		assertEquals(VolumeControlType.ATTENUATION, volume.getControlType());
		assertEquals(0.05, volume.getStepInterval(), 0.001);
	}

	@Test
	public void testDeserializationSpotify() throws Exception {
		final String jsonMSG = FixtureHelper.fixtureAsString("/status-spotify.json")
			.replaceFirst("\"type\"", "\"responseType\"");
		final StandardResponse.ReceiverStatusResponse response = jsonMapper.readValue(jsonMSG, StandardResponse.ReceiverStatusResponse.class);

		ReceiverStatus status = response.getStatus();
		assertNotNull(status);
		assertFalse(status.isActiveInput());
		assertFalse(status.isStandBy());

		assertEquals(1, status.getApplications().size());
		Application app = status.getRunningApplication();
		assertFalse(app.isIdleScreen());
		assertFalse(app.isLaunchedFromCloud());
		assertEquals("CC32E753", app.getAppId());
		assertEquals("Spotify", app.getDisplayName());
		assertEquals(
			"https://lh3.googleusercontent.com/HOX9yqNu6y87Chb1lHYqhK" + "VTQW43oFAFFe2ojx94yCLh0yMzgygTrM0RweAexApRWqq6UahgrWYimVgK",
			app.getIconUrl());
		assertEquals(6, app.getNamespaces().size());
		assertEquals("urn:x-cast:com.google.cast.debugoverlay", app.getNamespaces().get(0).getName());
		assertEquals("urn:x-cast:com.google.cast.cac", app.getNamespaces().get(1).getName());
		assertEquals("urn:x-cast:com.spotify.chromecast.secure.v1", app.getNamespaces().get(2).getName());
		assertEquals("urn:x-cast:com.google.cast.test", app.getNamespaces().get(3).getName());
		assertEquals("urn:x-cast:com.google.cast.broadcast", app.getNamespaces().get(4).getName());
		assertEquals("urn:x-cast:com.google.cast.media", app.getNamespaces().get(5).getName());
		assertEquals("7fb71850-b38b-43bb-967e-e2c76b6d0990", app.getSessionId());
		assertEquals("Spotify", app.getStatusText());
		assertEquals("7fb71850-b38b-43bb-967e-e2c76b6d0990", app.getTransportId());

		Volume volume = status.getVolume();
		assertNotNull(volume);
		assertEquals(0.2258118838071823, volume.getLevel(), 0.001);
		assertFalse(volume.getMuted());
		assertEquals(VolumeControlType.MASTER, volume.getControlType());
		assertEquals(0.019999999552965164, volume.getStepInterval(), 0.001);
	}
}
