package gg.xp.xivsupport.models;

/**
 * Class to represent an arena, and divide it into cardinals and intercards.
 */
public class ArenaPos {
	private final double xCenter;
	private final double yCenter;
	private final double xDiff;
	private final double yDiff;

	/**
	 * Defines the geometry of the arena. This divides the arena into nine areas (cards, intercards, center).
	 * <p>
	 * This definition consists of a center point, and a delta for X and Y directions. If a combatant's position is
	 * within xCenter +/- xDiff, and yCenter +/- yDiff, they are considered to be in the center of the arena.
	 * <p>
	 * If they are outside the xDiff but inside yDiff, they are considered to be west or east. If they are outside
	 * yDiff but inside xDiff, they are considered to be north or south. If they are outside of both, they are
	 * considered to be in an intercard.
	 * <p>
	 * In other words, you can visualize the sectors by drawing vertical lines at xCenter - xDiff and xCenter + xDiff,
	 * and horizontal lines at yCenter - yDiff and yCenter + yDiff. These four lines will divide the arena into nine
	 * distinct areas.
	 *
	 * @param xCenter Center X value (usually 100)
	 * @param yCenter Center Y value (usually 100)
	 * @param xDiff   How far west/east away from center you can get before you're actually considered west/east.
	 * @param yDiff   How far north/south away from center you can get before you're actually considered north/south.
	 */
	public ArenaPos(double xCenter, double yCenter, double xDiff, double yDiff) {
		this.xCenter = xCenter;
		this.yCenter = yCenter;
		this.xDiff = xDiff;
		this.yDiff = yDiff;
	}

	/**
	 * @param cbt A combatant to compute the sector of. Must not be null.
	 * @return The arena sector in which the combatant lies, or null if the combatant's position is null.
	 */
	public ArenaSector forCombatant(XivCombatant cbt) {
		Position pos = cbt.getPos();
		if (pos == null) {
			return ArenaSector.UNKNOWN;
		}
		return forPosition(pos);
	}

	/**
	 * @param pos A position to compute the sector of. Must not be null.
	 * @return The arena sector in which the position lies.
	 */
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

	/**
	 * @param cbt A combatant to compute the facing angle of.
	 * @return The facing angle expressed as a card/intercard, or 'unknown' if the combatant's position is null.
	 */
	public static ArenaSector combatantFacing(XivCombatant cbt) {
		Position pos = cbt.getPos();
		return combatantFacing(pos);
	}

	public static ArenaSector combatantFacing(Position position) {
		if (position == null) {
			return ArenaSector.UNKNOWN;
		}
		return combatantFacing(position.getHeading());
	}

	/**
	 * @param heading a raw facing angle.
	 * @return The facing angle expressed as a card/intercard, or 'unknown' if the combatant's position is null.
	 */
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

	/**
	 * @param cbt A combatant.
	 * @return The distance from this combatant to the center of the arena.
	 * @throws IllegalArgumentException if the combatant's position is null.
	 */
	public double distanceFromCenter(XivCombatant cbt) {
		Position pos = cbt.getPos();
		if (pos == null) {
			throw new IllegalArgumentException("Position cannot be null");
		}
		return distanceFromCenter(pos);
	}

	/**
	 * @param pos A position
	 * @return The distance from this position to the center of the arena.
	 */
	public double distanceFromCenter(Position pos) {
		double posX = pos.getX();
		double posY = pos.getY();
		double deltaX = posX - xCenter;
		double deltaY = posY - yCenter;
		return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
	}


}
