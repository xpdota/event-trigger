package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;

import java.io.Serial;

@SystemEvent
public class RefreshCombatantsRequest extends BaseEvent {

	@Serial
	private static final long serialVersionUID = -3649990345648876155L;

	@Override
	public boolean shouldSave() {
		return false;
	}
}
