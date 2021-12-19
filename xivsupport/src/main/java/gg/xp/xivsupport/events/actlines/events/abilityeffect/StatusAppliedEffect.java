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
			return String.format("S(0x%x)", status.getId());
		}
		else {
			return String.format("Sb(0x%x)", status.getId());
		}
	}

	public XivStatusEffect getStatus() {
		return status;
	}

	public boolean isOnTarget() {
		return onTarget;
	}

	@Override
	public String getDescription() {
		return String.format("Applied Status 0x%x to %s", status.getId(), onTarget ? "Target" : "Caster");
	}
}
