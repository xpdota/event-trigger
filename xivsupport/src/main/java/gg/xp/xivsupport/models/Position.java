package gg.xp.xivsupport.models;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public record Position(double x, double y, double z, double heading) implements Serializable {
	@Serial
	private static final long serialVersionUID = -5539446233772501577L;

	// TODO: these aren't really needed with record
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
		return String.format("Pos(%.2f, %.2f, %.2f : %.2f)", x, y, z, heading);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Position position = (Position) o;
		return Double.compare(position.x, x) == 0 && Double.compare(position.y, y) == 0 && Double.compare(position.z, z) == 0 && Double.compare(position.heading, heading) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y, z, heading);
	}
}
