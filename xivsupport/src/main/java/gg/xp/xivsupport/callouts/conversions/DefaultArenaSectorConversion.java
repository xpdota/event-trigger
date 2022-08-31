package gg.xp.xivsupport.callouts.conversions;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum DefaultArenaSectorConversion implements HasFriendlyName {
	FULL("Full ('Northwest')"),
	ABBREVIATION("Abbreviation ('NW')"),
	CUSTOM("Custom (Configure Below)");

	private final String friendlyName;

	DefaultArenaSectorConversion(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
