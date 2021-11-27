package gg.xp.xivdata.jobs;

public enum Cooldown {
	// List of ALL buffs to track - WL/BL will be done by user settings
	// JLS/javac being dumb, had to put the L there to make it a long
	FOO(JobType.TANK, 30.0d, "Foo Buff", CooldownType.DEFENSIVE, 0x123, 0x456);

	public enum CooldownType {
		DEFENSIVE,
		OFFENSIVE,
		PERSONAL
	}

	private final JobType jobType;
	private final Job job;
	private final double cooldown;
	private final CooldownType type;
	private final String label;
	private final long abilityId;
	private final long buffId;

	Cooldown(Job job, double cooldown, String label, CooldownType cooldownType, long abilityId, long buffId) {
		this.job = job;
		this.cooldown = cooldown;
		this.jobType = null;
		this.type = cooldownType;
		this.label = label;
		this.abilityId = abilityId;
		this.buffId = buffId;
	}

	Cooldown(JobType jobType, double cooldown, String label, CooldownType cooldownType, long abilityId, long buffId) {
		this.cooldown = cooldown;
		this.job = null;
		this.jobType = jobType;
		this.type = cooldownType;
		this.label = label;
		this.abilityId = abilityId;
		this.buffId = buffId;
	}

	public Job getJob() {
		return job;
	}

	public String getLabel() {
		return label;
	}
}
