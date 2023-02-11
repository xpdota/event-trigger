package gg.xp.xivsupport.events.misc;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.ChatLineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EchoMessage {

	private static final Logger log = LoggerFactory.getLogger(EchoMessage.class);

	@HandleEvents
	public static void handle(EventContext context, ChatLineEvent event) {
		if (event.getCode() == 0x38) {
			String line = event.getLine();
			if (!line.startsWith("Hojoring>[EX]")) {
				log.info("Echo Message: {}", line);
			}
			context.accept(new EchoEvent(line));
		}
	}


}
