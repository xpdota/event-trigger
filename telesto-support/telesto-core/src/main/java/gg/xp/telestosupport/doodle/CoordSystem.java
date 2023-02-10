package gg.xp.telestosupport.doodle;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CoordSystem {
	@JsonProperty("screen")
	Screen("screen"),
	@JsonProperty("world")
	World("world");

	private final String serialized;

	CoordSystem(String serialized) {
		this.serialized = serialized;
	}

	public String getSerializedForm() {
		return serialized;
	}
}
