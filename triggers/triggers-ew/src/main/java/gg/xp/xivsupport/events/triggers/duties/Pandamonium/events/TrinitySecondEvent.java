package gg.xp.xivsupport.events.triggers.duties.Pandamonium.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.models.ArenaSector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrinitySecondEvent extends BaseEvent {
	private final List<Boolean> rightSafe;
	private final List<ArenaSector> safeSpots;

	public TrinitySecondEvent(List<Boolean> rightSafe, List<ArenaSector> safeSpots) {
		this.rightSafe = new ArrayList<>(rightSafe);
		this.safeSpots = new ArrayList<>(safeSpots);
	}

	public List<Boolean> getRightSafe() {
		return Collections.unmodifiableList(rightSafe);
	}

	public List<ArenaSector> getSafeSpots() {
		return Collections.unmodifiableList(safeSpots);
	}
}
