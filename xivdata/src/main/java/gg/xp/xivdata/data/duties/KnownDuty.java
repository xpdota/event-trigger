package gg.xp.xivdata.data.duties;

public enum KnownDuty {
	Dragonsong("Dragonsong", Expansion.EW, DutyType.ULTIMATE),
	P1S("P1S", Expansion.EW, DutyType.SAVAGE_RAID),
	P2S("P2S", Expansion.EW, DutyType.SAVAGE_RAID),
	P3S("P3S", Expansion.EW, DutyType.SAVAGE_RAID),
	P4S("P4S", Expansion.EW, DutyType.SAVAGE_RAID);

	private final String name;
	private final Expansion expac;
	private final DutyType type;

	KnownDuty(String name, Expansion expac, DutyType type) {
		this.name = name;
		this.expac = expac;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public Expansion getExpac() {
		return expac;
	}

	public DutyType getType() {
		return type;
	}
}
