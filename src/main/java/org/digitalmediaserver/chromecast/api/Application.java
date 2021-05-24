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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Application descriptor.
 */
@Immutable
public class Application {

	private final String appId;
	private final String iconUrl;
	private final String displayName;
	private final String sessionId;
	private final String statusText;
	private final String transportId;
	private final boolean isIdleScreen;
	private final boolean launchedFromCloud;

	@Nonnull
	private final List<Namespace> namespaces;

	public Application(
		@JsonProperty("appId") String appId,
		@JsonProperty("iconUrl") String iconUrl,
		@JsonProperty("displayName") String displayName,
		@JsonProperty("sessionId") String sessionId,
		@JsonProperty("statusText") String statusText,
		@JsonProperty("isIdleScreen") boolean isIdleScreen,
		@JsonProperty("launchedFromCloud") boolean launchedFromCloud,
		@JsonProperty("transportId") String transportId,
		@JsonProperty("namespaces") List<Namespace> namespaces
	) {
		this.appId = appId;
		this.iconUrl = iconUrl;
		this.displayName = displayName;
		this.sessionId = sessionId;
		this.statusText = statusText;
		this.transportId = transportId;
		this.namespaces = namespaces == null ?
			Collections.<Namespace>emptyList() :
			Collections.unmodifiableList(new ArrayList<>(namespaces));
		this.isIdleScreen = isIdleScreen;
		this.launchedFromCloud = launchedFromCloud;
	}

	public String getAppId() {
		return appId;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getStatusText() {
		return statusText;
	}

	public String getTransportId() {
		return transportId;
	}

	@Nonnull
	public List<Namespace> getNamespaces() {
		return namespaces;
	}

	@JsonProperty("isIdleScreen")
	public boolean isIdleScreen() {
		return isIdleScreen;
	}

	public boolean isLaunchedFromCloud() {
		return launchedFromCloud;
	}

	@Override
	public int hashCode() {
		return Objects.hash(appId, displayName, iconUrl, isIdleScreen, launchedFromCloud, namespaces, sessionId, statusText, transportId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Application)) {
			return false;
		}
		Application other = (Application) obj;
		return Objects.equals(appId, other.appId) && Objects.equals(displayName, other.displayName)
			&& Objects.equals(iconUrl, other.iconUrl) && isIdleScreen == other.isIdleScreen && launchedFromCloud == other.launchedFromCloud
			&& Objects.equals(namespaces, other.namespaces) && Objects.equals(sessionId, other.sessionId)
			&& Objects.equals(statusText, other.statusText) && Objects.equals(transportId, other.transportId);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [");
		if (appId != null) {
			builder.append("appId=").append(appId).append(", ");
		}
		if (iconUrl != null) {
			builder.append("iconUrl=").append(iconUrl).append(", ");
		}
		if (displayName != null) {
			builder.append("displayName=").append(displayName).append(", ");
		}
		if (sessionId != null) {
			builder.append("sessionId=").append(sessionId).append(", ");
		}
		if (statusText != null) {
			builder.append("statusText=").append(statusText).append(", ");
		}
		if (transportId != null) {
			builder.append("transportId=").append(transportId).append(", ");
		}
		builder.append("isIdleScreen=").append(isIdleScreen)
			.append(", launchedFromCloud=").append(launchedFromCloud);
		if (namespaces != null) {
			builder.append(", ").append("namespaces=").append(namespaces);
		}
		builder.append("]");
		return builder.toString();
	}
}
