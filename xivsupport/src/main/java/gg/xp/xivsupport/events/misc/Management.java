package gg.xp.xivsupport.events.misc;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.reevent.scan.HandleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Management {

	private static final Logger log = LoggerFactory.getLogger(Management.class);

	@HandleEvents
	public void forceGc(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("gc")) {
			log.info("Explicit GC requested");
			//noinspection CallToSystemGC
			System.gc();
		}
	}

}
