package gg.xp.xivsupport.events.actlines.data;

public enum JobType {
	// Try to not use this directly - rather, query the isX methods
	UNKNOWN,
	DOH,
	DOL,
	TANK,
	HEALER,
	MELEE_DPS,
	CASTER,
	PRANGED;

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
