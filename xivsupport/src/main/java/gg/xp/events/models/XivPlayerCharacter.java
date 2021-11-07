package gg.xp.events.models;

public class XivPlayerCharacter extends XivEntity {
	private final XivJob job;
	private final XivWorld world;
	private final int level;

	public XivPlayerCharacter(long id, String name, XivJob job, XivWorld world, int level) {
		super(id, name);
		this.job = job;
		this.world = world;
		this.level = level;
	}

	public XivJob getJob() {
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
		return String.format("XivPlayerCharacter(0x%X:%s, %s, %s, %s)", getId(), getName(), getJob(), getWorld(), getLevel());
	}

}
