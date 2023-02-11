package gg.xp.xivsupport.events.debug;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ScanMe
public class DebugEventLogger {
	private static final Logger log = LoggerFactory.getLogger(DebugEventLogger.class);

	@HandleEvents
	public void debugLog(EventContext context, DebugEvent event) {
		log.info("Debug event: {}", event.getValue());
	}
}
