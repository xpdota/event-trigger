package gg.xp.xivsupport.models;

import gg.xp.xivsupport.gui.util.HasFriendlyName;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public enum ArenaSector implements HasFriendlyName {


	NORTH("North", "N"),
	NORTHEAST("Northeast", "NE"),
	EAST("East", "E"),
	SOUTHEAST("Southeast", "SE"),
	SOUTH("South", "S"),
	SOUTHWEST("Southwest", "SW"),
	WEST("West", "W"),
	NORTHWEST("Northwest", "NW"),
	/**
	 * Represents the center of the arena rather than a direction.
	 */
	CENTER("Center", "Mid"),
	/**
	 * Represents an unknown sector.
	 */
	UNKNOWN("?", "?");

	private static final Logger log = LoggerFactory.getLogger(ArenaSector.class);

	private final String friendlyName;
	private final String abbreviation;

	ArenaSector(String friendlyName, String abbreviation) {
		this.friendlyName = friendlyName;
		this.abbreviation = abbreviation;
	}

	/**
	 * @return The full name for this sector, e.g. "Northwest" or "East"
	 */
	@Override
	public String getFriendlyName() {
		return friendlyName;
	}

	/**
	 * @return The abbreviation for this sector, e.g. "NW" or "E"
	 */
	public String getAbbreviation() {
		return abbreviation;
	}

	/**
	 * The list of cardinal directions.
	 */
	public static final List<ArenaSector> cardinals = List.of(NORTH, EAST, SOUTH, WEST);
	/**
	 * The the list of intercardinal directions (or quadrants, if you prefer).
	 */
	public static final List<ArenaSector> quadrants = List.of(NORTHEAST, SOUTHEAST, SOUTHWEST, NORTHWEST);
	/**
	 * Intercardinals and cardinals. This only contains the 'real' directions (no 'center' or 'unknown'), so it is
	 * preferable to use this instead of {@link #values()}
	 */
	public static final List<ArenaSector> all = List.of(NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST);

	/**
	 * Given two intercardinal quadrants, check if there is a cardinal adjacent to both of them.
	 * <p>
	 * r e.g. NE, NW == north; NE, SE == south; NE, SW == null
	 * <p>
	 * null will also be returned if the input is invalid, e.g. if the list size was not 2,
	 * or if one or more instance was not an intercard.
	 *
	 * @param quadrants The two quadrants
	 * @return The adjacent cardinal, or null they are opposites, or if the input is invalid.
	 */
	public static @Nullable ArenaSector tryCombineTwoQuadrants(List<ArenaSector> quadrants) {
		if (quadrants.size() != 2) {
			log.warn("Expected two quadrants, but got: {}", quadrants);
			return null;
		}
		quadrants = new ArrayList<>(quadrants);
		quadrants.sort(Comparator.naturalOrder());
		ArenaSector firstQuadrant = quadrants.get(0);
		ArenaSector secondQuadrant = quadrants.get(1);
		return switch (firstQuadrant) {
			case NORTHEAST -> switch (secondQuadrant) {
				case SOUTHEAST -> EAST;
				case NORTHWEST -> NORTH;
				default -> null;
			};
			case SOUTHEAST -> switch (secondQuadrant) {
				case SOUTHWEST -> SOUTH;
				default -> null;
			};
			case SOUTHWEST -> switch (secondQuadrant) {
				case NORTHWEST -> WEST;
				default -> null;
			};
			default -> null;
		};
	}

	/**
	 * Given two cardinals, check if there is an intercardinal adjacent to both of them.
	 * <p>
	 * r e.g. W, N == NW; S, E == SE; W, E == null
	 * <p>
	 * null will also be returned if the input is invalid, e.g. if the list size was not 2,
	 * or if one or more instance was not a cardinal.
	 *
	 * @param cardinals The two cardinals
	 * @return The adjacent intercardinal, or null they are opposites, or if the input is invalid.
	 */
	public static @Nullable ArenaSector tryCombineTwoCardinals(List<ArenaSector> cardinals) {
		if (cardinals.size() != 2) {
			log.warn("Expected two cardinals, but got: {}", cardinals);
			return null;
		}
		cardinals = new ArrayList<>(cardinals);
		cardinals.sort(Comparator.naturalOrder());
		ArenaSector firstQuadrant = cardinals.get(0);
		ArenaSector secondQuadrant = cardinals.get(1);
		return switch (firstQuadrant) {
			case NORTH -> switch (secondQuadrant) {
				case WEST -> NORTHWEST;
				case EAST -> NORTHEAST;
				default -> null;
			};
			case EAST -> switch (secondQuadrant) {
				case SOUTH -> SOUTHEAST;
				default -> null;
			};
			case SOUTH -> switch (secondQuadrant) {
				case WEST -> SOUTHWEST;
				default -> null;
			};
			default -> null;
		};
	}

	/**
	 * Like {@link #tryCombineTwoQuadrants(List)} (List)}, but returns a list. If they were combined, the list will
	 * contain the single combined instance. Otherwise, returns the original input.
	 *
	 * @param quadrants The quadrants to combine.
	 * @return The original input if no combination possible, otherwise the combination.
	 */
	public static List<ArenaSector> tryMergeQuadrants(List<ArenaSector> quadrants) {
		ArenaSector combined = tryCombineTwoQuadrants(quadrants);
		if (combined == null) {
			return quadrants;
		}
		else {
			return Collections.singletonList(combined);
		}
	}

	/**
	 * @return The direct opposite of this arena area. If 'this' is center or unknown, returns 'this' unmodified.
	 */
	public ArenaSector opposite() {
		if (this == CENTER || this == UNKNOWN) {
			return this;
		}
		return values()[(ordinal() + 4) % 8];
	}

	/**
	 * Add the given number of eighth-turns (clockwise) to this sector and return the result.
	 * <p>
	 * e.g. NORTHEAST.plusEights(2) == SOUTHEAST and NORTHEAST.plusEights(-3) == WEST
	 * <p>
	 * To rotate counterclockwise, supply a negative value.
	 * <p>
	 * If 'this' is center/unknown, returns this unmodified.
	 *
	 * @param eighths Eigth-turns. Positive is clockwise, negative is CCW.
	 * @return The result.
	 */
	public ArenaSector plusEighths(int eighths) {
		if (this == CENTER || this == UNKNOWN) {
			return this;
		}
		if (eighths < 0) {
			eighths += 8;
		}
		return values()[(ordinal() + eighths) % 8];
	}

	/**
	 * Like {@link #plusEighths(int)}, but quarter-turns rather than eights.
	 * <p>
	 * e.g. NORTHEAST.plusQuads(1) == SOUTHEAST, NORTH.plusQuads(-1) == WEST;
	 *
	 * @param quads The number of quarter turns. Positive is clockwise, negative is CCW.
	 * @return The result.
	 */
	public ArenaSector plusQuads(int quads) {
		return plusEighths(quads * 2);
	}

	/**
	 * Computes the rotation from one sector to another, expressed in eight-turns. Positive indicates clockwise
	 * while negative indicates CCW.
	 * <p>
	 * e.g. NORTHEAST.eightsTo(WEST) == -3, because the shortest path from northeast to west is CCW.
	 * <p>
	 * If the locations being compared are directly opposite (e.g. this==EAST and other==WEST), returns 4 (never -4).
	 *
	 * @param other The sector to compare to.
	 * @return The eighth-turns to get to that sector from this.
	 * @throws IllegalArgumentException if 'this' or 'other' is center or unknown.
	 */
	public int eighthsTo(ArenaSector other) {
		if (this == CENTER || this == UNKNOWN) {
			throw new IllegalArgumentException(this.toString());
		}
		if (other == CENTER || other == UNKNOWN) {
			throw new IllegalArgumentException(other.toString());
		}
		int rawDiff = other.ordinal() - this.ordinal();
		// We want it to represent the "shortest" path.
		// That is, rather than NE (1) to NW (7) being a difference of 6, or -6 in reverse, we want to clamp it to (-4, 4].
		// To do this, simply add or subtract 8 if it falls outside of that range.
		// 4 will always be positive
		if (rawDiff > 4) {
			return rawDiff - 8;
		}
		if (rawDiff <= -4) {
			return rawDiff + 8;
		}
		return rawDiff;
	}

	/**
	 * @param other A sector to compare to
	 * @return true if and only if this sector is exactly one eight-turn away from 'other'. If they are the same, it
	 * still returns false.
	 * @throws IllegalArgumentException if 'this' or 'other' is center or unknown.
	 */
	public boolean isStrictlyAdjacentTo(ArenaSector other) {
		return Math.abs(eighthsTo(other)) == 1;
	}

	/**
	 * @return true if and only if this is an 'outside' direction, i.e. not 'center' or 'unknown'
	 */
	public boolean isOutside() {
		return ordinal() <= 7;
	}

	/**
	 * @return true if and only if this is a cardinal.
	 */
	public boolean isCardinal() {
		int ordinal = ordinal();
		return ordinal <= 7 && ordinal % 2 == 0;
	}


	/**
	 * @return true if and only if this is an intercardinal.
	 */
	public boolean isIntercard() {
		int ordinal = ordinal();
		return ordinal <= 7 && ordinal % 2 == 1;
	}

	/**
	 * @return The facing angle for this position
	 * @throws IllegalArgumentException if this position is not a cardinal or intercard
	 */
	public double facingAngle() {
		if (!isOutside()) {
			throw new IllegalArgumentException("Can only call facingAngle() on compass directions, but got " + this);
		}
		return Math.PI - (ordinal() * Math.PI / 4);
	}

	/**
	 * Sort starting north and going CCW
	 */
	public static final Comparator<ArenaSector> northCcwSort = Comparator.comparing(sector -> {
		// Ordinal is north==0, NE==1, etc, i.e. the opposite of what we want
		// By doing this, we flip the order (north = 8, NE = 7, NW = 1), and then %8 to get north back to 0
		return (8 - sector.ordinal()) % 8;
	});
}
