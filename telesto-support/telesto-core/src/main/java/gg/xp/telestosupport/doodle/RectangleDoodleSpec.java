package gg.xp.telestosupport.doodle;

import java.io.Serial;

public class RectangleDoodleSpec extends DoodleSpec {
	@Serial
	private static final long serialVersionUID = -6630423214658954367L;
	public final DoodleLocation pos1;
	public final DoodleLocation pos2;
	public final double thickness;
	public final boolean filled;
	public final CoordSystem system;

	public RectangleDoodleSpec(DoodleLocation pos1, DoodleLocation pos2, double thickness, boolean filled, CoordSystem system) {
		this.pos1 = pos1;
		this.pos2 = pos2;
		this.thickness = thickness;
		this.filled = filled;
		this.system = system;
	}

	@Override
	public String type() {
		return "rectangle";
	}
}
