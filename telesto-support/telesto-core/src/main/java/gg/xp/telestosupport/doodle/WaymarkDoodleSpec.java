package gg.xp.telestosupport.doodle;

import java.io.Serial;

public class WaymarkDoodleSpec extends DoodleSpec {
	@Serial
	private static final long serialVersionUID = -931021673974918666L;
	public final DoodleLocation position;

	public WaymarkDoodleSpec(DoodleLocation position) {
		this.position = position;
	}

	@Override
	public String type() {
		return "waymark";
	}
}
