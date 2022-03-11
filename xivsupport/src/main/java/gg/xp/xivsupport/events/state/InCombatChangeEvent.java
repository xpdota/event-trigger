package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.BaseEvent;

import java.io.Serial;

public class InCombatChangeEvent extends BaseEvent {

	@Serial
	private static final long serialVersionUID = -9160363705280650889L;
	private final boolean inCombat;

	public InCombatChangeEvent(boolean inCombat) {
		this.inCombat = inCombat;
	}

	public boolean isInCombat() {
		return inCombat;
	}
}
