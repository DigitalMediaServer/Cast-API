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
package org.digitalmediaserver.cast.message.entity;

import javax.annotation.concurrent.Immutable;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The namespace of the a cast application.
 */
@Immutable
public class Namespace {

	/** The name of this namespace */
	@JsonProperty
	protected final String name;

	/**
	 * Creates a new instance using the specified name.
	 *
	 * @param name the name of the namespace.
	 */
	public Namespace(@JsonProperty("name") String name) {
		this.name = name;
	}

	/**
	 * @return The name of the namespace.
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [");
		if (name != null) {
			builder.append("name=").append(name);
		}
		builder.append("]");
		return builder.toString();
	}
}
