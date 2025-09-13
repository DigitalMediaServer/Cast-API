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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;


/**
 * A cast device status.
 */
@Immutable
public class ReceiverStatus {

	/** The receiver {@link Volume} instance */
	@Nullable
	protected final Volume volume;

	/** The {@link List} of {@link Application}s */
	@Nonnull
	protected final List<Application> applications;

	/**
	 * {@code true} if the cast device is the active input, {@code false}
	 * otherwise
	 */
	protected final boolean activeInput;

	/**
	 * {@code true} if the cast device is in standby, {@code false} otherwise
	 */
	protected final boolean standBy;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param volume the {@link Volume} instance.
	 * @param applications the {@link List} of {@link Application}s.
	 * @param activeInput {@code true} if the cast device is the active input,
	 *            {@code false} otherwise.
	 * @param standBy {@code true} if the cast device is in standby,
	 *            {@code false} otherwise.
	 */
	public ReceiverStatus(
		@JsonProperty("volume") @Nullable Volume volume,
		@JsonProperty("applications") @Nullable List<Application> applications,
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

	/**
	 * @return The {@link Volume} instance.
	 */
	@Nullable
	public Volume getVolume() {
		return volume;
	}

	/**
	 * @return The {@link List} of {@link Application}s.
	 */
	@Nonnull
	public List<Application> getApplications() {
		return applications;
	}

	/**
	 * @return {@code true} if the cast device is the active input,
	 *         {@code false} otherwise.
	 */
	public boolean isActiveInput() {
		return activeInput;
	}

	/**
	 * @return {@code true} if the cast device is in standby, {@code false}
	 *         otherwise.
	 */
	public boolean isStandBy() {
		return standBy;
	}

	/**
	 * @return The {@link Application} instance of the "running" application.
	 */
	@Nullable
	@JsonIgnore
	public Application getRunningApplication() {
		return applications.isEmpty() ? null : applications.get(0);
	}

	/**
	 * Checks if the application with the specified application ID currently is
	 * the "running" application.
	 *
	 * @param applicationId the application ID.
	 * @return {@code true} if the application with the specified application ID
	 *         is currently running, {@code false} otherwise.
	 */
	public boolean isApplicationRunning(String applicationId) {
		Application runningApplication = getRunningApplication();
		return runningApplication != null && runningApplication.getAppId().equals(applicationId);
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
