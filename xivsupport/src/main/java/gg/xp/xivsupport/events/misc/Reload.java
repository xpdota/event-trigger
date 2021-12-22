package gg.xp.xivsupport.events.misc;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.TopologyReloadEvent;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.reevent.scan.HandleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

// TODO: reloading doesn't quite work yet. Add/remove of classes works, but not
// modifying an existing class.
public class Reload {

	private static final Logger log = LoggerFactory.getLogger(Reload.class);
	private static final int staticId = ThreadLocalRandom.current().nextInt();
	private final int instanceId = ThreadLocalRandom.current().nextInt();

	public Reload() {
		printUids();
	}

	@HandleEvents
	public void checkRandomId(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("printuids")) {
			printUids();
		}
	}

	// Debugging for testing hot reload
	private void printUids() {
		log.debug("UIDs: static {} instance {}", staticId, instanceId);
	}

	@HandleEvents
	public static void handle(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("reload")) {
			context.enqueue(new TopologyReloadEvent());
		}
	}
}
