/*
 * Copyright 2015 Vitaly Litvak (vitavaque@gmail.com)
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.cast.message.enumeration.TrackSubType;
import org.digitalmediaserver.cast.message.enumeration.TrackType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Track meta data information.
 *
 * @see <a href=
 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.Track">
 *      https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.Track</a>
 */
@Immutable
public class Track {

	/** The "caption MIME-type" for CEA 608 */
	public static final String CAPTION_MIME_TYPE_CEA608 = "CEA608";

	/** The "caption MIME-type" for TTML */
	public static final String CAPTION_MIME_TYPE_TTML = "TTML";

	/** The "caption MIME-type" for VTT */
	public static final String CAPTION_MIME_TYPE_VTT = "VTT";

	/** The "caption MIME-type" for TTML MP4 */
	public static final String CAPTION_MIME_TYPE_TTML_MP4 = "TTML_MP4";

	/** Custom data set by the receiver application */
	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	protected final Map<String, Object> customData;

	/**
	 * Indicate track is in-band and not side-loaded track. Relevant only for
	 * text tracks.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final Boolean isInband;

	/**
	 * Language tag as per RFC 5646 (If {@code subtype} is
	 * {@link TrackSubType#SUBTITLES} it is mandatory)
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final String language;

	/** A descriptive, human-readable name for the track. For example "Spanish" */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final String name;

	/**
	 * The role(s) of the track. The following values for each media type are
	 * recognized, with value explanations described in ISO/IEC 23009-1, labeled
	 * "DASH role scheme":
	 *
	 * <ul>
	 * <li>VIDEO: caption, subtitle, main, alternate, supplementary, sign,
	 * emergency</li>
	 * <li>AUDIO: main, alternate, supplementary, commentary, dub,
	 * emergency</li>
	 * <li>TEXT: main, alternate, subtitle, supplementary, commentary, dub,
	 * description, forced_subtitle</li>
	 * </ul>
	 */
	@Nonnull
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	protected final List<String> roles;

	/** For text tracks, the type of text track */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final TrackSubType subtype;

	/**
	 * It can be the URL of the track or any other identifier that allows the
	 * receiver to find the content (when the track is not inband or included in
	 * the manifest). For example it can be the URL of a VTT file.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final String trackContentId;

	/**
	 * It represents the MIME-type of the track content. For example if the
	 * track is a VTT file it will be 'text/vtt'. This field is needed for out
	 * of band tracks, so it is usually provided if a {@code trackContentId} has
	 * also been provided. It is not mandatory if the receiver has a way to
	 * identify the content from the {@code trackContentId}, but recommended.
	 * The track content type, if provided, must be consistent with the track
	 * type.
	 * <p>
	 * In addition to valid regular MIME-types, the constants starting with
	 * {@code CAPTION_MIME_TYPE} can be used.
	 */
	@Nullable
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	protected final String trackContentType;

	/**
	 * Unique identifier of the track within the context of a
	 * {@code MediaInformation} object
	 */
	@JsonProperty
	protected final int trackId;

	/** The type of track */
	@Nonnull
	@JsonProperty
	protected final TrackType type;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param customData the custom data set by the receiver application.
	 * @param isInband if {@code true} the track is in-band. If {@code false}
	 *            the track is side-loaded. Relevant only for text tracks.
	 * @param language the language tag as per RFC 5646 (If {@code subtype} is
	 *            "SUBTITLES" it is mandatory).
	 * @param name a descriptive, human-readable name for the track. For example
	 *            "Spanish".
	 * @param roles the role(s) of the track, as explained in ISO/IEC 23009-1,
	 *            labeled "DASH role scheme". See {@link #roles} for more
	 *            information.
	 * @param subtype the type of text track, for text tracks only.
	 * @param trackContentId the content ID. It can be the URL of the track or
	 *            any other identifier that allows the receiver to find the
	 *            content (when the track is not inband or included in the
	 *            manifest). For example it can be the URL of a VTT file.
	 * @param trackContentType the content type, represented by the MIME-type of
	 *            the track content. For example if the track is a VTT file it
	 *            will be 'text/vtt'. This field is needed for out of band
	 *            tracks, so it is usually provided if a {@code trackContentId}
	 *            has also been provided. It is not mandatory if the receiver
	 *            has a way to identify the content from the
	 *            {@code trackContentId}, but recommended. The track content
	 *            type, if provided, must be consistent with the track type.
	 *            <p>
	 *            In addition to valid regular MIME-types, the constants
	 *            starting with {@code CAPTION_MIME_TYPE} can be used.
	 * @param trackId the unique identifier of the track within the context of a
	 *            {@code MediaInformation} object.
	 * @param type the type of track.
	 */
	public Track(
		@JsonProperty("customData") Map<String, Object> customData,
		@JsonProperty("isInband") Boolean isInband,
		@JsonProperty("language") String language,
		@JsonProperty("name") String name,
		@JsonProperty("roles") List<String> roles,
		@JsonProperty("subtype") TrackSubType subtype,
		@JsonProperty("trackContentId") String trackContentId,
		@JsonProperty("trackContentType") String trackContentType,
		@JsonProperty("trackId") int trackId,
		@Nonnull @JsonProperty("trackType") TrackType type
	) {
		if (customData == null || customData.isEmpty()) {
			this.customData = Collections.emptyMap();
		} else {
			this.customData = Collections.unmodifiableMap(new LinkedHashMap<>(customData));
		}
		this.isInband = isInband;
		this.language = language;
		this.name = name;
		if (roles == null || roles.isEmpty()) {
			this.roles = Collections.emptyList();
		} else {
			this.roles = Collections.unmodifiableList(new ArrayList<>(roles));
		}
		this.subtype = subtype;
		this.trackContentId = trackContentId;
		this.trackContentType = trackContentType;
		this.trackId = trackId;
		this.type = type;
	}

	/**
	 * @return The custom data set by the receiver application.
	 */
	@Nonnull
	public Map<String, Object> getCustomData() {
		return customData;
	}

	/**
	 * @return {@code true} if the track is in-band. {@code false} if the track
	 *         is side-loaded. Relevant only for text tracks.
	 */
	@Nullable
	public Boolean getIsInband() {
		return isInband;
	}

	/**
	 * @return The language tag as per RFC 5646 (If {@code subtype} is
	 *         "SUBTITLES" it is mandatory).
	 */
	@Nullable
	public String getLanguage() {
		return language;
	}

	/**
	 * @return The descriptive, human-readable name for the track. For example
	 *         "Spanish".
	 */
	@Nullable
	public String getName() {
		return name;
	}

	/**
	 * Return the role(s) of the track. The following values for each media type
	 * are recognized, with value explanations described in ISO/IEC 23009-1,
	 * labeled "DASH role scheme":
	 *
	 * <ul>
	 * <li>VIDEO: caption, subtitle, main, alternate, supplementary, sign,
	 * emergency</li>
	 * <li>AUDIO: main, alternate, supplementary, commentary, dub,
	 * emergency</li>
	 * <li>TEXT: main, alternate, subtitle, supplementary, commentary, dub,
	 * description, forced_subtitle</li>
	 * </ul>
	 *
	 * @return The roles of the track.
	 */
	@Nonnull
	public List<String> getRoles() {
		return roles;
	}

	/**
	 * @return The type of text track. For text tracks only.
	 */
	@Nullable
	public TrackSubType getSubtype() {
		return subtype;
	}

	/**
	 * @return The track content ID. It can be the URL of the track or any other
	 *         identifier that allows the receiver to find the content (when the
	 *         track is not inband or included in the manifest). For example it
	 *         can be the URL of a VTT file.
	 */
	@Nullable
	public String getTrackContentId() {
		return trackContentId;
	}

	/**
	 * @return The track content type in the form of a MIME-type. For example if
	 *         the track is a VTT file it will be 'text/vtt'. This field is
	 *         needed for out of band tracks, so it is usually provided if a
	 *         {@code trackContentId} has also been provided. It is not
	 *         mandatory if the receiver has a way to identify the content from
	 *         the {@code trackContentId}, but recommended. The track content
	 *         type, if provided, must be consistent with the track type.
	 *         <p>
	 *         In addition to valid regular MIME-types, the constants starting
	 *         with {@code CAPTION_MIME_TYPE} can be used.
	 */
	@Nullable
	public String getTrackContentType() {
		return trackContentType;
	}

	/**
	 * @return The unique identifier of the track within the context of a
	 *         {@code MediaInformation} object.
	 */
	public int getTrackId() {
		return trackId;
	}

	/**
	 * @return The type of track.
	 */
	@Nonnull
	public TrackType getType() {
		return type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			customData, isInband, language, name, roles, subtype, trackContentId, trackContentType, trackId, type
		);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Track)) {
			return false;
		}
		Track other = (Track) obj;
		return Objects.equals(
			customData, other.customData) && Objects.equals(isInband, other.isInband) &&
			Objects.equals(language, other.language) && Objects.equals(name, other.name) &&
			Objects.equals(roles, other.roles) && Objects.equals(subtype, other.subtype) &&
			Objects.equals(trackContentId, other.trackContentId) &&
			Objects.equals(trackContentType, other.trackContentType) && trackId == other.trackId &&
			type == other.type;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append(" [");
		if (customData != null) {
			builder.append("customData=").append(customData).append(", ");
		}
		if (isInband != null) {
			builder.append("isInband=").append(isInband).append(", ");
		}
		if (language != null) {
			builder.append("language=").append(language).append(", ");
		}
		if (name != null) {
			builder.append("name=").append(name).append(", ");
		}
		if (roles != null) {
			builder.append("roles=").append(roles).append(", ");
		}
		if (subtype != null) {
			builder.append("subtype=").append(subtype).append(", ");
		}
		if (trackContentId != null) {
			builder.append("trackContentId=").append(trackContentId).append(", ");
		}
		if (trackContentType != null) {
			builder.append("trackContentType=").append(trackContentType).append(", ");
		}
		builder.append("trackId=").append(trackId).append(", ");
		if (type != null) {
			builder.append("type=").append(type);
		}
		builder.append("]");
		return builder.toString();
	}
}
