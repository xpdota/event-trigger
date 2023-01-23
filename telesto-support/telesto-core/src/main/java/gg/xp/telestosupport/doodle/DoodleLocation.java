package gg.xp.telestosupport.doodle;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public abstract class DoodleLocation implements Serializable {
	@Serial
	private static final long serialVersionUID = -8355912851592848448L;

	@JsonProperty("coords")
	public abstract String coordsType();
}
