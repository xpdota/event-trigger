package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public interface BasicCooldownDescriptor {
	String getLabel();

	boolean abilityIdMatches(long abilityId);

	boolean buffIdMatches(long buffId);

	double getCooldown();

	default Duration getCooldownAsDuration() {
		return Duration.ofMillis((long) (getCooldown() * 1000L));
	}
	// Purposefully saying "primary" here - as some might require multiple CDs (see: Raw/Nascent)
	long getPrimaryAbilityId();

	int getMaxCharges();

	@Nullable Double getDurationOverride();

	default boolean autoBuffs() {
		return false;
	};

	boolean noStatusEffect();

	default List<CdAuxAbility> getAuxAbilities() {
		return Collections.emptyList();
	}

	default @Nullable CdAuxAbility auxMatch(long abilityId) {
		return null;
	};

}
