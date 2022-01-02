package gg.xp.xivsupport.events.misc.pulls;

public enum PullStatus {
	PRE_PULL("Pre-Pull"),
	COMBAT("Combat"),
	WIPED("Wiped"),
	LEFT_ZONE("Left Zone"),
	MANUAL_END("Manually Ended"),
	VICTORY("Victory!");

	private final String friendlyName;

	PullStatus(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	public String getFriendlyName() {
		return this.friendlyName;
	}
}
