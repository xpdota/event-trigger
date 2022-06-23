package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;

public class InCombatChangeEvent extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = -9160363705280650889L;
	private final boolean inCombat;

	public InCombatChangeEvent(boolean inCombat) {
		this.inCombat = inCombat;
	}

	public boolean isInCombat() {
		return inCombat;
	}

	@Override
	public String getPrimaryValue() {
		return inCombat ? "In Combat" : "Out of Combat";
	}
}
