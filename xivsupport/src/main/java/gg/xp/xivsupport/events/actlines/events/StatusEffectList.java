package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.StatusAppliedEffect;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

public class StatusEffectList extends BaseEvent implements HasTargetEntity, HasEffects {

	@Serial
	private static final long serialVersionUID = -2055005771139926686L;
	private final XivCombatant target;
	private final List<BuffApplied> statusEffects;

	public StatusEffectList(XivCombatant target, List<BuffApplied> statusEffects) {
		this.target = target;
		this.statusEffects = statusEffects;
	}

	public List<BuffApplied> getStatusEffects() {
		return Collections.unmodifiableList(statusEffects);
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}


	@Override
	public List<AbilityEffect> getEffects() {
		return statusEffects.stream()
				.map(ba -> (AbilityEffect) new StatusAppliedEffect(0L, 0L, ba.getBuff().getId(), (int) ba.getRawStacks(), true))
				.toList();
	}
}
