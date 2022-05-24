package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivCombatant;

import java.util.List;

public class StatusEffectList extends BaseEvent implements HasTargetEntity {

	private final XivCombatant target;
	private final List<BuffApplied> statusEffects;

	public StatusEffectList(XivCombatant target, List<BuffApplied> statusEffects) {
		this.target = target;
		this.statusEffects = statusEffects;
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}
}
