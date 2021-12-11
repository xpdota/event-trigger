package gg.xp.xivsupport.events.actlines.events.abilityeffect;

import gg.xp.xivsupport.models.XivStatusEffect;

public class StatusNoEffect extends AbilityEffect {
	private final XivStatusEffect status;

	// TODO: target
	public StatusNoEffect(long id) {
		super(AbilityEffectType.STATUS_NO_EFFECT);
		this.status = new XivStatusEffect(id, "");
	}

	@Override
	public String toString() {
		return String.format("SNE(%s)", status.getId());
	}

	@Override
	public String getDescription() {
		return String.format("Status %x has no effect", status.getId());
	}

	public XivStatusEffect getStatus() {
		return status;
	}
}
