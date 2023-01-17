package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.DamageTakenEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.DamageType;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.HealEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.HitSeverity;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivStatusEffect;

import java.util.Collections;
import java.util.List;

public class GroundTickEvent extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasEffects, HasStatusEffect {

	private final XivCombatant source;
	private final XivCombatant target;
	private final TickType type;
	private final long damageOrHeal;
	private final long rawEffectId;
	private final XivStatusEffect statusEffect;
	private final DamageType damageType;


	public GroundTickEvent(XivCombatant target, TickType type, long damageOrHeal, DamageType damageType, long effectId, XivCombatant source) {
		this.source = source;
		this.target = target;
		this.type = type;
		this.damageOrHeal = damageOrHeal;
		this.damageType = damageType;
		this.rawEffectId = effectId;
		if (rawEffectId == 0) {
			this.statusEffect = new XivStatusEffect(0, type == TickType.HOT ? "Combined HoT" : "Combined DoT");
		}
		else {
			this.statusEffect = new XivStatusEffect(rawEffectId);
		}
	}

	@Override
	public XivCombatant getSource() {
		return source;
	}

	@Override
	public XivCombatant getTarget() {
		return target;
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

	public DamageType getDamageType() {
		return damageType;
	}

	@Override
	public List<AbilityEffect> getEffects() {
		if (type == TickType.HOT) {
			return Collections.singletonList(new HealEffect(HitSeverity.NORMAL, damageOrHeal));
		}
		else {
			return Collections.singletonList(new DamageTakenEffect(HitSeverity.NORMAL, damageOrHeal) {
				@Override
				public DamageType getDamageType() {
					return damageType;
				}
			});
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

	@Override
	public long getRawStacks() {
		return 0;
	}
}
