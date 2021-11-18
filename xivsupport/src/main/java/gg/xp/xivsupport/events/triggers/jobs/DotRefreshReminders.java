package gg.xp.xivsupport.events.triggers.jobs;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.delaytest.BaseDelayedEvent;
import gg.xp.xivsupport.models.BuffTrackingKey;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;

public class DotRefreshReminders {

	private static final Logger log = LoggerFactory.getLogger(DotRefreshReminders.class);

	private static final long dotRefreshAdvance = 5000L;
	private final StatusEffectRepository buffs;

	public DotRefreshReminders(StatusEffectRepository buffs) {
		this.buffs = buffs;
	}

	private static boolean isWhitelisted(long id) {
		return Arrays.stream(WhitelistedBuffs.values())
				.anyMatch(b -> b.matches(id));
	}

	// WL of buffs to track
	private enum WhitelistedBuffs {
		// JLS/javac being dumb, had to put the L there to make it a long
		Dia(0x8fL, 0x90L, 0x74fL),
		Biolysis(0xb3L, 0xbdL, 0x767L),
		GoringBlade(0x2d5L);

		private final Set<Long> buffIds;

		WhitelistedBuffs(Long... buffIds) {
			this.buffIds = Set.of(buffIds);
		}

		boolean matches(long id) {
			return buffIds.contains(id);
		}
	}

	@HandleEvents
	public void buffApplication(EventContext context, BuffApplied event) {
		if (event.getSource().isThePlayer() && isWhitelisted(event.getBuff().getId()) && !event.getTarget().isFake()) {
			context.enqueue(new DelayedBuffCallout(event, (long) (event.getDuration() * 1000L - dotRefreshAdvance)));
		}

	}


	private static class DelayedBuffCallout extends BaseDelayedEvent {

		private static final long serialVersionUID = 499685323334095132L;
		private final BuffApplied originalEvent;

		protected DelayedBuffCallout(BuffApplied originalEvent, long delayMs) {
			super(delayMs);
			this.originalEvent = originalEvent;
		}
	}


	@HandleEvents
	public void refreshReminderCall(EventContext context, DelayedBuffCallout event) {
		BuffApplied originalEvent = event.originalEvent;
		BuffApplied mostRecentEvent = buffs.get(BuffTrackingKey.of(originalEvent));
		if (originalEvent == mostRecentEvent) {
			log.debug("Dot refresh callout still valid");
			context.accept(new CalloutEvent(originalEvent.getBuff().getName()));
		}
		else {
			log.debug("Not calling");
		}
	}


}
