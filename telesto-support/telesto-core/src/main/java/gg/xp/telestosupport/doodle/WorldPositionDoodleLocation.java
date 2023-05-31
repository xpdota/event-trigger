package gg.xp.telestosupport.doodle;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.xp.xivsupport.models.Position;

import java.io.Serial;
import java.util.Map;

public class WorldPositionDoodleLocation extends DoodleLocation {

	@Serial
	private static final long serialVersionUID = 6284749148392942853L;
	@JsonIgnore
	public final Position position;

	public WorldPositionDoodleLocation(Position position) {
		this.position = position;
	}

	@JsonAnyGetter
	public Map<String, Object> flattenPosition() {
		// Uses the in-game style rather than ACT-style
		return Map.of("x", position.x(), "z", position.y(), "y", position.z());
	}

	@Override
	public String coordsType() {
		return "world";
	}
}
