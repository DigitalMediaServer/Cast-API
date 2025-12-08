/*
 * Copyright (C) 2025 Digital Media Server developers.
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
package org.digitalmediaserver.cast.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;


/**
 * A custom deserializer for {@code fontFamily} values since despite the
 * documentation stating that they should be strings, it's not always the case,
 * which leads to parsing errors.
 *
 * @author Nadahar
 */
public class FontFamilyDeserializer extends StdDeserializer<String> {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance.
	 */
	public FontFamilyDeserializer() {
		super((Class<?>) null);
	}

	/**
	 * Creates a new instance using the specified parameter.
	 *
	 * @param vc the {@link Class} to use.
	 */
	public FontFamilyDeserializer(Class<?> vc) {
		super(vc);
	}

	/**
	 * Creates a new instance using the specified parameter.
	 *
	 * @param valueType the {@link JavaType} to use.
	 */
	public FontFamilyDeserializer(JavaType valueType) {
		super(valueType);
	}

	/**
	 * Creates a new instance using the specified parameter.
	 *
	 * @param src the {@link StdDeserializer} to use.
	 */
	public FontFamilyDeserializer(StdDeserializer<?> src) {
		super(src);
	}

	@Override
	public String deserialize(
		JsonParser parser,
		DeserializationContext ctxt
	) throws IOException, JsonProcessingException {
		JsonNode node = parser.getCodec().readTree(parser);
		if (node.isContainerNode()) {
			List<String> strings = new ArrayList<>();
			String s;
			if (node.isArray()) {
				for (int i = 0; i < node.size(); i++) {
					s = node.get(i).textValue();
					if (!Util.isBlank(s)) {
						strings.add(s);
					}
				}
			} else {
				Entry<String, JsonNode> entry;
				for (Iterator<Entry<String, JsonNode>> iterator = node.fields(); iterator.hasNext();) {
					entry = iterator.next();
					s = entry.getValue().textValue();
					if (!Util.isBlank(s)) {
						strings.add(s);
					}
				}
			}
			StringBuilder sb = new StringBuilder();
			for (String string : strings) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(string);
			}
			return sb.toString();
		}
		return node.asText();
	}
}
