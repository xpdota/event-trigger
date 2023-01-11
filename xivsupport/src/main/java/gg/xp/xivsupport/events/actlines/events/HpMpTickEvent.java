package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.models.XivCombatant;

import java.io.Serial;

/**
 * Equivalent to a 39-line. Represents natural HP/MP regen ticks.
 */
@SystemEvent
public class HpMpTickEvent extends BaseEvent implements HasTargetEntity {

	@Serial
	private static final long serialVersionUID = -2571879878928458773L;
	private final XivCombatant combatant;

	public HpMpTickEvent(XivCombatant combatant) {
		this.combatant = combatant;
	}

	@Override
	public XivCombatant getTarget() {
		return combatant;
	}
}
