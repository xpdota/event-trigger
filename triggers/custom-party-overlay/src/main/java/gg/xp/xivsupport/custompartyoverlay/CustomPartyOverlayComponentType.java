package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum CustomPartyOverlayComponentType implements HasFriendlyName {
	NOTHING("Dummy Component"),
	NAME("Name"),
	JOB("Job"),
	HP("HP/Shield Bar"),
	BUFFS("Buffs"),
	BUFFS_WITH_TIMERS("Buffs with Timers"),
	CAST_BAR("Cast Bar"),
	MP_BAR("MP Bar");

	private final String friendlyName;

	CustomPartyOverlayComponentType(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
