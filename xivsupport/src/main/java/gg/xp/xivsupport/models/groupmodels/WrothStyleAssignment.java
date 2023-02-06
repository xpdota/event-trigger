package gg.xp.xivsupport.models.groupmodels;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum WrothStyleAssignment implements HasFriendlyName {
	SPREAD_1("Spread 1"),
	SPREAD_2("Spread 2"),
	SPREAD_3("Spread 3"),
	SPREAD_4("Spread 4"),
	STACK_1("Stack 1"),
	STACK_2("Stack 2"),
	NOTHING_1("Nothing 1"),
	NOTHING_2("Nothing 2");

	private final String friendlyName;

	WrothStyleAssignment(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
