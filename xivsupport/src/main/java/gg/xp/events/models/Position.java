package gg.xp.events.models;

import java.io.Serializable;

public class Position implements Serializable {
	private static final long serialVersionUID = -5539446233772501577L;
	private final double x;
	private final double y;
	private final double z;
	private final double heading;

	public Position(double x, double y, double z, double heading) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.heading = heading;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}

	public double getHeading() {
		return heading;
	}

	@Override
	public String toString() {
		return String.format("Pos(%.2s, %.2s, %.2s : %.2s)", x, y, z, heading);
	}
}
