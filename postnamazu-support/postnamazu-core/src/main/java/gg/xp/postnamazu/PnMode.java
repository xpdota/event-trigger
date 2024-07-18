package gg.xp.postnamazu;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum PnMode implements HasFriendlyName {
	/**
	 * Direct PN connection
	 */
	HTTP("HTTP Connection"),
	/**
	 * Use OverlayPlugin hook
	 */
	OP("Via OverlayPlugin (Easier)");

	private final String friendlyName;

	PnMode(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
