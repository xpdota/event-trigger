package gg.xp.events.state;

import gg.xp.events.BaseEvent;

import java.util.Collections;
import java.util.List;

public class CombatantsUpdateRaw extends BaseEvent {

	private static final long serialVersionUID = 6485573030632033688L;
	private final List<RawXivCombatantInfo> combatantMaps;

	public CombatantsUpdateRaw(List<RawXivCombatantInfo> combatantMaps) {
		this.combatantMaps = combatantMaps;
	}

	public List<RawXivCombatantInfo> getCombatantMaps() {
		return Collections.unmodifiableList(combatantMaps);
	}

}
