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

	public static ArenaSector combatantFacing(XivCombatant cbt) {
		Position pos = cbt.getPos();
		if (pos == null) {
			return ArenaSector.UNKNOWN;
		}
		return combatantFacing(pos.getHeading());
	}

	public static ArenaSector combatantFacing(double heading) {
		// Zero = south, then negative = clockwise
		double pi = Math.PI;
		if (heading > pi * 7 / 8) {
			return ArenaSector.NORTH;
		}
		else if (heading > pi * 5 / 8) {
			return ArenaSector.NORTHEAST;
		}
		else if (heading > pi * 3 / 8) {
			return ArenaSector.EAST;
		}
		else if (heading > pi * 1 / 8) {
			return ArenaSector.SOUTHEAST;
		}
		else if (heading > pi * -1 / 8) {
			return ArenaSector.SOUTH;
		}
		else if (heading > pi * -3 / 8) {
			return ArenaSector.SOUTHWEST;
		}
		else if (heading > pi * -5 / 8) {
			return ArenaSector.WEST;
		}
		else if (heading > pi * -7 / 8) {
			return ArenaSector.NORTHWEST;
		}
		else {
			return ArenaSector.NORTH;
		}
	}

	// TWO DIMENSIONAL distance from center
	public double distanceFromCenter(XivCombatant cbt) {
		Position pos = cbt.getPos();
		if (pos == null) {
			throw new IllegalArgumentException("Position cannot be null");
		}
		return distanceFromCenter(pos);
	}

	public double distanceFromCenter(Position pos) {
		double posX = pos.getX();
		double posY = pos.getY();
		double deltaX = posX - xCenter;
		double deltaY = posY - yCenter;
		return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
	}


}
