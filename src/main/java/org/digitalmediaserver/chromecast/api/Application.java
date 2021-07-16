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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Application descriptor.
 */
@Immutable
public class Application {

	/** The application ID */
	@Nullable
	protected final String appId;

	/** The application display name */
	@Nullable
	protected final String displayName;

	/** The icon URL */
	@Nullable
	protected final String iconUrl;

	/** Whether this application is the idle-screen */
	@Nullable
	protected final Boolean isIdleScreen;

	/** Whether this application was launched from the cloud */
	@Nullable
	protected final Boolean launchedFromCloud;

	/** The {@link List} of {@link Namespace}s */
	@Nonnull
	protected final List<Namespace> namespaces;

	/** The session ID */
	@Nullable
	protected final String sessionId;

	/** The status text */
	@Nullable
	protected final String statusText;

	/** The transport ID */
	@Nullable
	protected final String transportId;

	/** The universal application ID */
	@Nullable
	protected final String universalAppId;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param appId the application ID.
	 * @param displayName the application display name.
	 * @param iconUrl the icon URL.
	 * @param isIdleScreen whether this application is the idle-screen.
	 * @param launchedFromCloud whether this application was launched from the
	 *            cloud.
	 * @param namespaces the {@link List} of {@link Namespace}s.
	 * @param sessionId the session ID.
	 * @param statusText the status text.
	 * @param transportId he transport ID.
	 * @param universalAppId the universal application ID.
	 */
	public Application(
		@JsonProperty("appId") @Nullable String appId,
		@JsonProperty("displayName") @Nullable String displayName,
		@JsonProperty("iconUrl") @Nullable String iconUrl,
		@JsonProperty("isIdleScreen") @Nullable Boolean isIdleScreen,
		@JsonProperty("launchedFromCloud") @Nullable Boolean launchedFromCloud,
		@JsonProperty("namespaces") @Nullable List<Namespace> namespaces,
		@JsonProperty("sessionId") @Nullable String sessionId,
		@JsonProperty("statusText") @Nullable String statusText,
		@JsonProperty("transportId") @Nullable String transportId,
		@JsonProperty("universalAppId") @Nullable String universalAppId
	) {
		this.appId = appId;
		this.displayName = displayName;
		this.iconUrl = iconUrl;
		this.isIdleScreen = isIdleScreen;
		this.launchedFromCloud = launchedFromCloud;
		this.namespaces = namespaces == null ?
			Collections.<Namespace>emptyList() :
			Collections.unmodifiableList(new ArrayList<>(namespaces));
		this.sessionId = sessionId;
		this.statusText = statusText;
		this.transportId = transportId;
		this.universalAppId = universalAppId;
	}

	/**
	 * @return The application ID.
	 */
	public String getAppId() {
		return appId;
	}

	/**
	 * @return The application display name.
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @return The icon URL.
	 */
	public String getIconUrl() {
		return iconUrl;
	}

	/**
	 * @return Whether this application is the idle-screen.
	 */
	@Nullable
	public Boolean getIsIdleScreen() {
		return isIdleScreen;
	}

	/**
	 * Convenience method where {@code null} returns {@code false}.
	 *
	 * @return Whether this application is the idle-screen.
	 */
	@JsonIgnore
	public boolean isIdleScreen() {
		return isIdleScreen != null && isIdleScreen.booleanValue();
	}

	/**
	 * @return Whether this application was launched from the cloud.
	 */
	@Nullable
	public Boolean getLaunchedFromCloud() {
		return launchedFromCloud;
	}

	/**
	 * Convenience method where {@code null} returns {@code false}.
	 *
	 * @return Whether this application was launched from the cloud.
	 */
	@JsonIgnore
	public boolean isLaunchedFromCloud() {
		return launchedFromCloud != null && launchedFromCloud.booleanValue();
	}

	/**
	 * @return The {@link List} of {@link Namespace}s.
	 */
	@Nonnull
	public List<Namespace> getNamespaces() {
		return namespaces;
	}

	/**
	 * @return The session ID.
	 */
	@Nullable
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * @return The status text.
	 */
	@Nullable
	public String getStatusText() {
		return statusText;
	}

	/**
	 * @return The transport ID.
	 */
	@Nullable
	public String getTransportId() {
		return transportId;
	}

	/**
	 * @return The universal application ID.
	 */
	@Nullable
	public String getUniversalAppId() {
		return universalAppId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			appId, displayName, iconUrl, isIdleScreen, launchedFromCloud, namespaces,
			sessionId, statusText, transportId, universalAppId
		);
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
		return Objects.equals(appId, other.appId) && Objects.equals(displayName, other.displayName) &&
			Objects.equals(iconUrl, other.iconUrl) && Objects.equals(isIdleScreen, other.isIdleScreen) &&
			launchedFromCloud == other.launchedFromCloud && Objects.equals(namespaces, other.namespaces) &&
			Objects.equals(sessionId, other.sessionId) && Objects.equals(statusText, other.statusText) &&
			Objects.equals(transportId, other.transportId) && Objects.equals(universalAppId, other.universalAppId);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		if (appId != null) {
			builder.append("appId=").append(appId).append(", ");
		}
		if (displayName != null) {
			builder.append("displayName=").append(displayName).append(", ");
		}
		if (iconUrl != null) {
			builder.append("iconUrl=").append(iconUrl).append(", ");
		}
		if (isIdleScreen != null) {
			builder.append("isIdleScreen=").append(isIdleScreen).append(", ");
		}
		builder.append("launchedFromCloud=").append(launchedFromCloud).append(", ");
		if (namespaces != null) {
			builder.append("namespaces=").append(namespaces).append(", ");
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
		if (universalAppId != null) {
			builder.append("universalAppId=").append(universalAppId);
		}
		builder.append("]");
		return builder.toString();
	}
}
