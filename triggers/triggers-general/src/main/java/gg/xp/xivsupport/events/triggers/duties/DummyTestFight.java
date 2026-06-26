package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.CalloutVar;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.callouts.RawModifiedCallout;
import gg.xp.xivsupport.events.debug.DebugCommand;

@CalloutRepo(name = "Dummy (/e c:testcall)", duty = KnownDuty.None)
public class DummyTestFight {

	private final EventMaster master;

	private volatile boolean dummyHold;

	private final ModifiableCallout<DebugCommand> dummy = new ModifiableCallout<DebugCommand>("Dummy Callout to Test UI", "Test").extendedDescription("""
			Use the 'Test Callouts' tab (above) to trigger these callouts.""");
	private final ModifiableCallout<?> dummy2 = new ModifiableCallout<>("Dummy Callout to Test Holds", "Test", "Test", x -> !dummyHold);
	private final ModifiableCallout<?> dummy3 = new ModifiableCallout<>("Dummy Callout to Test Variables", "{testVar} {testVar2} {random}");
	private final ModifiableCallout<?> dummy4 = new ModifiableCallout<>("Dummy Callout to Test Icons", "Foo").statusIcons(0x640, 0x641, 0x642, 0x643);
	private final ModifiableCallout<?> dummy5 = new ModifiableCallout<>("Dummy Callout to Test Tts Delay", "Delay").defaultTtsDelay(1_000);
	private final CalloutVar testVar = new CalloutVar("testVar", "Test Variable");
	private final CalloutVar testVar2 = new CalloutVar("testVar2", "Test Variable 2").extendedDescription("This is a test variable with a description");

	public DummyTestFight(EventMaster master) {
		this.master = master;
	}

	// Dummy call that uses the event
	@HandleEvents
	public void dummyCall(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("testcall")) {
			context.accept(dummy.getModified(event));
		}
	}

	public void callDummyNoEvent() {
		master.pushEvent(dummy.getModified());
	}

	public void callDummy2on() {
		dummyHold = true;
		master.pushEvent(dummy2.getModified());
	}

	public void callDummy2off() {
		dummyHold = false;
	}

	public void callDummy3() {
		master.pushEvent(dummy3.getModified());
	}

	public void callDummy4() {
		master.pushEvent(dummy4.getModified());
	}

	public void callDummy5() {
		master.pushEvent(dummy5.getModified());
	}

	public void callDummy5moreDelay() {
		RawModifiedCallout<?> modified = dummy5.getModified();
		modified.setTtsDelayMs(2_000);
		master.pushEvent(modified);
	}
}
