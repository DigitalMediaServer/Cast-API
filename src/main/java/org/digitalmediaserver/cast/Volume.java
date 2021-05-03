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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import javax.annotation.concurrent.Immutable;

/**
 * Volume settings.
 */
@Immutable
public class Volume {

	protected static final Float DEFAULT_INCREMENT = 0.05f;
	protected static final String DEFAULT_CONTROL_TYPE = "attenuation";
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Float level;
	@JsonProperty
	protected final boolean muted;

	@JsonProperty
	protected final Float increment;
	@JsonProperty
	protected final Double stepInterval;
	@JsonProperty
	protected final String controlType;

	public Volume() {
		level = -1f;
		muted = false;
		increment = DEFAULT_INCREMENT;
		stepInterval = DEFAULT_INCREMENT.doubleValue();
		controlType = DEFAULT_CONTROL_TYPE;
	}

	public Volume(
		@JsonProperty("level") Float level,
		@JsonProperty("muted") boolean muted,
		@JsonProperty("increment") Float increment,
		@JsonProperty("stepInterval") Double stepInterval,
		@JsonProperty("controlType") String controlType
	) {
		this.level = level;
		this.muted = muted;
		if (increment != null && increment > 0f) {
			this.increment = increment;
		} else {
			this.increment = DEFAULT_INCREMENT;
		}
		if (stepInterval != null && stepInterval > 0d) {
			this.stepInterval = stepInterval;
		} else {
			this.stepInterval = DEFAULT_INCREMENT.doubleValue();
		}
		this.controlType = controlType;
	}

	public Float getLevel() {
		return level;
	}

	public boolean isMuted() {
		return muted;
	}

	public Float getIncrement() {
		return increment;
	}

	public Double getStepInterval() {
		return stepInterval;
	}

	public String getControlType() {
		return controlType;
	}

	@Override
	public final int hashCode() {
		return Arrays.hashCode(new Object[] {level, muted, increment, stepInterval, controlType});
	}

	@Override
	public final boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Volume)) {
			return false;
		}
		final Volume that = (Volume) obj;
		return
			this.level == null ?
				that.level == null :
				this.level.equals(that.level) &&
			this.muted == that.muted &&
			this.increment == null ?
				that.increment == null :
				this.increment.equals(that.increment) &&
			this.stepInterval == null ?
				that.stepInterval == null :
				this.stepInterval.equals(that.stepInterval) &&
			this.controlType == null ?
				that.controlType == null :
				this.controlType.equals(that.controlType);
	}

	@Override
	public final String toString() {
		return String.format(
			"Volume{level: %s, muted: %b, increment: %s, stepInterval: %s, controlType: %s}",
			level,
			muted,
			increment,
			stepInterval,
			controlType
		);
	}
}
