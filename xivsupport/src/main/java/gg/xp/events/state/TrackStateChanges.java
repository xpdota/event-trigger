package gg.xp.events.state;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.actlines.events.PlayerChangeEvent;
import gg.xp.events.actlines.events.ZoneChangeEvent;
import gg.xp.scan.HandleEvents;

@SuppressWarnings("UtilityClassWithoutPrivateConstructor")
public final class TrackStateChanges {

	@HandleEvents
	public static void zoneChange(EventContext<Event> context, ZoneChangeEvent event) {
		context.getStateInfo().get(XivState.class).setZone(event.getZone());
	}

	@HandleEvents
	public static void playerChange(EventContext<Event> context, PlayerChangeEvent event) {
		context.getStateInfo().get(XivState.class).setPlayer(event.getPlayer());
		// After learning about the player, make sure we request combatant data
		context.accept(new RefreshCombatantsRequest());
	}

	@HandleEvents
	public static void partyChange(EventContext<Event> context, PartyChangeEvent event) {
		context.getStateInfo().get(XivState.class).setPartyList(event.getMembers());
	}

	@HandleEvents
	public static void combatants(EventContext<Event> context, CombatantsUpdateRaw event) {
		context.getStateInfo().get(XivState.class).setCombatants(event.getCombatantMaps());
	}

}
