package gg.xp.xivsupport.models.groupmodels;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum PsMarkerGroup implements HasFriendlyName {

	GROUP1_CIRCLE("Group 1 Circle", 1, 1),
	GROUP1_TRIANGLE("Group 1 Triangle", 1, 2),
	GROUP1_SQUARE("Group 1 Square", 1, 3),
	GROUP1_X("Group 1 X", 1, 4),
	GROUP2_CIRCLE("Group 2 Circle", 2, 1),
	GROUP2_TRIANGLE("Group 2 Triangle", 2, 2),
	GROUP2_SQUARE("Group 2 Square", 2, 3),
	GROUP2_X("Group 2 X", 2, 4),
	;

	private final String friendlyName;
	private final int group;
	private final int number;

	PsMarkerGroup(String friendlyName, int group, int number) {
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

	public PsMarkerGroup getCounterpart() {
		if (this.group == 1) {
			return values()[this.ordinal() + 4];
		}
		else {
			return values()[this.ordinal() - 4];
		}
	}

	public static PsMarkerGroup forNumbers(int groupNum, int num) {
		return values()[(groupNum - 1) * 4 + (num - 1)];
	}
}
