package gg.xp.events.misc;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.scan.HandleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TestChain {

	private static final Logger log = LoggerFactory.getLogger(TestChain.class);

	@HandleEvents
	public void checkRandomId(EventContext<Event> context, EchoEvent event) {
		if (event.getLine().equals("showchain")) {
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
