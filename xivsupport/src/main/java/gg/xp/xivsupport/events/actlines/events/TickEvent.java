package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.DamageTakenEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.HealEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.HitSeverity;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivStatusEffect;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

public class TickEvent extends BaseEvent implements HasTargetEntity, HasEffects, HasStatusEffect {

	@Serial
	private static final long serialVersionUID = -78631681579667812L;
	private final XivCombatant combatant;
	private final TickType type;
	private final long damageOrHeal;
	private final long rawEffectId;
	private final XivStatusEffect statusEffect;

	public TickEvent(XivCombatant combatant, TickType type, long damageOrHeal, XivStatusEffect statusEffect) {
		this.combatant = combatant;
		this.type = type;
		this.damageOrHeal = damageOrHeal;
		this.rawEffectId = statusEffect.getId();
		this.statusEffect = statusEffect;
	}

	public TickEvent(XivCombatant combatant, TickType type, long damageOrHeal, long rawEffectId) {
		this.combatant = combatant;
		this.type = type;
		this.damageOrHeal = damageOrHeal;
		this.rawEffectId = rawEffectId;
		if (rawEffectId == 0) {
			this.statusEffect = new XivStatusEffect(0, type == TickType.HOT ? "Combined HoT" : "Combined DoT");
		}
		else {
			this.statusEffect = new XivStatusEffect(rawEffectId);
		}
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

	@Override
	public long getDamage() {
		return type == TickType.DOT ? damageOrHeal : 0;
	}

	public long getRawEffectId() {
		return rawEffectId;
	}

	@Override
	public List<AbilityEffect> getEffects() {
		if (type == TickType.HOT) {
			return Collections.singletonList(new HealEffect(0, 0, HitSeverity.NORMAL, damageOrHeal));
		}
		else {
			return Collections.singletonList(new DamageTakenEffect(0, 0, HitSeverity.NORMAL, damageOrHeal));
		}
	}

	@Override
	public XivStatusEffect getBuff() {
		return statusEffect;
	}

	@Override
	public long getStacks() {
		return 0;
	}
}
