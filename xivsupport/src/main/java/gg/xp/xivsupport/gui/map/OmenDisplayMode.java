package gg.xp.xivsupport.gui.map;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum OmenDisplayMode implements HasFriendlyName {
	NONE("None"),
	ENEMIES_ONLY("Enemies Only"),
	SELECTED_ONLY("Selection Only"),
	ALL("All");

	private final String friendlyName;

	OmenDisplayMode(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
