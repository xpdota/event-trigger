package gg.xp.xivsupport.events.misc;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.events.actlines.events.ChatLineEvent;
import gg.xp.reevent.scan.HandleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoMessage {

	private static final Logger log = LoggerFactory.getLogger(EchoMessage.class);

	@HandleEvents
	public static void handle(EventContext<Event> context, ChatLineEvent event) {
		if (event.getCode() == 0x38) {
			String line = event.getLine();
			log.info("Echo Message: {}", line);
			context.accept(new EchoEvent(line));
		}
	}


}
