package gg.xp.xivsupport.events.filters;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.events.state.XivState;

public final class Filters {

	private Filters() {
	}

	private static long getPlayerId(EventContext<Event> context) {
		XivEntity player = context.getStateInfo().get(XivState.class).getPlayer();
		if (player == null) {
			// I don't like this but it works
			return -1;
		}
		return player.getId();
	}

	public static boolean sourceIsPlayer(EventContext<Event> context, HasSourceEntity event) {
		return getPlayerId(context) == event.getSource().getId();
	}

	public static boolean targetIsPlayer(EventContext<Event> context, HasTargetEntity event) {
		return getPlayerId(context) == event.getTarget().getId();
	}

}
