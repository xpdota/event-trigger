package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.XivCombatant;

public class TickEvent extends BaseEvent implements HasTargetEntity {


	private final XivCombatant combatant;
	private final TickType type;
	private final long damage;
	private final long rawEffectId;

	public TickEvent(XivCombatant combatant, TickType type, long damage, long rawEffectId) {

		this.combatant = combatant;
		this.type = type;
		this.damage = damage;
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
}
