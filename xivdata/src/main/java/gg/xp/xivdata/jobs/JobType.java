package gg.xp.xivdata.jobs;

public enum JobType {
	// Try to not use this directly - rather, query the isX methods
	UNKNOWN("Unknown"),
	DOH("Disciple of Hand"),
	DOL("Disciple of Land"),
	TANK("Tank"),
	HEALER("Healer"),
	MELEE_DPS("Melee DPS"),
	CASTER("Caster"),
	PRANGED("Phys Ranged");

	private final String friendlyName;

	JobType(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	public boolean isCombatJob() {
		return this != DOH && this != DOL && this != UNKNOWN;
	}

	public boolean isCrafter() {
		return this == DOH;
	}

	public boolean isGatherer() {
		return this == DOL;
	}

	public boolean isTank() {
		return this == TANK;
	}

	public boolean isHealer() {
		return this == HEALER;
	}

	public boolean isDps() {
		return this == CASTER || this == PRANGED || this == MELEE_DPS;
	}

	public boolean isCaster() {
		return this == CASTER;
	}

	public boolean isPranged() {
		return this == PRANGED;
	}

	public boolean isMeleeDps() {
		return this == MELEE_DPS;
	}
}
