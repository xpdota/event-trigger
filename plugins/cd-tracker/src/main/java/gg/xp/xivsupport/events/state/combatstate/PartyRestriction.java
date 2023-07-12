package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

public enum PartyRestriction implements HasFriendlyName {
	PARTY("Party Members"),
//	ALLIANCE("All Alliances"),
	EVERYONE("Everyone")
	;

	private final String friendlyName;

	PartyRestriction(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}
}
