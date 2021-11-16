package gg.xp.xivsupport.events.models;

import gg.xp.xivsupport.events.actlines.data.Job;

public class XivPlayerCharacter extends XivCombatant {
	private static final long serialVersionUID = 8719229961190925919L;
	private final Job job;
	private final XivWorld world;

	public XivPlayerCharacter(long id,
	                          String name,
	                          Job job,
	                          XivWorld world,
	                          boolean isLocalPlayerCharacter,
	                          long typeRaw,
	                          HitPoints hp,
	                          Position pos,
	                          long bNpcId,
	                          long bNpcNameId,
	                          long partyType,
	                          long level,
	                          long ownerId
	) {
		super(id, name, true, isLocalPlayerCharacter, typeRaw, hp, pos, bNpcId, bNpcNameId, partyType, level, ownerId);
		this.job = job;
		this.world = world;
	}

	public Job getJob() {
		return job;
	}

	public XivWorld getWorld() {
		return world;
	}

	@Override
	public String toString() {
		return String.format("XivPlayerCharacter(0x%X:%s, %s, %s, %s, %s)", getId(), getName(), getJob(), getWorld(), getLevel(), isThePlayer());
	}

}
