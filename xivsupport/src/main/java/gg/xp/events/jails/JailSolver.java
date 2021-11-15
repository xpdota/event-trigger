package gg.xp.events.jails;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.actlines.events.AbilityUsedEvent;
import gg.xp.events.actlines.events.WipeEvent;
import gg.xp.events.models.XivEntity;
import gg.xp.scan.HandleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JailSolver {
	private static final Logger log = LoggerFactory.getLogger(JailSolver.class);

	private final List<XivEntity> jailedPlayers = new ArrayList<>();

	@HandleEvents
	public void handleWipe(EventContext<Event> context, WipeEvent event) {
		jailedPlayers.clear();
	}

	@HandleEvents
	public void handleJailCast(EventContext<Event> context, AbilityUsedEvent event) {
		// Check ability ID - we only care about these two
		long id = event.getAbility().getId();
		if (id != 0x2B6B && id != 0x2B6C) {
			return;
		}
		jailedPlayers.add(event.getTarget());
		// Fire off new event if we have exactly 3 events
		if (jailedPlayers.size() == 3) {
			context.accept(new UnsortedTitanJailsSolvedEvent(new ArrayList<>(jailedPlayers)));
		}
	}

	@HandleEvents
	public void sortTheJails(EventContext<Event> context, UnsortedTitanJailsSolvedEvent event) {
		// This is where we would do job prio, custom prio, or whatever else you can come up with
		List<XivEntity> jailedPlayers = new ArrayList<>(event.getJailedPlayers());
		jailedPlayers.sort(Comparator.comparing(XivEntity::getName));
		context.accept(new FinalTitanJailsSolvedEvent(jailedPlayers));
		log.info("Unsorted jails: {}", event.getJailedPlayers());
		log.info("Sorted jails: {}", jailedPlayers);
	}
}
