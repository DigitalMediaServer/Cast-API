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
package org.digitalmediaserver.cast;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConnectionLostTest {

	MockedChromeCast chromeCastStub;
	CastDevice cast = new CastDevice(
		"Mock",
		"localhost",
		null,
		null,
		null,
		null,
		"Mocked ChromeCast",
		null,
		1,
		null,
		true
	);

	@Before
	public void initMockedCast() throws Exception {
		chromeCastStub = new MockedChromeCast();
		cast.connect();
		chromeCastStub.close();
		// ensure that chrome cast disconnected
		int retry = 0;
		while (cast.isConnected() && retry++ < 25) {
			Thread.sleep(50);
		}
		assertTrue("ChromeCast wasn't properly disconnected", retry < 25);
	}

	@Test(expected = ConnectException.class)
	public void testDisconnect() throws Exception {
		assertNull(cast.getReceiverStatus());
	}

	@Test
	public void testReconnect() throws Exception {
		chromeCastStub = new MockedChromeCast();
		assertNotNull(cast.getReceiverStatus());
	}

	@After
	public void shutdown() throws IOException {
		if (cast.isConnected()) {
			cast.disconnect();
		}
		chromeCastStub.close();
	}
}
