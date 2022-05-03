package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ActiveCastRepository {
	public ActiveCastRepository() {

	}

	private final Object lock = new Object();
	private final Map<XivCombatant, CastTracker> cbtCasts = new HashMap<>();


	public @Nullable CastTracker getCastFor(XivCombatant cbt) {
		synchronized (lock) {
			return cbtCasts.get(cbt);
		}
	}

	@HandleEvents(order = -50_000)
	public void castStart(EventContext ctx, AbilityCastStart cast) {
		synchronized (lock) {
			cbtCasts.put(cast.getSource(), new CastTracker(cast));
		}
	}

	@HandleEvents(order = -50_000)
	public void castFinished(EventContext ctx, AbilityUsedEvent used) {
		doEnd(used);
	}

	// TODO: I never did interrupt lines
//	@HandleEvents(order = -50_000)
//	public void castInterrupted(EventContext ctx, AbilityIn)

	private <X extends Event & HasSourceEntity & HasAbility> void doEnd(X event) {
		CastTracker tracker;
		synchronized (lock) {
			tracker = cbtCasts.get(event.getSource());
		}
		if (tracker == null) {
			return;
		}
		if (tracker.getCast().getAbility().equals(event.getAbility())) {
			tracker.setEnd(event);
		}

	}
}
