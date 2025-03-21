package gg.xp.xivsupport.gui.map;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum NameDisplayMode implements HasFriendlyName {
	FULL("Full Names"),
	JOB("Player Jobs"),
	HIDE("Hide Player Names");

	private final String friendlyName;

	NameDisplayMode(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
