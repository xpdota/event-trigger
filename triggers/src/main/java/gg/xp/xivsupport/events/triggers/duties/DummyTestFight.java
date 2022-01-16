package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.debug.DebugCommand;

@CalloutRepo("Dummy (/e c:testcall)")
public class DummyTestFight {

	private final ModifiableCallout dummy = new ModifiableCallout("Dummy Callout to Test UI", "Test");

	@HandleEvents
	public void dummyCall(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("testcall")) {
			context.accept(dummy.getModified(event));
		}
	}


}
