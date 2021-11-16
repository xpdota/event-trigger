package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.models.XivCombatant;

/**
 * Equivalent to an ACT 04 line. This event is intentionally left pretty bare, because
 * you should get all of the info from XivState.
 */
@SystemEvent
public class RawRemoveCombatantEvent extends BaseEvent {
	private static final long serialVersionUID = 3615674340829313314L;
	private final XivCombatant entity;

	public RawRemoveCombatantEvent(XivCombatant entity) {
		this.entity = entity;
	}

	public XivCombatant getEntity() {
		return entity;
	}

}
