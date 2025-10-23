/*
 * Copyright 2019 Vitaly Litvak (vitavaque@gmail.com)
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
package org.digitalmediaserver.cast.message.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.digitalmediaserver.cast.FixtureHelper;
import org.digitalmediaserver.cast.util.JacksonHelper;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class MultizoneStatusResponseTest {

	final ObjectMapper jsonMapper = JacksonHelper.createJSONMapper();

	@Test
	public void testStandard() throws IOException {
		final String jsonMSG = FixtureHelper.fixtureAsString("/multizoneStatus.json").replaceFirst("\"type\"", "\"responseType\"");
		final MultizoneStatusResponse response = (MultizoneStatusResponse) jsonMapper.readValue(
			jsonMSG,
			StandardResponse.class
		);
		assertNotNull(response.getStatus());
		assertEquals(1, response.getStatus().getDevices().size());
		assertFalse(response.getStatus().isMultichannel());
		assertEquals("Living Room speaker", response.getStatus().getDevices().get(0).getName());
		assertEquals(196612, response.getStatus().getDevices().get(0).getCapabilities());
		assertNotNull(response.getStatus().getDevices().get(0).getVolume());
	}
}
