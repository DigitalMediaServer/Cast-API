/*
 * Copyright 2018 Vitaly Litvak (vitavaque@gmail.com)
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
import org.digitalmediaserver.chromecast.api.Volume.VolumeControlType;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DeviceTest {

	final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();

	@Test
	public void testDeviceAdded() throws IOException {
		final String jsonMSG = FixtureHelper.fixtureAsString("/device-added.json").replaceFirst("\"type\"", "\"responseType\"");
		final StandardResponse.DeviceAddedResponse response = jsonMapper.readValue(jsonMSG, StandardResponse.DeviceAddedResponse.class);

		assertNotNull(response.getDevice());
		Device device = response.getDevice();
		assertEquals("Amplifier", device.getName());
		assertEquals("123456", device.getDeviceId());
		assertEquals(4, device.getCapabilities());
		assertNotNull(device.getVolume());
		Volume volume = device.getVolume();
		assertEquals(0.24, volume.getLevel(), 0.001);
		assertFalse(volume.isMuted());
		assertNotNull(volume.getStepInterval());
		assertEquals(VolumeControlType.ATTENUATION, volume.getControlType());
	}

	@Test
	public void testDeviceRemoved() throws IOException {
		final String jsonMSG = FixtureHelper.fixtureAsString("/device-removed.json").replaceFirst("\"type\"", "\"responseType\"");
		final StandardResponse.DeviceRemovedResponse response = jsonMapper.readValue(jsonMSG, StandardResponse.DeviceRemovedResponse.class);

		assertNotNull(response.getDeviceId());
		assertEquals("111111", response.getDeviceId());
	}

	@Test
	public void testDeviceUpdated() throws IOException {
		final String jsonMSG = FixtureHelper.fixtureAsString("/device-updated.json").replaceFirst("\"type\"", "\"responseType\"");
		final StandardResponse.DeviceUpdatedResponse response = jsonMapper.readValue(jsonMSG, StandardResponse.DeviceUpdatedResponse.class);

		assertNotNull(response.getDevice());
		Device device = response.getDevice();
		assertEquals("Amplifier", device.getName());
		assertEquals("654321", device.getDeviceId());
		assertEquals(4, device.getCapabilities());
		assertNotNull(device.getVolume());
		Volume volume = device.getVolume();
		assertEquals(0.35, volume.getLevel(), 0.001);
		assertFalse(volume.isMuted());
		assertNotNull(volume.getStepInterval());
		assertEquals(VolumeControlType.ATTENUATION, volume.getControlType());
	}
}
