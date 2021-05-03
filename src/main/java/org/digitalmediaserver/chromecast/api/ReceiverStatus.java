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
import java.util.Arrays;
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

	protected final Volume volume;

	@Nonnull
	protected final List<Application> applications;
	protected final boolean activeInput;
	protected final boolean standBy;

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
	public Application getRunningApp() {
		return applications.isEmpty() ? null : applications.get(0);
	}

	public boolean isAppRunning(String appId) {
		Application runningApp = getRunningApp();
		return runningApp != null && runningApp.getAppId().equals(appId);
	}

	@Override
	public String toString() {
		return String.format(
			"Media{volume: %s, applications: %s, activeInput: %b, standBy; %b}",
			volume,
			Arrays.toString(applications.toArray()),
			activeInput,
			standBy
		);
	}
}
