/*
 * Copyright 2017 Vitaly Litvak (vitavaque@gmail.com)
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CustomRequestTest {

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

	private static class KioskStatusRequest implements Request {

		@JsonProperty
		final boolean status;

		private long requestId;

		KioskStatusRequest() {
			status = true;
		}

		@Override
		public long getRequestId() {
			return requestId;
		}

		@Override
		public void setRequestId(long requestId) {
			this.requestId = requestId;
		}

	}

	private static class KioskStatusResponse implements Response {

		// current url
		@JsonProperty("url")
		String url;
		// current refresh rate
		@JsonProperty("refresh")
		int refresh;

		private long requestId;

		public String getUrl() {
			return this.url;
		}

		public int getRefresh() {
			return this.refresh;
		}

		@Override
		public long getRequestId() {
			return requestId;
		}

		@SuppressWarnings("unused")
		public void setRequestId(long arg0) {
			requestId = arg0;
		}
	}

	@Before
	public void init() throws IOException, GeneralSecurityException {
		chromeCastStub = new MockedChromeCast();
		cast.connect();
		cast.launchApplication("KIOSK", true);
	}

	@After
	public void destroy() throws IOException {
		cast.disconnect();
		chromeCastStub.close();
	}

	@Test
	public void test() throws IOException {
		chromeCastStub.customHandler = new MockedChromeCast.CustomHandler() {

			@Override
			public Response handle(JsonNode json) {
				long requestId;
				if (json.has("requestId")) {
					requestId = json.get("requestId").asLong();
				} else {
					requestId = 0L;
				}
				KioskStatusResponse response = new KioskStatusResponse();
				response.requestId = requestId;
				response.url = "http://google.com";
				response.refresh = 50;
				return response;
			}
		};
		KioskStatusResponse response = cast.channel().send(
			null,
			"urn:x-cast:de.michaelkuerbis.kiosk",
			new KioskStatusRequest(),
			"sender-0",
			"receiver-0",
			KioskStatusResponse.class
		);
		assertNotNull(response);
		assertEquals("http://google.com", response.getUrl());
		assertEquals(50, response.getRefresh());
	}
}
