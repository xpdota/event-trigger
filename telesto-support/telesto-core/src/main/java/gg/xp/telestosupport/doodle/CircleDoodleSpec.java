package gg.xp.telestosupport.doodle;

public class CircleDoodleSpec extends DoodleSpec {
	public final DoodleLocation position;
	public final double radius;
	public final boolean filled;
	public final CoordSystem system;

	public CircleDoodleSpec(DoodleLocation position, double radius, boolean filled) {
		this(position, radius, filled, CoordSystem.Screen);
	}

	public CircleDoodleSpec(DoodleLocation position, double radius, boolean filled, CoordSystem system) {
		this.position = position;
		this.radius = radius;
		this.filled = filled;
		this.system = system;
	}

	@Override
	public String type() {
		return "circle";
	}
}
