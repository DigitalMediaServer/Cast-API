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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * The volume of a device.
 */
@Immutable
public class Volume {

	/** The type of volume control that is available */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final VolumeControlType controlType;

	/**
	 * The current volume level as a value between {@code 0.0} and {@code 1.0}.
	 * {@code 1.0} is the maximum volume possible on the receiver or stream.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Double level;

	/** Whether the receiver is muted, independent of the volume level */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Boolean muted;

	/** The allowed steps for changing volume */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Double stepInterval;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param controlType the type of volume control that is available.
	 * @param level the current volume level as a value between {@code 0.0} and
	 *            {@code 1.0}.
	 * @param muted whether the receiver is muted, independent of the volume
	 *            level.
	 * @param stepInterval the allowed steps for changing volume.
	 */
	public Volume(
		@JsonProperty("controlType") @Nullable VolumeControlType controlType,
		@JsonProperty("level") @Nullable Double level,
		@JsonProperty("muted") @Nullable Boolean muted,
		@JsonProperty("stepInterval") @Nullable Double stepInterval
	) {
		this.controlType = controlType;
		this.level = level;
		this.muted = muted;
		this.stepInterval = stepInterval;
	}

	/**
	 * @return The type of volume control that is available.
	 */
	@Nullable
	public VolumeControlType getControlType() {
		return controlType;
	}

	/**
	 * @return The current volume level as a value between {@code 0.0} and
	 *         {@code 1.0}.
	 */
	@Nullable
	public Double getLevel() {
		return level;
	}

	/**
	 * @return Whether the receiver is muted, independent of the volume level.
	 */
	@Nullable
	public Boolean getMuted() {
		return muted;
	}

	/**
	 * @return The allowed steps for changing volume.
	 */
	@Nullable
	public Double getStepInterval() {
		return stepInterval;
	}

	/**
	 * Creates a new {@link VolumeBuilder} that is initialized with the values
	 * from this {@link Volume} instance, which can be modified and then used to
	 * create a new (immutable) {@link Volume} instance.
	 *
	 * @return The initialized {@link VolumeBuilder}.
	 */
	@Nonnull
	public VolumeBuilder modify() {
		return new VolumeBuilder(controlType, stepInterval).level(level).muted(muted);
	}

	@Override
	public int hashCode() {
		return Objects.hash(controlType, level, muted, stepInterval);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Volume)) {
			return false;
		}
		Volume other = (Volume) obj;
		return
			controlType == other.controlType &&
			Objects.equals(level, other.level) &&
			Objects.equals(muted, other.muted) &&
			Objects.equals(stepInterval, other.stepInterval);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		builder.append("controlType=").append(controlType).append(", ");
		if (level != null) {
			builder.append("level=").append(level).append(", ");
		}
		if (muted != null) {
			builder.append("muted=").append(muted).append(", ");
		}
		builder.append("stepInterval=").append(stepInterval);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * A builder class for building {@link Volume} instances.
	 *
	 * @author Nadahar
	 */
	public static class VolumeBuilder {

		/** The type of volume control that is available */
		@Nullable
		protected VolumeControlType controlType;

		/**
		 * The current volume level as a value between {@code 0.0} and {@code 1.0}.
		 * {@code 1.0} is the maximum volume possible on the receiver or stream.
		 */
		@Nullable
		protected Double level;

		/** Whether the receiver is muted, independent of the volume level */
		@Nullable
		protected Boolean muted;

		/** The allowed steps for changing volume */
		@Nullable
		protected Double stepInterval;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param controlType the type of volume control that is available.
		 * @param stepInterval the allowed steps for changing volume.
		 */
		public VolumeBuilder(@Nullable VolumeControlType controlType, @Nullable Double stepInterval) {
			this.controlType = controlType;
			this.stepInterval = stepInterval;
		}

		/**
		 * @return The type of volume control that is available.
		 */
		@Nullable
		public VolumeControlType controlType() {
			return controlType;
		}

		/**
		 * @return The current volume level as a value between {@code 0.0} and
		 *         {@code 1.0}.
		 */
		@Nullable
		public Double level() {
			return level;
		}

		/**
		 * Sets the current volume level as a value between {@code 0.0} and
		 * {@code 1.0}.
		 *
		 * @param level the volume level to set.
		 * @return This {@link VolumeBuilder}.
		 */
		@Nonnull
		public VolumeBuilder level(@Nullable Double level) {
			this.level = level;
			return this;
		}

		/**
		 * @return Whether the receiver is muted, independent of the volume
		 *         level.
		 */
		@Nullable
		public Boolean muted() {
			return muted;
		}

		/**
		 * Sets whether the receiver is muted, independent of the volume level.
		 *
		 * @param muted the mute state to set.
		 * @return This {@link VolumeBuilder}.
		 */
		@Nonnull
		public VolumeBuilder muted(@Nullable Boolean muted) {
			this.muted = muted;
			return this;
		}

		/**
		 * @return The allowed steps for changing volume.
		 */
		@Nullable
		public Double stepInterval() {
			return stepInterval;
		}

		/**
		 * Builds a new {@link Volume} instance based on the current values of
		 * this builder.
		 *
		 * @return The new {@link Volume} instance.
		 */
		public Volume build() {
			return new Volume(controlType, level, muted, stepInterval);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(getClass().getSimpleName()).append(" [");
			if (controlType != null) {
				builder.append("controlType=").append(controlType).append(", ");
			}
			if (level != null) {
				builder.append("level=").append(level).append(", ");
			}
			if (muted != null) {
				builder.append("muted=").append(muted).append(", ");
			}
			if (stepInterval != null) {
				builder.append("stepInterval=").append(stepInterval);
			}
			builder.append("]");
			return builder.toString();
		}
	}

	/**
	 * Describes types of volume control.
	 */
	public enum VolumeControlType {

		/** Cast device volume, can be changed */
		ATTENUATION,

		/** Cast device volume is fixed and cannot be changed */
		FIXED,

		/** Master system volume control, i.e. TV or Audio device, can be changed */
		MASTER;

		/**
		 * Parses the specified string and returns the corresponding
		 * {@link VolumeControlType}, or {@code null} if no match could be
		 * found.
		 *
		 * @param controlType the string to parse.
		 * @return The resulting {@link VolumeControlType} or {@code null}.
		 */
		@Nullable
		@JsonCreator
		public static VolumeControlType typeOf(String controlType) {
			if (Util.isBlank(controlType)) {
				return null;
			}
			String typeString = controlType.toUpperCase(Locale.ROOT);
			for (VolumeControlType type : values()) {
				if (typeString.equals(type.name())) {
					return type;
				}
			}
			return null;
		}
	}
}
