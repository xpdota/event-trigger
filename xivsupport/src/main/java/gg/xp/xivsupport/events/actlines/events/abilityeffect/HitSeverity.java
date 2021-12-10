package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public enum HitSeverity {
	NORMAL(""),
	CRIT("!"),
	DHIT("*"),
	CRIT_DHIT("!!");

	private final String symbol;

	HitSeverity(String symbol) {
		this.symbol = symbol;
	}

	public String getSymbol() {
		return this.symbol;
	}
}
