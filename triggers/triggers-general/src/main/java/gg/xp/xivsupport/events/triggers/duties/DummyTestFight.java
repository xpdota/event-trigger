package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.CalloutVar;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.debug.DebugCommand;

import java.util.Map;

@CalloutRepo(name = "Dummy (/e c:testcall)", duty = KnownDuty.None)
public class DummyTestFight {

	private volatile boolean dummyHold;

	private final ModifiableCallout<DebugCommand> dummy = new ModifiableCallout<DebugCommand>("Dummy Callout to Test UI", "Test").extendedDescription("""
			Use the 'Test Callouts' tab (above) to trigger these callouts.""");
	private final ModifiableCallout<DebugCommand> dummy2 = new ModifiableCallout<>("Dummy Callout to Test Holds", "Test", "Test", x -> !dummyHold);
	private final ModifiableCallout<DebugCommand> dummy3 = new ModifiableCallout<>("Dummy Callout to Test Variables", "{testVar} {testVar2} {random}");
	private final ModifiableCallout<DebugCommand> dummy4 = new ModifiableCallout<DebugCommand>("Dummy Callout to Test Icons", "Foo").statusIcons(0x640, 0x641, 0x642, 0x643);
	private final CalloutVar testVar = new CalloutVar("testVar", "Test Variable");
	private final CalloutVar testVar2 = new CalloutVar("testVar2", "Test Variable 2").extendedDescription("This is a test variable with a description");

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

	@HandleEvents
	public void dummyCall3(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("testcall3")) {
			boolean useVar2 = Math.random() > 0.5;
			context.accept(dummy3.getModified(Map.of("random", useVar2 ? testVar2 : testVar)));
		}
	}

	@HandleEvents
	public void dummyCall4(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("testcall4")) {
			context.accept(dummy4.getModified(event));
		}
	}
}
