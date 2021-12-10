package gg.xp.xivsupport.events.actlines.events.abilityeffect;

import gg.xp.xivsupport.models.XivStatusEffect;

public class StatusAppliedEffect extends AbilityEffect {
	private final XivStatusEffect status;
	private final boolean onTarget;

	// TODO: target
	public StatusAppliedEffect(long id, boolean onTarget) {
		super(AbilityEffectType.APPLY_STATUS);
		this.status = new XivStatusEffect(id, "");
		this.onTarget = onTarget;
	}

	@Override
	public String toString() {
		if (isOnTarget()) {
			return String.format("S(%s)", status.getId());
		}
		else {
			return String.format("Sb(%s)", status.getId());
		}
	}

	public XivStatusEffect getStatus() {
		return status;
	}

	public boolean isOnTarget() {
		return onTarget;
	}
}
