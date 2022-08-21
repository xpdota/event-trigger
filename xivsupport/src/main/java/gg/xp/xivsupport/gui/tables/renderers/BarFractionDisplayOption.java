package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum BarFractionDisplayOption implements HasFriendlyName {
	AUTO("Automatic based on width"),
	BOTH("Show current and max"),
	NUMERATOR("Show current only");

	private final String friendlyName;

	BarFractionDisplayOption(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
