package gg.xp.xivsupport.events.triggers.jobs;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.jobs.Cooldown;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.actlines.events.XivBuffsUpdatedEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.delaytest.BaseDelayedEvent;
import gg.xp.xivsupport.events.filters.Filters;
import gg.xp.xivsupport.events.state.PlayerChangedJobEvent;
import gg.xp.xivsupport.models.CdTrackingKey;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.LongSetting;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CdTracker {

	private static final Logger log = LoggerFactory.getLogger(CdTracker.class);
	private static final String cdKeyStub = "cd-tracker.enable-cd.";
	// TODO: have these be tristate rather than on/off:
	// Never call, call only my own, call in party
	// Or just two checkboxes

	private final BooleanSetting enableTts;
	private final BooleanSetting enableOverlay;
	private final LongSetting cdTriggerAdvance;
	private final Map<Cooldown, BooleanSetting> enabledCds = new LinkedHashMap<>();

	public CdTracker(PersistenceProvider persistence) {
		for (Cooldown cd : Cooldown.values()) {
			enabledCds.put(cd, new BooleanSetting(persistence, getKey(cd), false));
		}
		enableTts = new BooleanSetting(persistence, "cd-tracker.enable-tts", true);
		enableOverlay = new BooleanSetting(persistence, "cd-tracker.enable-tts", true);
		cdTriggerAdvance = new LongSetting(persistence, "cd-tracker.pre-call-ms", 5000L);
	}

	private static String getKey(Cooldown buff) {
		return cdKeyStub + buff;
	}

	// To be incremented on wipe or other event that would reset cooldowns
	private volatile int cdResetKey;

	private final Object cdLock = new Object();
	private final Map<CdTrackingKey, AbilityUsedEvent> cds = new HashMap<>();

	private static @Nullable Cooldown getCdInfo(long id) {
		return Arrays.stream(Cooldown.values())
				.filter(b -> b.abilityIdMatches(id))
				.findFirst()
				.orElse(null);
	}

	@SystemEvent
	private static class DelayedCdCallout extends BaseDelayedEvent {

		private static final long serialVersionUID = 6817565445334081296L;
		private final AbilityUsedEvent originalEvent;
		private final int originalKey;

		protected DelayedCdCallout(AbilityUsedEvent originalEvent, int originalKey, long delayMs) {
			super(delayMs);
			this.originalEvent = originalEvent;
			this.originalKey = originalKey;
		}
	}

	private boolean isWhitelisted(long id) {
		return Arrays.stream(Cooldown.values())
				.filter(b -> b.abilityIdMatches(id))
				.map(enabledCds::get)
				.map(BooleanSetting::get)
				.findFirst()
				// Non-dots will go here
				.orElse(false);
	}

	// TODO: handle buff removal, enemy dying before buff expires, etc

	@HandleEvents
	public void cdUsed(EventContext context, AbilityUsedEvent event) {
		Cooldown cd;
		if (Filters.sourceIsPlayer(context, event) && (cd = getCdInfo(event.getAbility().getId())) != null && isWhitelisted(event.getAbility().getId())) {
			log.info("CD used: {}", event);
			//noinspection NumericCastThatLosesPrecision
			context.enqueue(new DelayedCdCallout(event, cdResetKey, (long) (cd.getCooldown() * 1000) - cdTriggerAdvance.get()));
			synchronized (cdLock) {
				cds.put(CdTrackingKey.of(event, cd), event);
			}
		}
	}

	@HandleEvents
	public void wiped(EventContext context, WipeEvent event) {
		//noinspection NonAtomicOperationOnVolatileField
		cdResetKey++;
		synchronized (cdLock) {
			cds.clear();
		}
	}

	@HandleEvents
	public void zoneChange(EventContext context, ZoneChangeEvent wipe) {
		log.debug("Zone change, clearing {} cds", cds.size());
		cds.clear();
	}

	@HandleEvents
	public void jobChange(EventContext context, PlayerChangedJobEvent job) {
		log.debug("Job change, clearing {} cds", cds.size());
		cds.clear();
	}

	@HandleEvents
	public void refreshReminderCall(EventContext context, DelayedCdCallout event) {
		XivAbility originalAbility = event.originalEvent.getAbility();
		if (event.originalKey == cdResetKey) {
			log.info("CD callout still valid");
			if (enableTts.get()) {
				context.accept(new CalloutEvent(originalAbility.getName()));
			}
		}
		else {
			log.info("Not calling {} - no longer valid", originalAbility.getName());
		}
	}

	public Map<CdTrackingKey, AbilityUsedEvent> getCurrentCooldowns() {
		synchronized (cdLock) {
//			recheckMyDots();
			return new LinkedHashMap<>(cds);
		}
	}

	public BooleanSetting getEnableTts() {
		return enableTts;
	}

	public BooleanSetting getEnableOverlay() {
		return enableOverlay;
	}

	public LongSetting getCdTriggerAdvance() {
		return cdTriggerAdvance;
	}

	public Map<Cooldown, BooleanSetting> getEnabledCds() {
		return Collections.unmodifiableMap(enabledCds);
	}
}
