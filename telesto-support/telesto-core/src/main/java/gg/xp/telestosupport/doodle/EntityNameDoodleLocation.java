package gg.xp.telestosupport.doodle;

import java.io.Serial;

public class EntityNameDoodleLocation extends DoodleLocation {

	@Serial
	private static final long serialVersionUID = 5098734525584022290L;
	public final String name;

	public EntityNameDoodleLocation(String name) {
		this.name = name;
	}

	@Override
	public String coordsType() {
		return "entity";
	}
}
