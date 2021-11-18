package gg.xp.xivsupport.events.state;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.state.QueueState;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.reevent.scan.HandleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateDumpDebugCommand {

	private static final Logger log = LoggerFactory.getLogger(StateDumpDebugCommand.class);

	@HandleEvents
	public void checkRandomId(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("dumpstate")) {
			XivState xivState = context.getStateInfo().get(XivState.class);
			log.info("Dumping state");
			log.info("Player: {}", xivState.getPlayer());
			log.info("Party: {}", xivState.getPartyList());
			log.info("Zone: {}", xivState.getZone());
			QueueState queueState = context.getStateInfo().get(QueueState.class);
			if (queueState == null) {
				log.error("No queue state!");
			}
			else {
				log.info("Queue depth: {}", queueState.getQueueDepth());
			}
		}
	}

}
