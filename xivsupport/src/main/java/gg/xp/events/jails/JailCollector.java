package gg.xp.events.jails;

import gg.xp.events.AbilityUsedEvent;
import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.EventHandler;
import gg.xp.events.XivEntity;
import gg.xp.scan.HandleEvents;

import java.util.ArrayList;
import java.util.List;

public class JailCollector implements EventHandler<AbilityUsedEvent> {

	private final List<XivEntity> jailedPlayers = new ArrayList<>();

	@Override
	@HandleEvents
	public void handle(EventContext<Event> context, AbilityUsedEvent event) {
		// Check ability ID - we only care about these two
		int id = event.getAbility().getId();
		if (id != 0x2B6B && id != 0x2B6C) {
			return;
		}
		jailedPlayers.add(event.getTarget());
		// Fire off new event if we have exactly 3 events
		if (jailedPlayers.size() == 3) {
			context.accept(new UnsortedTitanJailsSolvedEvent(new ArrayList<>(jailedPlayers)));
		}
	}
}
