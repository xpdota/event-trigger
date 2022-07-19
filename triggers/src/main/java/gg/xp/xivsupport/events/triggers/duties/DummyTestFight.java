package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.debug.DebugCommand;

@CalloutRepo(name = "Dummy (/e c:testcall)", duty = KnownDuty.None)
public class DummyTestFight {

	private volatile boolean dummyHold;

	private final ModifiableCallout<DebugCommand> dummy = new ModifiableCallout<>("Dummy Callout to Test UI", "Test");
	private final ModifiableCallout<DebugCommand> dummy2 = new ModifiableCallout<>("Dummy Callout to Test Holds", "Test", "Test", x -> !dummyHold);

	// Dummy call that uses the event
	@HandleEvents
	public void dummyCall(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("testcall")) {
			context.accept(dummy.getModified(event));
		}
	}

	// Dummy call that does not use the event
	@HandleEvents
	public void dummyCall2(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("testcall2")) {
			context.accept(dummy.getModified());
		}
	}

	@HandleEvents
	public void dummyCallOn(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("testcall_on")) {
			dummyHold = true;
			context.accept(dummy2.getModified(event));
		}
	}
	@HandleEvents
	public void dummyCallOff(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("testcall_off")) {
			dummyHold = false;
		}
	}

}
