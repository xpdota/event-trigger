package gg.xp.xivsupport.events.triggers.jobs;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.data.Job;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.delaytest.BaseDelayedEvent;
import gg.xp.xivsupport.models.BuffTrackingKey;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

// TODO: figure out how to track BRD buffs in a good way

public class DotRefreshReminders {

	private static final Logger log = LoggerFactory.getLogger(DotRefreshReminders.class);
	private static final String dotKeyStub = "dot-tracker.enable-buff.";

	private long dotRefreshAdvance = 5000L;
	private final Map<DotBuff, Boolean> enabledDots = new LinkedHashMap<>();
	private final StatusEffectRepository buffs;
	private final PersistenceProvider persistence;

	public DotRefreshReminders(StatusEffectRepository buffs, PersistenceProvider persistence) {
		this.buffs = buffs;
		this.persistence = persistence;
		for (DotBuff dot : DotBuff.values()) {
			enabledDots.put(dot, persistence.get(getKey(dot), boolean.class, true));
		}
	}

	private static String getKey(DotBuff buff) {
		return dotKeyStub + buff;
	}

	private boolean isWhitelisted(long id) {
		return Arrays.stream(DotBuff.values())
				.filter(b -> b.matches(id))
				.map(enabledDots::get)
				.findFirst()
				.orElse(false);
	}

	// List of ALL buffs to track - WL/BL will be done by user settings
	public enum DotBuff {
		// JLS/javac being dumb, had to put the L there to make it a long
		Dia(Job.WHM, 0x8fL, 0x90L, 0x74fL),
		Biolysis(Job.SCH, 0xb3L, 0xbdL, 0x767L),
		GoringBlade(Job.PLD, 0x2d5L);

		private final Job job;
		private final Set<Long> buffIds;

		DotBuff(Job job, Long... buffIds) {
			this.job = job;
			this.buffIds = Set.of(buffIds);
		}

		public Job getJob() {
			return job;
		}

		public String getLabel() {
			return name();
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

	public Map<DotBuff, Boolean> getEnabledDots() {
		return Collections.unmodifiableMap(enabledDots);
	}

	public void setDotEnabled(DotBuff buff, boolean enabled) {
		enabledDots.put(buff, enabled);
		persistence.save(getKey(buff), enabled);
	}

	public long getDotRefreshAdvance() {
		return dotRefreshAdvance;
	}

	public void setDotRefreshAdvance(long dotRefreshAdvance) {
		this.dotRefreshAdvance = dotRefreshAdvance;
	}
}
