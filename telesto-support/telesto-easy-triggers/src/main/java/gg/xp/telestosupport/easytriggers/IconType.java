package gg.xp.telestosupport.easytriggers;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum IconType implements HasFriendlyName {

	IconId("Raw Icon ID"),
	AbilityId("Specific Ability"),
	StatusId("Specific Status"),
	AbilityAuto("Ability that Triggered This"),
	StatusAuto("Status that Triggered This");

	private final String friendlyName;

	IconType(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
