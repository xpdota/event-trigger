package gg.xp.xivsupport.events.triggers.marks;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum AutoMarkLanguage implements HasFriendlyName {
	Automatic("Automatic"),
	EN("English/French"),
	DE("German"),
	JP("Japanese/Korean");

	private final String friendlyName;

	AutoMarkLanguage(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
