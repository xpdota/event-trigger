package gg.xp.telestosupport.easytriggers;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum TelestoLocationType implements HasFriendlyName {
	SOURCE("Event Source"),
	TARGET("Event Target"),
	PLAYER("The Player"),
	CUSTOM("Custom Expression");

	private final String friendlyName;

	TelestoLocationType(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
