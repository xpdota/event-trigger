package gg.xp.xivsupport.gui.tables.timedisplay;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum TimeDisplayOption implements HasFriendlyName {

	LOCAL_TIME("Local Time"),
	RELATIVE_TO_SELECTION("Relative to Selection");

	private final String friendlyName;

	TimeDisplayOption(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
