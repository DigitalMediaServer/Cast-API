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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for creating pre-configured instances of JSON mapper.
 */
public class JacksonHelper {

	/**
	 * Not to be instantiated.
	 */
	private JacksonHelper() {
	}

	/**
	 * @return A new configured {@link ObjectMapper}.
	 */
	public static ObjectMapper createJSONMapper() {
		ObjectMapper jsonMapper = new ObjectMapper();
		jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return jsonMapper;
	}
}
