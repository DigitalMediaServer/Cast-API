/*
 * Copyright (C) 2021 Digital Media Server developers.
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

import java.io.IOException;
import java.util.Iterator;
import org.digitalmediaserver.chromecast.api.StandardResponse.MediaStatusResponse;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;


public class MediaStatusResponseDeserializer extends StdDeserializer<MediaStatusResponse> {

	private static final long serialVersionUID = 1L;
	public MediaStatusResponseDeserializer() {
		super((Class<?>) null);
	}
	public MediaStatusResponseDeserializer(Class<?> vc) {
		super(vc);
	}

	public MediaStatusResponseDeserializer(JavaType valueType) {
		super(valueType);
	}

	public MediaStatusResponseDeserializer(StdDeserializer<?> src) {
		super(src);
	}
	@Override
	public MediaStatusResponse deserialize(
		JsonParser parser,
		DeserializationContext ctxt
	) throws IOException, JsonProcessingException {
		JsonNode node = parser.getCodec().readTree(parser);
		JsonNode tmpNode = node.get("requestId");
		long requestId = tmpNode == null ? -1L : tmpNode.asLong(-1L);
		tmpNode = node.get("status");
		if (tmpNode == null) {
			MediaStatus status = parser.getCodec().treeToValue(node, MediaStatus.class);
			return status == null ? new MediaStatusResponse(requestId) : new MediaStatusResponse(requestId, status);
		} else {
			JsonNode mediaStatusNode;
			MediaStatus[] statuses;
			MediaStatus status;
			if (tmpNode.isArray()) {
				statuses = new MediaStatus[tmpNode.size()];
				int i = 0;
				for (Iterator<JsonNode> iterator = tmpNode.elements(); iterator.hasNext();) {
					mediaStatusNode = iterator.next();
					status = parser.getCodec().treeToValue(mediaStatusNode, MediaStatus.class);
					statuses[i++] = status;
				}
			} else {
				status = parser.getCodec().treeToValue(tmpNode, MediaStatus.class);
				if (status == null) {
					statuses = new MediaStatus[0];
				} else {
					statuses = new MediaStatus[1];
					statuses[0] = status;
				}
			}
			return new MediaStatusResponse(requestId, statuses);
		}
	}
}
