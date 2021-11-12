package gg.xp.events.actlines.events;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivAbility;
import gg.xp.events.models.XivCombatant;

public class AbilityUsedEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity {

	private static final long serialVersionUID = -4539070760062288496L;
	private final XivAbility ability;
	private final XivCombatant caster;
	private final XivCombatant target;
	private final long damage;

	public AbilityUsedEvent(XivAbility ability, XivCombatant caster, XivCombatant target, long flags, long damage) {
		this.ability = ability;
		this.caster = caster;
		this.target = target;
		// flags and damage is TODO: it's a bitmask but has some weird shifting things going on
		this.damage = damage;
	}

	public XivAbility getAbility() {
		return ability;
	}

	@Override
	public XivCombatant getSource() {
		return caster;
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}
//
//	public long getDamage() {
//		return damage;
//	}
}
