package org.digitalmediaserver.chromecast.api;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents the type of virtual connections that can be made to cast devices.
 *
 * @author Nadahar
 */
// TODO: (Nad) Header
public enum VirtualConnectionType {

	/**
	 * The "normal" connection type, which is mandated when connecting to the
	 * "platform receiver"
	 */
	STRONG(0),

	/** Not in use */
	@Deprecated
	WEAK(1),

	/** Only allowed when connecting with applications */
	INVISIBLE(2);

	private final int value;

	private VirtualConnectionType(int value) {
		this.value = value;
	}

	/**
	 * @return The connection type code.
	 */
	@JsonValue
	public int getValue() {
		return value;
	}
}
