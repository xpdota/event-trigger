package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.RawAddCombatantEvent;
import gg.xp.xivsupport.events.actlines.events.RawRemoveCombatantEvent;

import java.util.Collections;

/**
 * Acts as a replacement for ActWS when not using live data
 */
public final class ActStateReader {

	private final XivState xivState;

	public ActStateReader(XivState xivState) {
		this.xivState = xivState;
	}

//	@HandleEvents(order = Integer.MIN_VALUE)
//	public void zoneChange(EventContext context, ZoneChangeEvent event) {
//		xivState.setZone(event.getZone());
//		context.accept(new RefreshCombatantsRequest());
//	}
//
//	@HandleEvents(order = Integer.MIN_VALUE)
//	public void playerChange(EventContext context, RawPlayerChangeEvent event) {
//		xivState.setPlayer(event.getPlayer());
//		// After learning about the player, make sure we request combatant data
//		context.accept(new RefreshCombatantsRequest());
//	}

	@HandleEvents(order = Integer.MIN_VALUE)
	public void combatantAdded(EventContext context, RawAddCombatantEvent event) {
		xivState.setSpecificCombatants(Collections.singletonList(event.getFullInfo()));
	}

	@HandleEvents(order = Integer.MIN_VALUE)
	public void combatantRemoved(EventContext context, RawRemoveCombatantEvent event) {
		long idToRemove = event.getEntity().getId();
		xivState.removeSpecificCombatant(idToRemove);
	}
}
