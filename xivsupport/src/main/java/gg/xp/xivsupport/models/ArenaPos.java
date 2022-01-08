package gg.xp.xivsupport.models;

public class ArenaPos {
	private final double xCenter;
	private final double yCenter;
	private final double xDiff;
	private final double yDiff;

	public ArenaPos(double xCenter, double yCenter, double xDiff, double yDiff) {
		this.xCenter = xCenter;
		this.yCenter = yCenter;
		this.xDiff = xDiff;
		this.yDiff = yDiff;
	}

	public ArenaSector forCombatant(XivCombatant cbt) {
		Position pos = cbt.getPos();
		if (pos == null) {
			return ArenaSector.UNKNOWN;
		}
		return forPosition(pos);
	}

	public ArenaSector forPosition(Position pos) {
		double x = pos.getX();
		double y = pos.getY();
		if (x > xCenter + xDiff) {
			if (y > yCenter + yDiff) {
				return ArenaSector.SOUTHEAST;
			}
			else if (y < yCenter - yDiff) {
				return ArenaSector.NORTHEAST;
			}
			else {
				return ArenaSector.EAST;
			}
		}
		else if (x < xCenter - xDiff) {
			if (y > yCenter + yDiff) {
				return ArenaSector.SOUTHWEST;
			}
			else if (y < yCenter - yDiff) {
				return ArenaSector.NORTHWEST;
			}
			else {
				return ArenaSector.WEST;
			}
		}
		else {
			if (y > yCenter + yDiff) {
				return ArenaSector.SOUTH;
			}
			else if (y < yCenter - yDiff) {
				return ArenaSector.NORTH;
			}
			else {
				return ArenaSector.CENTER;
			}
		}

	}
}
