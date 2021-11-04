package gg.xp.events.jails;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.EventHandler;
import gg.xp.events.XivEntity;
import gg.xp.scan.HandleEvents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JailSorter implements EventHandler<UnsortedTitanJailsSolvedEvent> {

	@Override
	@HandleEvents
	public void handle(EventContext<Event> context, UnsortedTitanJailsSolvedEvent event) {
		// This is where we would do job prio, custom prio, or whatever else you can come up with
		List<XivEntity> jailedPlayers = new ArrayList<>(event.getJailedPlayers());
		jailedPlayers.sort(Comparator.comparing(XivEntity::getName));
		context.accept(new FinalTitanJailsSolvedEvent(jailedPlayers));
	}
}
