package gg.xp.xivsupport.events.actlines.events.actorcontrol;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.ActorControlEvent;

public class ActorControlEventHandler {

	@HandleEvents
	public void actorControl(EventContext context, ActorControlEvent acEvent) {
		long command = acEvent.getCommand();
		// Can't use switch because Java doesn't allow switches on longs for some reason
		// https://github.com/quisquous/cactbot/blob/main/docs/LogGuide.md#line-33-0x21-network6d-actor-control
		// Not going to actually put the instance ID in there, since it is already available via context.
		Event event;
		if (command == 0x4000_0001L) {
			// Initial commence
			event = new DutyInitialCommenceEvent();
		}
		else if (command == 0x4000_0006L) {
			// Recommence
			event = new DutyRecommenceEvent();
		}
		else if (command == 0x8000_0004L) {
			// Lockout time adjust
			// TODO: implement
			return;
		}
		else if (command == 0x8000_000CL) {
			// Charge boss LB
			// TODO: implement
			return;
		}
		else if (command == 0x8000_0001L) {
			// Music change
			// TODO: implement
			return;
		}
		else if (command == 0x4000_0005L) {
			// Fade Out
			event = new FadeOutEvent();
		}
		// These two changed in 6.2
		else if (command == 0x4000_0010L || command == 0x4000_000FL) {
			// Fade In
			event = new FadeInEvent();
		}
		else if (command == 0x4000_0012L || command == 0x4000_0011L) {
			// Barrier Up
			event = new BarrierUpEvent();
		}
		else if (command == 0x4000_0003L) {
			// Victory
			event = new VictoryEvent();
		}
		else {
			return;
		}
//		event.setHappenedAt(acEvent.getHappenedAt());
		context.accept(event);
	}

}
