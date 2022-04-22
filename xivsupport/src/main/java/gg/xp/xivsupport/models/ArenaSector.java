package gg.xp.xivsupport.models;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public enum ArenaSector {


	NORTH("North"),
	NORTHEAST("Northeast"),
	EAST("East"),
	SOUTHEAST("Southeast"),
	SOUTH("South"),
	SOUTHWEST("Southwest"),
	WEST("West"),
	NORTHWEST("Northwest"),
	CENTER("Center"),
	UNKNOWN("?");

	private static final Logger log = LoggerFactory.getLogger(ArenaSector.class);

	private final String friendlyName;

	ArenaSector(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	public static final List<ArenaSector> cardinals = List.of(NORTH, EAST, SOUTH, WEST);
	public static final List<ArenaSector> quadrants = List.of(NORTHEAST, SOUTHEAST, SOUTHWEST, NORTHWEST);

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

	public ArenaSector opposite() {
		return values()[(ordinal() + 4) % 8];
	}

	public ArenaSector plusEighths(int eights) {
		if (this == CENTER || this == UNKNOWN) {
			return this;
		}
		if (eights < 0) {
			eights += 8;
		}
		return values()[(ordinal() + eights) % 8];
	}

	public ArenaSector plusQuads(int quads) {
		return plusEighths(quads * 2);
	}

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

	public boolean isStrictlyAdjacentTo(ArenaSector other) {
		return Math.abs(eighthsTo(other)) == 1;
	}

}
