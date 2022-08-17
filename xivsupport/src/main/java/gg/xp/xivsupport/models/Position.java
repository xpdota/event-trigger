package gg.xp.xivsupport.models;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public record Position(double x, double y, double z, double heading) implements Serializable {
	@Serial
	private static final long serialVersionUID = -5539446233772501577L;

	// TODO: these aren't really needed with record

	/**
	 * @return The X coordinate (-x is West, +x is East).
	 */
	public double getX() {
		return x;
	}

	/**
	 * @return The Y coordinate (-y is North, +y is South).
	 */
	public double getY() {
		return y;
	}

	/**
	 * @return The Z coordinate
	 */
	public double getZ() {
		return z;
	}

	/**
	 * @return The heading in radians (+pi and -pi are north, 0 is south, -pi/2 is west, +pi/2 is east)
	 */
	public double getHeading() {
		return heading;
	}

	public static Position of2d(double x, double y) {
		return new Position(x, y, 0, 0);
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

	public double distanceFrom2D(Position other) {
		double deltaX = (this.x - other.x);
		double deltaY = (this.y - other.y);
		return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
	}

	public Position translateAbsolute(double deltaX, double deltaY) {
		return new Position(x + deltaX, y + deltaY, z, heading);
	}

	/**
	 * Performs a translation of this position, using its facing angle.
	 *
	 * @param right   How far right (based on its facing angle) to move the position.
	 * @param forward How far forward (based on its facing angle) to move the position.
	 * @return a new Position object that has been translated in the X-Y plane, with the same Z and heading.
	 */
	// TODO: unit tests for this
	public Position translateRelative(double right, double forward) {
		// Normalize heading to north = 0, + = clockwise to make math easier
		double effectiveHeading = northUpClockwiseHeading();
		double effectiveDeltaEast = right * StrictMath.cos(effectiveHeading) + forward * StrictMath.sin(effectiveHeading);
		double effectiveDeltaSouth = right * StrictMath.sin(effectiveHeading) - forward * StrictMath.cos(effectiveHeading);
		return new Position(x + effectiveDeltaEast, y + effectiveDeltaSouth, z, heading);
	}

	/**
	 * @return The heading, but instead oriented so that zero is north, and clockwise rotation is positive heading.
	 */
	public double northUpClockwiseHeading() {
		return heading * -1 + Math.PI;
	}

	public Position normalizedTo(Position basis) {
		double headingDelta = -1.0 * basis.northUpClockwiseHeading();
		double rawXdelta = x - basis.x;
		double rawYdelta = y - basis.y;
		double newX = rawXdelta * StrictMath.cos(headingDelta) - rawYdelta * StrictMath.sin(headingDelta);
		double newY = rawXdelta * StrictMath.sin(headingDelta) + rawYdelta * StrictMath.cos(headingDelta);
		double newAngle = heading - headingDelta + Math.PI * 2;
		while (newAngle > Math.PI) {
			newAngle -= 2 * Math.PI;
		}
		return new Position(newX, newY, z - basis.z, newAngle);
	}
}
