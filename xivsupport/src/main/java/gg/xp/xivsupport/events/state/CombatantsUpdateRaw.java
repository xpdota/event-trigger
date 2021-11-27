package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;

import java.util.Collections;
import java.util.List;

@SystemEvent
// TODO: make this event collapsible
public class CombatantsUpdateRaw extends BaseEvent {

	private static final long serialVersionUID = 6485573030632033688L;
	private final List<RawXivCombatantInfo> combatantMaps;
	private final boolean fullRefresh;

	public CombatantsUpdateRaw(List<RawXivCombatantInfo> combatantMaps, boolean fullRefresh) {
		this.combatantMaps = combatantMaps;
		this.fullRefresh = fullRefresh;
	}

	public boolean isFullRefresh() {
		return fullRefresh;
	}

	public List<RawXivCombatantInfo> getCombatantMaps() {
		return Collections.unmodifiableList(combatantMaps);
	}

}
