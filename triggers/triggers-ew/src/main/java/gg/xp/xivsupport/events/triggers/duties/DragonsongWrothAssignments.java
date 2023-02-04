package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum DragonsongWrothAssignments implements HasFriendlyName {
	Spread_1("Spread 1"),
	Spread_2("Spread 2"),
	Spread_3("Spread 3"),
	Spread_4("Spread 4"),
	Stack_Buff_1("Stack Buff 1"),
	Stack_Buff_2("Stack Buff 2"),
	Nothing_1("Nothing 1"),
	Nothing_2("Nothing 2");

	private final String friendlyName;

	DragonsongWrothAssignments(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
