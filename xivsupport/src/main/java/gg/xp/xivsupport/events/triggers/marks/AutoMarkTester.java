package gg.xp.xivsupport.events.triggers.marks;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.debug.DebugCommand;

import java.util.List;

public class AutoMarkTester {

	@HandleEvents
	public void amTest(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("amtest")) {
			List<String> args = event.getArgs();
			args.subList(1, args.size())
					.stream()
					.mapToInt(Integer::parseInt)
					.forEach(i -> context.accept(new AutoMarkSlotRequest(i)));
		}
	}

}
