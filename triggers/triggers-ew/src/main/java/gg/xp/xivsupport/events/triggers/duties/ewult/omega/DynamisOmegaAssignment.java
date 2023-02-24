package gg.xp.xivsupport.events.triggers.duties.ewult.omega;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum DynamisOmegaAssignment implements HasFriendlyName {

	NearWorld("Near World"),
	DistantWorld("Distant World"),
	Baiter1("Baiter #1"),
	Baiter2("Baiter #2"),
	Baiter3("Baiter #3"),
	Baiter4("Baiter #4"),
	Remaining1("Monitor/Tether #1"),
	Remaining2("Monitor/Tether #2");

	private final String friendlyName;

	DynamisOmegaAssignment(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
