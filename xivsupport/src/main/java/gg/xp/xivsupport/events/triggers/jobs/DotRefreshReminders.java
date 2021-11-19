package gg.xp.xivsupport.events.triggers.jobs;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.jobs.DotBuff;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.delaytest.BaseDelayedEvent;
import gg.xp.xivsupport.models.BuffTrackingKey;
import gg.xp.xivsupport.persistence.BooleanSetting;
import gg.xp.xivsupport.persistence.LongSetting;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// TODO: figure out how to track BRD buffs in a good way

public class DotRefreshReminders {

	private static final Logger log = LoggerFactory.getLogger(DotRefreshReminders.class);
	private static final String dotKeyStub = "dot-tracker.enable-buff.";

	private final LongSetting dotRefreshAdvance;
	private final Map<DotBuff, BooleanSetting> enabledDots = new LinkedHashMap<>();
	private final StatusEffectRepository buffs;

	public DotRefreshReminders(StatusEffectRepository buffs, PersistenceProvider persistence) {
		this.buffs = buffs;
		for (DotBuff dot : DotBuff.values()) {
			enabledDots.put(dot, new BooleanSetting(persistence, getKey(dot), true));
		}
		this.dotRefreshAdvance = new LongSetting(persistence, "dot-tracker.pre-call-ms", 5000);
	}

	private static String getKey(DotBuff buff) {
		return dotKeyStub + buff;
	}

	private boolean isWhitelisted(long id) {
		return Arrays.stream(DotBuff.values())
				.filter(b -> b.matches(id))
				.map(enabledDots::get)
				.map(BooleanSetting::get)
				.findFirst()
				// Non-dots will go here
				.orElse(false);
	}


	@HandleEvents
	public void buffApplication(EventContext context, BuffApplied event) {
		if (event.getSource().isThePlayer() && isWhitelisted(event.getBuff().getId()) && !event.getTarget().isFake()) {
			context.enqueue(new DelayedBuffCallout(event, (long) (event.getDuration() * 1000L - dotRefreshAdvance.get())));
		}
	}


	static class DelayedBuffCallout extends BaseDelayedEvent {

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

	public Map<DotBuff, BooleanSetting> getEnabledDots() {
		return Collections.unmodifiableMap(enabledDots);
	}

	public LongSetting getDotRefreshAdvance() {
		return dotRefreshAdvance;
	}
}
