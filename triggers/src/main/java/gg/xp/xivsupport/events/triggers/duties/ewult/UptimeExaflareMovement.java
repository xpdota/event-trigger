package gg.xp.xivsupport.events.triggers.duties.ewult;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

import java.util.Arrays;
import java.util.List;

public enum UptimeExaflareMovement implements HasFriendlyName {
	SOUTH_PLANT("South Plant"),
	SOUTH_WEST("South, Dodge Southwest"),
	SOUTH_EAST("South, Dodge Southeast"),
	SOUTH_NORTH("South, Dodge North (Wide Safe Spot)"),
	NORTHWEST_PLANT("Northwest Plant"),
	NORTHEAST_PLANT("Northeast Plant"),
	SOUTH_NORTH_NARROW("South, Dodge North (Narrow Safe Spot)"),
	DOWNTIME("Downtime (South, Dodge South)");

	private final String friendlyName;

	UptimeExaflareMovement(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	public static List<UptimeExaflareMovement> defaultOrder() {
		return Arrays.asList(values());
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
