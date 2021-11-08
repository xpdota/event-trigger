package gg.xp.events.misc;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.debug.DebugCommand;
import gg.xp.scan.HandleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Management {

	private static final Logger log = LoggerFactory.getLogger(Management.class);

	@HandleEvents
	public void forceGc(EventContext<Event> context, DebugCommand event) {
		if (event.getCommand().equals("gc")) {
			log.info("Explicit GC requested");
			//noinspection CallToSystemGC
			System.gc();
		}
	}

}
