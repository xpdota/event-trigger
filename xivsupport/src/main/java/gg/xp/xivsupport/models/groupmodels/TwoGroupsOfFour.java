package gg.xp.xivsupport.models.groupmodels;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum TwoGroupsOfFour implements HasFriendlyName {

	GROUP1_NUM1("Group 1 Number 1", 1, 1),
	GROUP1_NUM2("Group 1 Number 2", 1, 2),
	GROUP1_NUM3("Group 1 Number 3", 1, 3),
	GROUP1_NUM4("Group 1 Number 4", 1, 4),
	GROUP2_NUM1("Group 2 Number 1", 2, 1),
	GROUP2_NUM2("Group 2 Number 2", 2, 2),
	GROUP2_NUM3("Group 2 Number 3", 2, 3),
	GROUP2_NUM4("Group 2 Number 4", 2, 4),
	;

	private final String friendlyName;
	private final int group;
	private final int number;

	TwoGroupsOfFour(String friendlyName, int group, int number) {
		this.friendlyName = friendlyName;
		this.group = group;
		this.number = number;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}

	public int getGroup() {
		return group;
	}

	public int getNumber() {
		return number;
	}

	public TwoGroupsOfFour getCounterpart() {
		if (this.group == 1) {
			return values()[this.ordinal() + 4];
		}
		else {
			return values()[this.ordinal() - 4];
		}
	}

	public static TwoGroupsOfFour forNumbers(int groupNum, int num) {
		return values()[(groupNum - 1) * 4 + (num - 1)];
	}
}
