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
package org.digitalmediaserver.cast.message.response;

import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.digitalmediaserver.cast.event.CastEvent.CastEventType;
import org.digitalmediaserver.cast.message.enumeration.DetailedErrorCode;
import org.digitalmediaserver.cast.message.enumeration.ErrorReason;
import org.digitalmediaserver.cast.message.enumeration.ErrorType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * An error response from the cast device.
 *
 * @author Nadahar
 */
@Immutable
public class ErrorResponse extends StandardResponse {

	/** Application specific data */
	@Nullable
	protected final Map<String, Object> customData;

	/** The {@link DetailedErrorCode} */
	@Nullable
	protected final DetailedErrorCode detailedErrorCode;

	/** The item ID */
	@Nullable
	protected final Integer itemId;

	/** The {@link ErrorReason} */
	@Nullable
	protected final ErrorReason reason;

	/** The {@link ErrorType} */
	@Nullable
	protected final ErrorType type;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param customData the application specific data.
	 * @param detailedErrorCode the {@link DetailedErrorCode}.
	 * @param itemId the item ID.
	 * @param reason the {@link ErrorReason}.
	 * @param requestId the request ID.
	 * @param type the {@link ErrorType}.
	 */
	public ErrorResponse(
		@JsonProperty("customData") @Nullable Map<String, Object> customData,
		@JsonProperty("detailedErrorCode") @Nullable DetailedErrorCode detailedErrorCode,
		@JsonProperty("itemId") @Nullable Integer itemId,
		@JsonProperty("reason") @Nullable ErrorReason reason,
		@JsonProperty("requestId") long requestId,
		@JsonProperty("responseType") @Nullable ErrorType type
	) {
		super(requestId);
		this.customData = customData;
		this.detailedErrorCode = detailedErrorCode;
		this.itemId = itemId;
		this.reason = reason;
		this.type = type;
	}

	/**
	 * @return The application specific data or {@code null}.
	 */
	@Nullable
	public Map<String, Object> getCustomData() {
		return customData;
	}

	/**
	 * @return The {@link DetailedErrorCode} or {@code null}.
	 */
	@Nullable
	public DetailedErrorCode getDetailedErrorCode() {
		return detailedErrorCode;
	}

	/**
	 * @return The item ID or {@code null}.
	 */
	@Nullable
	public Integer getItemId() {
		return itemId;
	}

	/**
	 * @return The {@link ErrorReason} or {@code null}.
	 */
	@Nullable
	public ErrorReason getReason() {
		return reason;
	}

	/**
	 * @return The {@link ErrorType} or {@code null}.
	 */
	@Nullable
	public ErrorType getType() {
		return type;
	}

	@JsonIgnore
	@Override
	public CastEventType getEventType() {
		return CastEventType.ERROR_RESPONSE;
	}

	@Override
	public int hashCode() {
		return Objects.hash(requestId, customData, detailedErrorCode, itemId, reason, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ErrorResponse)) {
			return false;
		}
		ErrorResponse other = (ErrorResponse) obj;
		return
			Objects.equals(requestId, other.requestId) &&
			Objects.equals(customData, other.customData) &&
			Objects.equals(detailedErrorCode, other.detailedErrorCode) &&
			Objects.equals(itemId, other.itemId) &&
			reason == other.reason &&
			type == other.type;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ErrorResponse [");
		if (customData != null) {
			builder.append("customData=").append(customData).append(", ");
		}
		if (detailedErrorCode != null) {
			builder.append("detailedErrorCode=").append(detailedErrorCode).append(", ");
		}
		if (itemId != null) {
			builder.append("itemId=").append(itemId).append(", ");
		}
		if (reason != null) {
			builder.append("reason=").append(reason).append(", ");
		}
		if (type != null) {
			builder.append("type=").append(type).append(", ");
		}
		builder.append("requestId=").append(requestId).append("]");
		return builder.toString();
	}
}
