package gg.xp.xivdata.data;

public enum ActionCategory {
	Unknown(""),
	AutoAttack("Auto-attack"),
	Spell("Spell"),
	Weaponskill("Weaponskill"),
	Ability("Ability"),
	Item("Item"),
	DoLAbility("DoL Ability"),
	DoHAbility("DoH Ability"),
	Event("Event"),
	LimitBreak("Limit Break"),
	System("System"),
	System_11("System11"),
	Mount("Mount"),
	Special("Special"),
	ItemManipulation("Item Manipulation"),
	LimitBreak_15("Limit Break"),
	Unknown_16("Unknown16"),
	Artillery("Artillery"),
	Unknown_18("Unknown18");

	private final String name;

	ActionCategory(String name) {
		this.name = name;
	}

	public String getFriendlyName() {
		return name;
	}

	public static ActionCategory fromRaw(int id) {
		if (id < 0 || id >= values().length) {
			return Unknown;
		}
		return values()[id];
	}
}
