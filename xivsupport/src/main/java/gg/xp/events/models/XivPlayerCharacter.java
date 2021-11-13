package gg.xp.events.models;

import gg.xp.events.actlines.data.Job;

public class XivPlayerCharacter extends XivCombatant {
	private static final long serialVersionUID = 8719229961190925919L;
	private final Job job;
	private final XivWorld world;
	private final int level;

	public XivPlayerCharacter(long id, String name, Job job, XivWorld world, int level, boolean isLocalPlayerCharacter) {
		super(id, name, true, isLocalPlayerCharacter);
		this.job = job;
		this.world = world;
		this.level = level;
	}

	public Job getJob() {
		return job;
	}

	public XivWorld getWorld() {
		return world;
	}

	public int getLevel() {
		return level;
	}

	@Override
	public String toString() {
		return String.format("XivPlayerCharacter(0x%X:%s, %s, %s, %s, %s)", getId(), getName(), getJob(), getWorld(), getLevel(), isThePlayer());
	}

}
