package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.DamageTakenEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.HealEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.HitSeverity;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

public class TickEvent extends BaseEvent implements HasTargetEntity, HasEffects {

	@Serial
	private static final long serialVersionUID = -78631681579667812L;
	private final XivCombatant combatant;
	private final TickType type;
	private final long damage;
	private final long rawEffectId;

	public TickEvent(XivCombatant combatant, TickType type, long damageOrHeal, long rawEffectId) {
		this.combatant = combatant;
		this.type = type;
		this.damage = damageOrHeal;
		this.rawEffectId = rawEffectId;
	}

	@Override
	public XivCombatant getTarget() {
		return combatant;
	}

	public XivCombatant getCombatant() {
		return combatant;
	}

	public TickType getType() {
		return type;
	}

	public long getDamage() {
		return damage;
	}

	public long getRawEffectId() {
		return rawEffectId;
	}

	@Override
	public List<AbilityEffect> getEffects() {
		if (type == TickType.HOT) {
			return Collections.singletonList(new HealEffect(0, 0, HitSeverity.NORMAL, damage));
		}
		else {
			return Collections.singletonList(new DamageTakenEffect(0, 0, HitSeverity.NORMAL, damage));
		}
	}
}
