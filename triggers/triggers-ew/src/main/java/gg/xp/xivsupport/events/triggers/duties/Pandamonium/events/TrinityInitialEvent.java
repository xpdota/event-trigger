package gg.xp.xivsupport.events.triggers.duties.Pandamonium.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.ArenaSector;

public class TrinityInitialEvent extends BaseEvent {
	private final boolean rightSafe;
	private final ArenaSector safeSpot;

	public TrinityInitialEvent(boolean rightSafe, ArenaSector safeSpot) {
		this.rightSafe = rightSafe;
		this.safeSpot = safeSpot;
	}

	public boolean isRightSafe() {
		return rightSafe;
	}

	public ArenaSector getSafeSpot() {
		return safeSpot;
	}
}
