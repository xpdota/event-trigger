package gg.xp.telestosupport.doodle;

public class CircleDoodleSpec extends DoodleSpec {
	public final DoodleLocation position;
	public final double radius;
	public final boolean filled;

	public CircleDoodleSpec(DoodleLocation position, double radius, boolean filled) {
		this.position = position;
		this.radius = radius;
		this.filled = filled;
	}

	@Override
	public String type() {
		return "circle";
	}
}
