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
package org.digitalmediaserver.cast.message.entity;

import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.cast.message.enumeration.ContentFilteringMode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Provides additional options for load requests.
 *
 * @author Nadahar
 */
@Immutable
public class LoadOptions {

	/** The content filtering mode to apply for which items to play */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final ContentFilteringMode contentFilteringMode;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param contentFilteringMode the content filtering mode to apply for which
	 *            items to play.
	 */
	public LoadOptions(
		@JsonProperty("contentFilteringMode") @Nullable ContentFilteringMode contentFilteringMode
	) {
		this.contentFilteringMode = contentFilteringMode;
	}

	/**
	 * @return The content filtering mode to apply for which items to play.
	 */
	@Nullable
	public ContentFilteringMode getContentFilteringMode() {
		return contentFilteringMode;
	}

	@Override
	public int hashCode() {
		return Objects.hash(contentFilteringMode);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof LoadOptions)) {
			return false;
		}
		LoadOptions other = (LoadOptions) obj;
		return Objects.equals(contentFilteringMode, other.contentFilteringMode);
	}

	@Override
	public String toString() {
		return new StringBuilder()
			.append(getClass().getSimpleName())
			.append(" [contentFilteringMode=").append(contentFilteringMode).append("]")
			.toString();
	}
}
