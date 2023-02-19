package gg.xp.xivsupport.events.triggers.duties.ewult.omega;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum DynamisSigmaAssignment implements HasFriendlyName {

	NearWorld("Near World"),
	DistantWorld("Distant World"),
	OneStack1("One Stack #1"),
	OneStack2("One Stack #2"),
	OneStack3("One Stack #3"),
	OneStack4("One Stack #4"),
	Remaining1("Leftover #1"),
	Remaining2("Leftover #2");

	private final String friendlyName;

	DynamisSigmaAssignment(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
