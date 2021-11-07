package gg.xp.events.triggers.jobs;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.actlines.BuffApplied;
import gg.xp.events.delaytest.BaseDelayedEvent;
import gg.xp.events.filters.Filters;
import gg.xp.events.models.BuffTrackingKey;
import gg.xp.scan.HandleEvents;
import gg.xp.speech.TtsCall;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DotTracker {

	private static final long dotRefreshAdvance = 5000L;

	// Buffs are actually kind of complicated in terms of what does/doesn't stack on the same
	// target, so I'll need to revisit. IIRC buffs that get kicked off due to a similar buff
	// DO in fact explicitly remove the first one, while refreshes don't, so it might not be
	// that bad.
	// For now, just use the event objects as valuessince they contain everything we need.
	private final Map<BuffTrackingKey, BuffApplied> buffs = new HashMap<>();


	// WL of buffs to track
	private enum WhitelistedBuffs {
		// JLS/javac being dumb, had to put the L there to make it a long
		Dia(0x8fL, 0x90L, 0x74fL),
		GoringBlade(0x2d5L);

		private final Set<Long> buffIds;

		WhitelistedBuffs(Long... buffIds) {
			this.buffIds = Set.of(buffIds);
		}

		boolean matches(long id) {
			return buffIds.contains(id);
		}
	}

	private static boolean isWhitelisted(long id) {
		return Arrays.stream(WhitelistedBuffs.values())
				.anyMatch(b -> b.matches(id));
	}

	private static class DelayedBuffCallout extends BaseDelayedEvent {

		private final BuffApplied originalEvent;

		protected DelayedBuffCallout(BuffApplied originalEvent, long delayMs) {
			super(delayMs);
			this.originalEvent = originalEvent;
		}
	}

	private static BuffTrackingKey getKey(BuffApplied event) {
		return new BuffTrackingKey(event.getSource(), event.getTarget(), event.getBuff());
	}

	// TODO: handle buff removal, enemy dying before buff expires, etc

	@HandleEvents
	public void buffApplication(EventContext<Event> context, BuffApplied event) {
		if (Filters.sourceIsPlayer(context, event) && isWhitelisted(event.getBuff().getId())) {
			buffs.put(
					getKey(event),
					event
			);
			context.enqueue(new DelayedBuffCallout(event, (long) (event.getDuration() * 1000L - dotRefreshAdvance)));
		}
	}

	@HandleEvents
	public void refreshReminderCall(EventContext<Event> context, DelayedBuffCallout event) {
		BuffApplied originalEvent = event.originalEvent;
		BuffApplied mostRecentEvent = buffs.get(getKey(originalEvent));
		if (originalEvent == mostRecentEvent) {
			context.accept(new TtsCall(originalEvent.getBuff().getName()));
		}
	}
}
