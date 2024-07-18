package gg.xp.xivdata.data.duties;

public enum Expansion {
	GENERAL("General"),
	ARR("A Realm Reborn"),
	HW("Heavensward"),
	SB("Stormblood"),
	ShB("Shadowbringers"),
	EW("Endwalker"),
	DT("Dawntrail");

	private final String name;

	Expansion(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
