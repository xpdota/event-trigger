package gg.xp.xivsupport.events.misc;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.reevent.scan.HandleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TestChain {

	private static final Logger log = LoggerFactory.getLogger(TestChain.class);

	@HandleEvents
	public void checkPedigree(EventContext<Event> context, DebugCommand event) {
		if (event.getCommand().equals("showchain")) {
			List<Event> eventChain = new ArrayList<>();
			Event current = event;
			while (current != null) {
				eventChain.add(0, current);
				current = current.getParent();
			}
			log.info("Event Pedigree: {}", eventChain.stream().map(Event::getClass).map(Class::getSimpleName).collect(Collectors.joining(" -> ")));
		}
	}
}
