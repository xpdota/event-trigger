package gg.xp.events.jails;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.EventHandler;
import gg.xp.events.models.XivEntity;
import gg.xp.scan.HandleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JailSorter implements EventHandler<UnsortedTitanJailsSolvedEvent> {

	private static final Logger log = LoggerFactory.getLogger(JailSorter.class);

	@Override
	@HandleEvents
	public void handle(EventContext<Event> context, UnsortedTitanJailsSolvedEvent event) {
		// This is where we would do job prio, custom prio, or whatever else you can come up with
		List<XivEntity> jailedPlayers = new ArrayList<>(event.getJailedPlayers());
		jailedPlayers.sort(Comparator.comparing(XivEntity::getName));
		context.accept(new FinalTitanJailsSolvedEvent(jailedPlayers));
		log.info("Unsorted jails: {}", event.getJailedPlayers());
		log.info("Sorted jails: {}", jailedPlayers);
	}
}
