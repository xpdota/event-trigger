package gg.xp.xivsupport.models.groupmodels;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum TwoGroupsOfFour implements HasFriendlyName {

	GROUP1_NUM1("Group 1 Number 1"),
	GROUP1_NUM2("Group 1 Number 2"),
	GROUP1_NUM3("Group 1 Number 3"),
	GROUP1_NUM4("Group 1 Number 4"),
	GROUP2_NUM1("Group 2 Number 1"),
	GROUP2_NUM2("Group 2 Number 2"),
	GROUP2_NUM3("Group 2 Number 3"),
	GROUP2_NUM4("Group 2 Number 4"),
	;

	private final String friendlyName;

	TwoGroupsOfFour(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
