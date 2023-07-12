package gg.xp.xivsupport.custompartyoverlay.castbar;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum JobIconPlacement implements HasFriendlyName {
	NONE("None"),
	LEFT("Left"),
	RIGHT("Right");

	private final String friendlyName;

	JobIconPlacement(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
