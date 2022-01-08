package gg.xp.xivsupport.models;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

public enum ArenaSector {


	CENTER("Center"),
	NORTH("North"),
	NORTHEAST("Northeast"),
	EAST("East"),
	SOUTHEAST("Southeast"),
	SOUTH("South"),
	SOUTHWEST("Southwest"),
	WEST("West"),
	NORTHWEST("Northwest"),
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
}
