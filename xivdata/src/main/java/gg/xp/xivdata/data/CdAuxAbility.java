package gg.xp.xivdata.data;

import java.time.Duration;

public class CdAuxAbility {
	private final long abilityId;
	private final double durationModifier;

	public CdAuxAbility(long abilityId, double durationModifier) {
		this.abilityId = abilityId;
		this.durationModifier = durationModifier;
	}

	public long getAbilityId() {
		return abilityId;
	}

	public double getDurationModifier() {
		return durationModifier;
	}

	public Duration getAsDuration() {
		return Duration.ofMillis((long) (durationModifier * 1000L));
	}
}
