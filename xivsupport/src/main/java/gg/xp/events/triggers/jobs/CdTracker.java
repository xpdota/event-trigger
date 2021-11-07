package gg.xp.events.triggers.jobs;

import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.actlines.events.AbilityUsedEvent;
import gg.xp.events.actlines.events.BuffApplied;
import gg.xp.events.actlines.events.BuffRemoved;
import gg.xp.events.actlines.events.WipeEvent;
import gg.xp.events.delaytest.BaseDelayedEvent;
import gg.xp.events.filters.Filters;
import gg.xp.events.models.BuffTrackingKey;
import gg.xp.events.models.XivAbility;
import gg.xp.scan.HandleEvents;
import gg.xp.speech.TtsCall;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CdTracker {

	private static final Logger log = LoggerFactory.getLogger(CdTracker.class);

	private static final long cdTriggerAdvance = 5000L;
	// To be incremented on wipe or other event that would reset cooldowns
	private volatile int cdResetKey;

	// Buffs are actually kind of complicated in terms of what does/doesn't stack on the same
	// target, so I'll need to revisit. IIRC buffs that get kicked off due to a similar buff
	// DO in fact explicitly remove the first one, while refreshes don't, so it might not be
	// that bad.
	// For now, just use the event objects as valuessince they contain everything we need.
	private final Map<BuffTrackingKey, BuffApplied> buffs = new HashMap<>();


	// WL of buffs to track
	private enum TrackedCooldown {
		// JLS/javac being dumb, had to put the L there to make it a long
		Benison(30, 0x1d08L);

		private final int cooldown;
		private final Set<Long> buffIds;

		TrackedCooldown(int cooldown, Long... buffIds) {
			this.cooldown = cooldown;
			this.buffIds = Set.of(buffIds);
		}

		boolean matches(long id) {
			return buffIds.contains(id);
		}
	}

	private static @Nullable TrackedCooldown getCdInfo(long id) {
		return Arrays.stream(TrackedCooldown.values())
				.filter(b -> b.matches(id))
				.findFirst()
				.orElse(null);
	}

	private static class DelayedCdCallout extends BaseDelayedEvent {

		private final AbilityUsedEvent originalEvent;
		private final int originalKey;

		protected DelayedCdCallout(AbilityUsedEvent originalEvent, int originalKey, long delayMs) {
			super(delayMs);
			this.originalEvent = originalEvent;
			this.originalKey = originalKey;
		}
	}

	// TODO: combine?
	private static BuffTrackingKey getKey(BuffApplied event) {
		return new BuffTrackingKey(event.getSource(), event.getTarget(), event.getBuff());
	}

	private static BuffTrackingKey getKey(BuffRemoved event) {
		return new BuffTrackingKey(event.getSource(), event.getTarget(), event.getBuff());
	}

	// TODO: handle buff removal, enemy dying before buff expires, etc

	@HandleEvents
	public void cdUsed(EventContext<Event> context, AbilityUsedEvent event) {
		TrackedCooldown cd;
		if (Filters.sourceIsPlayer(context, event) && (cd = getCdInfo(event.getAbility().getId())) != null) {
			log.info("CD used: {}", event);
			context.enqueue(new DelayedCdCallout(event, cdResetKey, cd.cooldown * 1000L - cdTriggerAdvance));
		}
	}

	// TODO: this doesn't actually work as well as I'd like - if the advance timing is too small and/or we're behind on
	// processing, we might hit the remove before the callout.
	@HandleEvents
	public void wiped(EventContext<Event> context, WipeEvent event) {
		//noinspection NonAtomicOperationOnVolatileField
		cdResetKey++;
	}

	@HandleEvents
	public void refreshReminderCall(EventContext<Event> context, DelayedCdCallout event) {
		XivAbility originalAbility = event.originalEvent.getAbility();
		if (event.originalKey == cdResetKey) {
			log.info("CD callout still valid");
			context.accept(new TtsCall(originalAbility.getName()));
		}
		else {
			log.info("Not calling {} - no longer valid", originalAbility.getName());
		}
	}
}
