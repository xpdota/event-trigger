package gg.xp.xivdata.data.duties;

public enum DutyType {
	OTHER("Other"),
	DUNGEON("Dungeon"),
	TRIAL("Trial"),
	TRIAL_EX("Extreme Trial"),
	RAID("Normal Raid"),
	SAVAGE_RAID("Savage Raid"),
	ULTIMATE("Ultimate Raid"),
	HUNT("Hunt"),
	SOLO_INSTANCE("Solo Instance"),
	OPEN_WORLD("Eureka-like"),
	ALLIANCE_RAID("Alliance Raid"),
	CAR("Chaotic AR");

	private final String name;

	DutyType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
