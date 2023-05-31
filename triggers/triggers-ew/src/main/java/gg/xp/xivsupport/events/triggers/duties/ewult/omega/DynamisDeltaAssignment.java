package gg.xp.xivsupport.events.triggers.duties.ewult.omega;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum DynamisDeltaAssignment implements HasFriendlyName {

	NearWorld("Near World"),
	DistantWorld("Distant World");

	private final String friendlyName;

	DynamisDeltaAssignment(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
