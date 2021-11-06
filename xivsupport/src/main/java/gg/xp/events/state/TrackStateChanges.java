package gg.xp.events.state;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.actlines.PlayerChangeEvent;
import gg.xp.events.actlines.ZoneChangeEvent;
import gg.xp.scan.HandleEvents;

@SuppressWarnings("UtilityClassWithoutPrivateConstructor")
public final class TrackStateChanges {

	@HandleEvents
	public static void handle(EventContext<Event> context, ZoneChangeEvent event) {
		context.getStateInfo().get(XivState.class).setZone(event.getZone());
	}

	@HandleEvents
	public static void handle(EventContext<Event> context, PlayerChangeEvent event) {
		context.getStateInfo().get(XivState.class).setPlayer(event.getPlayer());
	}

}
