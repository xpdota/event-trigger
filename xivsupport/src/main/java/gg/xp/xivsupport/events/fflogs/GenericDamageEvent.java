package gg.xp.xivsupport.events.fflogs;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasEffects;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.DamageEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.DamageTakenEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.HitSeverity;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;

import java.util.Collections;
import java.util.List;

// Hack for fflogs support, don't depend on this sticking around
public class GenericDamageEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasAbility, HasEffects {

	private final XivCombatant source;
	private final XivCombatant target;
	private final XivAbility ability;
	private final long amount;
	private final HitSeverity severity;

	public GenericDamageEvent(XivCombatant source, XivCombatant target, XivAbility ability, long amount, HitSeverity severity) {
		this.source = source;
		this.target = target;
		this.ability = ability;
		this.amount = amount;
		this.severity = severity;
	}


	@Override
	public List<AbilityEffect> getEffects() {
		return Collections.singletonList(new DamageTakenEffect(0, 0, severity, amount));
	}

	@Override
	public XivCombatant getSource() {
		return source;
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}

	@Override
	public XivAbility getAbility() {
		return ability;
	}
}
