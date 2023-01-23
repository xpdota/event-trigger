package gg.xp.telestosupport.easytriggers;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum TelestoPositionRefType implements HasFriendlyName {

	FOLLOW_ENTITY("Follow Entity"),
	SNAPSHOT_POSITION("Snapshot Position");

	private final String friendlyName;

	TelestoPositionRefType(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
