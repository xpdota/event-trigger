package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.events.state.XivState;

import java.io.Serial;

/**
 * Emitted to represent that things in {@link XivState} have been recalculated (combatants info, party list, etc).
 */
@SystemEvent
public class XivStateRecalculatedEvent extends BaseEvent implements XivStateChange {
	@Serial
	private static final long serialVersionUID = -6310170328601920843L;

	@Override
	public boolean shouldSave() {
		return false;
	}
}

