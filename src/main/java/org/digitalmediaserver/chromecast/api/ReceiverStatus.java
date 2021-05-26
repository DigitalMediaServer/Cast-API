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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Current ChromeCast device status.
 */
@Immutable
public class ReceiverStatus {

	private final Volume volume;

	@Nonnull
	private final List<Application> applications;
	private final boolean activeInput;
	private final boolean standBy;

	public ReceiverStatus(
		@JsonProperty("volume") Volume volume,
		@JsonProperty("applications") List<Application> applications,
		@JsonProperty("isActiveInput") boolean activeInput,
		@JsonProperty("isStandBy") boolean standBy
	) {
		this.volume = volume;
		this.applications = applications == null ?
			Collections.<Application>emptyList() :
			Collections.unmodifiableList(new ArrayList<>(applications));
		this.activeInput = activeInput;
		this.standBy = standBy;
	}

	public Volume getVolume() {
		return volume;
	}

	@Nonnull
	public List<Application> getApplications() {
		return applications;
	}

	public boolean isActiveInput() {
		return activeInput;
	}

	public boolean isStandBy() {
		return standBy;
	}

	@Nullable
	@JsonIgnore
	public Application getRunningApplication() {
		return applications.isEmpty() ? null : applications.get(0);
	}

	public boolean isApplicationRunning(String appId) {
		Application runningApplication = getRunningApplication();
		return runningApplication != null && runningApplication.getAppId().equals(appId);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [");
		if (volume != null) {
			builder.append("volume=").append(volume).append(", ");
		}
		if (applications != null) {
			builder.append("applications=").append(applications).append(", ");
		}
		builder.append("activeInput=").append(activeInput).append(", standBy=").append(standBy).append("]");
		return builder.toString();
	}
}
