package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public enum HitSeverity {
	NORMAL("", "Normal"),
	CRIT("!", "Critical Hit"),
	DHIT("*", "Direct Hit"),
	CRIT_DHIT("!!", "Critical Direct Hit");

	private final String symbol;
	private final String friendlyName;

	HitSeverity(String symbol, String friendlyName) {
		this.symbol = symbol;
		this.friendlyName = friendlyName;
	}

	public static HitSeverity of(boolean chit, boolean dhit) {
		return dhit ? (chit ? CRIT_DHIT : DHIT) : (chit ? CRIT : NORMAL);
	}

	public String getSymbol() {
		return this.symbol;
	}

	public String getFriendlyName() {
		return friendlyName;
	}
}
