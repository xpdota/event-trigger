package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.AbilityCastCancel;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasCastPrecursor;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActiveCastRepositoryImpl implements ActiveCastRepository {

	private final Object lock = new Object();
	private final Map<XivCombatant, CastTracker> cbtCasts = new HashMap<>();


	@Override
	public @Nullable CastTracker getCastFor(XivCombatant cbt) {
		synchronized (lock) {
			return cbtCasts.get(cbt);
		}
	}

	@Override
	public List<CastTracker> getAll() {
		synchronized (lock) {
			// Remove stale - TODO check performance impact before enabling
//			cbtCasts.values().removeIf(ct -> ct.getResult() == CastResult.IN_PROGRESS && ct.getEstimatedTimeSinceExpiry().toSeconds() > 50);
			return new ArrayList<>(cbtCasts.values());
		}
	}

	@HandleEvents(order = -50_000)
	public void castStart(EventContext ctx, AbilityCastStart cast) {
		CastTracker tracker = new CastTracker(cast);
		XivCombatant source = cast.getSource();
		synchronized (lock) {
			cbtCasts.put(source, tracker);
		}
	}

	@HandleEvents(order = -50_000)
	public void castFinished(EventContext ctx, AbilityUsedEvent used) {
		doEnd(used);
	}

	@HandleEvents(order = -50_000)
	public void castInterrupted(EventContext ctx, AbilityCastCancel acc) {
		doEnd(acc);
	}

	@HandleEvents(order = -50_000)
	public void pullStartedEvent(EventContext ctx, PullStartedEvent event) {
		synchronized (lock) {
			cbtCasts.clear();
		}
	}

	private <X extends BaseEvent & HasSourceEntity & HasAbility & HasCastPrecursor> void doEnd(X event) {
		CastTracker tracker;
		synchronized (lock) {
			tracker = cbtCasts.get(event.getSource());
		}
		if (tracker == null) {
			return;
		}
		if (tracker.getCast().getAbility().equals(event.getAbility())) {
			event.setPrecursor(tracker.getCast());
			tracker.setEnd(event);
		}

	}
}
