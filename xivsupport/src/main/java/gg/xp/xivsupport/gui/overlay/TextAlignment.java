package gg.xp.xivsupport.gui.overlay;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum TextAlignment implements HasFriendlyName {
	LEFT("Left"),
	CENTER("Center"),
	RIGHT("Right");

	private final String friendlyName;

	TextAlignment(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
