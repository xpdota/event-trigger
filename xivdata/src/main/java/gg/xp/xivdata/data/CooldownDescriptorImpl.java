package gg.xp.xivdata.data;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public class CooldownDescriptorImpl implements ExtendedCooldownDescriptor {

	public CooldownDescriptorImpl(CdBuilder builder) {
		jobType = builder.getJobType();
		job = builder.getJob();
		abilityIds = builder.abilityIds;
		if (abilityIds.length == 0) {
			throw new IllegalArgumentException("Ability ID list cannot be empty");
		}
		defaultPersOverlay = builder.defaultPersOverlay;
		cooldown = builder.getCooldown();
		type = builder.type;
		label = builder.getName();
		maxCharges = builder.getMaxCharges();
		durationOverride = builder.durationOverride;
		if (job == null && jobType == null) {
			throw new IllegalArgumentException(String.format("Cooldown %s has neither a job nor jobtype", label));
		}
		if (builder.autoBuffs && builder.buffIds.length > 0) {
			throw new IllegalArgumentException(String.format("Cooldown %s specified both autoBuffs and explicit buff IDs (%s)", label, Arrays.toString(builder.buffIds)));
		}
		buffIds = builder.buffIds;
		autoBuffs = builder.autoBuffs;
	}

	@Override
	public @Nullable Job getJob() {
		return job;
	}

	@Override
	public @Nullable JobType getJobType() {
		return jobType;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public boolean abilityIdMatches(long abilityId) {
		for (long id : abilityIds) {
			if (id == abilityId) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean buffIdMatches(long buffId) {
		for (long thisBuffId : this.buffIds) {
			if (thisBuffId == buffId) {
				return true;
			}
		}
		return false;
	}

	@Override
	public double getCooldown() {
		return cooldown;
	}

	// Purposefully saying "primary" here - as some might require multiple CDs (see: Raw/Nascent)
	@Override
	public long getPrimaryAbilityId() {
		return abilityIds[0];
	}

	@Override
	public int getMaxCharges() {
		return maxCharges;
	}

	@Override
	public @Nullable Double getDurationOverride() {
		return durationOverride;
	}

	@Override
	public boolean autoBuffs() {
		return autoBuffs;
	}

	@Override
	public boolean defaultPersOverlay() {
		return this.defaultPersOverlay;
	}

	@Override
	public String getSettingKeyStub() {
		return "Custom_" + getPrimaryAbilityId();
	}

	@Override
	public boolean noStatusEffect() {
		return !autoBuffs && buffIds.length == 0;
	}

	private final JobType jobType;
	private final Job job;
	private final double cooldown;
	private final CooldownType type;
	private final String label;
	private final long[] abilityIds;
	private final long[] buffIds;
	private final boolean autoBuffs;
	private final int maxCharges;
	private final boolean defaultPersOverlay;
	private final @Nullable Double durationOverride;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CooldownDescriptorImpl that = (CooldownDescriptorImpl) o;
		return Double.compare(that.getPrimaryAbilityId(), getPrimaryAbilityId()) == 0;
	}

	@Override
	public int hashCode() {
		return (int) getPrimaryAbilityId();
	}
}
