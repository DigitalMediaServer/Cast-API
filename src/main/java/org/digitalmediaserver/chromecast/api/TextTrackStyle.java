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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Describes style information for a text track.
 */
@Immutable
public class TextTrackStyle {

	/**
	 * The background 32 bit RGBA color. The alpha channel should be used for
	 * transparent backgrounds.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String backgroundColor;

	/** Custom data set by the receiver application */
	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private final Map<String, Object> customData;

	/**
	 * RGBA color for the edge, this value will be ignored if {@code edgeType}
	 * is {@link TextTrackEdgeType#NONE}
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String edgeColor;

	/** The type of edge */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final TextTrackEdgeType edgeType;

	/**
	 * The font family. If the font is not available in the receiver the
	 * {@code fontGenericFamily} will be used
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String fontFamily;

	/** The text track generic font family */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final TextTrackFontGenericFamily fontGenericFamily;

	/** The font scaling factor for the text track (the default is 1) */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final Integer fontScale;

	/** The text track font style */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final TextTrackFontStyle fontStyle;

	/** The foreground 32 bit RGBA color */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String foregroundColor;

	/**
	 * 32 bit RGBA color for the window. This value will be ignored if
	 * {@code windowType} is {@link TextTrackWindowType#NONE}.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final String windowColor;

	/**
	 * Rounded corner radius absolute value in pixels (px). This value will be
	 * ignored if {@code windowType} is not
	 * {@link TextTrackWindowType#ROUNDED_CORNERS}.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final Integer windowRoundedCornerRadius;

	/**
	 * The window concept is defined in CEA-608 and CEA-708. In WebVTT it is
	 * called a region.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private final TextTrackWindowType windowType;

	/**
	 * Creates a new instance using the specific parameters.
	 *
	 * @param backgroundColor the background 32 bit RGBA color. The alpha
	 *            channel should be used for transparent backgrounds.
	 * @param customData the custom data set by the receiver application.
	 * @param edgeColor the RGBA color for the edge, this value will be ignored
	 *            if {@code edgeType} is {@link TextTrackEdgeType#NONE}.
	 * @param edgeType the type of edge.
	 * @param fontFamily the font family. If the font is not available in the
	 *            receiver the {@code fontGenericFamily} will be used.
	 * @param fontGenericFamily the text track generic font family.
	 * @param fontScale the font scaling factor for the text track (the default
	 *            is 1).
	 * @param fontStyle the text track font style.
	 * @param foregroundColor the foreground 32 bit RGBA color.
	 * @param windowColor the 32 bit RGBA color for the window. This value will
	 *            be ignored if {@code windowType} is
	 *            {@link TextTrackWindowType#NONE}.
	 * @param windowRoundedCornerRadius the rounded corner radius absolute value
	 *            in pixels (px). This value will be ignored if
	 *            {@code windowType} is not
	 *            {@link TextTrackWindowType#ROUNDED_CORNERS}.
	 * @param windowType the window type. The window concept is defined in
	 *            CEA-608 and CEA-708. In WebVTT it is called a region.
	 */
	public TextTrackStyle(
		@JsonProperty("backgroundColor") String backgroundColor,
		@JsonProperty("customData") Map<String, Object> customData,
		@JsonProperty("edgeColor") String edgeColor,
		@JsonProperty("edgeType") TextTrackEdgeType edgeType,
		@JsonProperty("fontFamily") String fontFamily,
		@JsonProperty("fontGenericFamily") TextTrackFontGenericFamily fontGenericFamily,
		@JsonProperty("fontScale") Integer fontScale,
		@JsonProperty("fontStyle") TextTrackFontStyle fontStyle,
		@JsonProperty("foregroundColor") String foregroundColor,
		@JsonProperty("windowColor") String windowColor,
		@JsonProperty("windowRoundedCornerRadius") Integer windowRoundedCornerRadius,
		@JsonProperty("windowType") TextTrackWindowType windowType
	) {
		this.backgroundColor = backgroundColor;
		if (customData == null || customData.isEmpty()) {
			this.customData = Collections.emptyMap();
		} else {
			this.customData = Collections.unmodifiableMap(new LinkedHashMap<>(customData));
		}
		this.edgeColor = edgeColor;
		this.edgeType = edgeType;
		this.fontFamily = fontFamily;
		this.fontGenericFamily = fontGenericFamily;
		this.fontScale = fontScale;
		this.fontStyle = fontStyle;
		this.foregroundColor = foregroundColor;
		this.windowColor = windowColor;
		this.windowRoundedCornerRadius = windowRoundedCornerRadius;
		this.windowType = windowType;
	}

	/**
	 * @return The background 32 bit RGBA color. The alpha channel should be
	 *         used for transparent backgrounds.
	 */
	@Nullable
	public String getBackgroundColor() {
		return backgroundColor;
	}

	/**
	 * @return The custom data set by the receiver application.
	 */
	@Nonnull
	public Map<String, Object> getCustomData() {
		return customData;
	}

	/**
	 * @return The RGBA color for the edge, this value will be ignored if
	 *         {@code edgeType} is {@link TextTrackEdgeType#NONE}.
	 */
	@Nullable
	public String getEdgeColor() {
		return edgeColor;
	}

	/**
	 * @return The The type of edge.
	 */
	@Nullable
	public TextTrackEdgeType getEdgeType() {
		return edgeType;
	}

	/**
	 * @return The text track font family. If the font is not available in the
	 *         receiver the {@code fontGenericFamily} will be used.
	 */
	@Nullable
	public String getFontFamily() {
		return fontFamily;
	}

	/**
	 * @return The The text track generic font family.
	 */
	@Nullable
	public TextTrackFontGenericFamily getFontGenericFamily() {
		return fontGenericFamily;
	}

	/**
	 * @return The The font scaling factor for the text track (the default is
	 *         1).
	 */
	@Nullable
	public Integer getFontScale() {
		return fontScale;
	}

	/**
	 * @return The text track font style.
	 */
	@Nullable
	public TextTrackFontStyle getFontStyle() {
		return fontStyle;
	}

	/**
	 * @return The foreground 32 bit RGBA color.
	 */
	@Nullable
	public String getForegroundColor() {
		return foregroundColor;
	}

	/**
	 * @return The 32 bit RGBA color for the window. This value will be ignored
	 *         if {@code windowType} is {@link TextTrackWindowType#NONE}.
	 */
	@Nullable
	public String getWindowColor() {
		return windowColor;
	}

	/**
	 * @return The Rounded corner radius absolute value in pixels (px). This
	 *         value will be ignored if {@code windowType} is not
	 *         {@link TextTrackWindowType#ROUNDED_CORNERS}.
	 */
	@Nullable
	public Integer getWindowRoundedCornerRadius() {
		return windowRoundedCornerRadius;
	}

	/**
	 * @return The window type. The window concept is defined in CEA-608 and
	 *         CEA-708. In WebVTT it is called a region.
	 */
	@Nullable
	public TextTrackWindowType getWindowType() {
		return windowType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			backgroundColor, customData, edgeColor, edgeType, fontFamily, fontGenericFamily, fontScale,
			fontStyle, foregroundColor, windowColor, windowRoundedCornerRadius, windowType
		);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TextTrackStyle)) {
			return false;
		}
		TextTrackStyle other = (TextTrackStyle) obj;
		return
			Objects.equals(backgroundColor, other.backgroundColor) && Objects.equals(customData, other.customData) &&
			Objects.equals(edgeColor, other.edgeColor) && edgeType == other.edgeType &&
			Objects.equals(fontFamily, other.fontFamily) && fontGenericFamily == other.fontGenericFamily &&
			Objects.equals(fontScale, other.fontScale) && fontStyle == other.fontStyle &&
			Objects.equals(foregroundColor, other.foregroundColor) && Objects.equals(windowColor, other.windowColor) &&
			Objects.equals(windowRoundedCornerRadius, other.windowRoundedCornerRadius) && windowType == other.windowType;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		if (backgroundColor != null) {
			builder.append("backgroundColor=").append(backgroundColor).append(", ");
		}
		if (customData != null) {
			builder.append("customData=").append(customData).append(", ");
		}
		if (edgeColor != null) {
			builder.append("edgeColor=").append(edgeColor).append(", ");
		}
		if (edgeType != null) {
			builder.append("edgeType=").append(edgeType).append(", ");
		}
		if (fontFamily != null) {
			builder.append("fontFamily=").append(fontFamily).append(", ");
		}
		if (fontGenericFamily != null) {
			builder.append("fontGenericFamily=").append(fontGenericFamily).append(", ");
		}
		if (fontScale != null) {
			builder.append("fontScale=").append(fontScale).append(", ");
		}
		if (fontStyle != null) {
			builder.append("fontStyle=").append(fontStyle).append(", ");
		}
		if (foregroundColor != null) {
			builder.append("foregroundColor=").append(foregroundColor).append(", ");
		}
		if (windowColor != null) {
			builder.append("windowColor=").append(windowColor).append(", ");
		}
		if (windowRoundedCornerRadius != null) {
			builder.append("windowRoundedCornerRadius=").append(windowRoundedCornerRadius).append(", ");
		}
		if (windowType != null) {
			builder.append("windowType=").append(windowType);
		}
		builder.append("]");
		return builder.toString();
	}



	/**
	 * Defines the text track edge type.
	 */
	public enum TextTrackEdgeType {

		/** No edge is displayed around text */
		NONE,

		/** Solid outline is displayed around text */
		OUTLINE,

		/** A fading shadow is casted around text */
		DROP_SHADOW,

		/** Text is embossed on background */
		RAISED,

		/** Text is debossed on background */
		DEPRESSED
	}

	/**
	 * Generic font family to be used if font not defined in text track.
	 */
	public enum TextTrackFontGenericFamily {

		/** Uses Sans Serif font */
		SANS_SERIF,

		/** Uses Sans Monospaced font */
		MONOSPACED_SANS_SERIF,

		/** Uses Serif font */
		SERIF,

		/** Uses Monospaced Serif font */
		MONOSPACED_SERIF,

		/** Uses Short Stack font */
		CASUAL,

		/** Uses Cursive font */
		CURSIVE,

		/** Uses Small Capitals font */
		SMALL_CAPITALS
	}

	/**
	 * Possible text track font style.
	 */
	public enum TextTrackFontStyle {

		/** Unmodified font */
		NORMAL,

		/** Bolded font */
		BOLD,

		/** Bolded and italicized font */
		BOLD_ITALIC,

		/** Italicized font */
		ITALIC
	}

	/**
	 * Text track window type.
	 */
	public enum TextTrackWindowType {

		/** No window type */
		NONE,

		/** Normal */
		NORMAL,

		/** Rounded corners */
		ROUNDED_CORNERS
	}
}
