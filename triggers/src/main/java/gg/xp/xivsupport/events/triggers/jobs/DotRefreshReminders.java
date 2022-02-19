package gg.xp.xivsupport.events.triggers.jobs;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.jobs.DotBuff;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.StatusAppliedEffect;
import gg.xp.xivsupport.events.delaytest.BaseDelayedEvent;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.models.BuffTrackingKey;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.persistence.settings.LongSetting;
import gg.xp.xivsupport.speech.BasicCalloutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class DotRefreshReminders {

	private static final Logger log = LoggerFactory.getLogger(DotRefreshReminders.class);
	private static final String dotKeyStub = "dot-tracker.enable-buff.";

	private final BooleanSetting enableTts;
	private final BooleanSetting enableFlyingText;
	private final LongSetting dotRefreshAdvance;
	private final Map<DotBuff, BooleanSetting> enabledDots = new LinkedHashMap<>();
	private final StatusEffectRepository buffs;
	private final IntSetting numberToDisplay;
	// TODO: make this a real setting
	boolean suppressSpamCallouts = true;

	public DotRefreshReminders(StatusEffectRepository buffs, PersistenceProvider persistence) {
		this.buffs = buffs;
		for (DotBuff dot : DotBuff.values()) {
			enabledDots.put(dot, new BooleanSetting(persistence, getKey(dot), true));
		}
		this.dotRefreshAdvance = new LongSetting(persistence, "dot-tracker.pre-call-ms", 5000);
		this.enableTts = new BooleanSetting(persistence, "dot-tracker.enable-tts", true);
		// TODO put this on UI
		this.enableFlyingText = new BooleanSetting(persistence, "dot-tracker.enable-flying-text", false);
		this.numberToDisplay = new IntSetting(persistence, "dot-tracker.disp-time", 8, 1, 32);
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

	private final Object myDotsLock = new Object();
	private final Map<BuffTrackingKey, BuffApplied> myDots = new HashMap<>();

	@HandleEvents
	public void buffApplication(EventContext context, BuffApplied event) {
		if (event.getSource().isThePlayer() && isWhitelisted(event.getBuff().getId()) && !event.getTarget().isFake()) {
			if (enableTts.get()) {
				context.enqueue(new DelayedBuffCallout(event, event.getInitialDuration().toMillis() - dotRefreshAdvance.get()));
			}
			synchronized (myDotsLock) {
				myDots.put(BuffTrackingKey.of(event), event);
				recheckMyDots();
			}
		}
	}

	@HandleEvents
	public void buffPreApplication(EventContext context, AbilityUsedEvent event) {
		// Be more responsive by also tracking pre-apps
		if (event.getSource().isThePlayer() && !event.getTarget().isFake()) {
			List<StatusAppliedEffect> preApps = event.getEffects().stream()
					.filter(StatusAppliedEffect.class::isInstance).map(StatusAppliedEffect.class::cast)
					.filter(StatusAppliedEffect::isOnTarget)
					.filter(effect -> isWhitelisted(effect.getStatus().getId())).toList();
			synchronized (myDotsLock) {
				for (StatusAppliedEffect preApp : preApps) {
					BuffApplied value = new BuffApplied(event, preApp);
					value.setParent(event);
					value.setHappenedAt(Instant.now());
					myDots.put(new BuffTrackingKey(event.getSource(), event.getTarget(), preApp.getStatus()), value);
				}
				recheckMyDots();
			}
		}
	}

	private void recheckMyDots() {
		myDots.entrySet().removeIf(e -> {
			BuffApplied buffFromRepo = buffs.get(e.getKey());
			// Keep the buff displayed if it is still active,
			// OR if it is inactive but would be past expiry,
			// but not by more than 5 seconds.
			if (buffFromRepo != null) {
				// TODO: find more universal solution for this
				// Sometimes a combatant does not get removed correctly by ACT.
				// This is a last-resort way of removing dots on such combatants.
				//noinspection RedundantIfStatement
				if (buffFromRepo.getEstimatedTimeSinceExpiry().compareTo(Duration.of(15, ChronoUnit.SECONDS)) > 0) {
					return true;
				}
				return false;
			}
			if (buffs.getPreApp(e.getKey()) != null) {
				return false;
			}
			Duration timeSinceExpiry = e.getValue().getEstimatedTimeSinceExpiry();
			Duration timeRemaining = e.getValue().getEstimatedRemainingDuration();
			// Keep showing the buff as "expired" for 5 seconds, but also allow for a bit of slop
			// in the other direction.
			return timeRemaining.compareTo(Duration.of(2, ChronoUnit.SECONDS)) > 0
					|| timeSinceExpiry.compareTo(Duration.of(5, ChronoUnit.SECONDS)) > 0;
		});
	}

	@SystemEvent
	static class DelayedBuffCallout extends BaseDelayedEvent {

		@Serial
		private static final long serialVersionUID = 499685323334095132L;
		private final BuffApplied originalEvent;

		protected DelayedBuffCallout(BuffApplied originalEvent, long delayMs) {
			super(delayMs);
			this.originalEvent = originalEvent;
		}
	}

	private Instant lastCallout = Instant.now();
	// Kind of a hack but good enough
	private Instant lastBrdCallout = Instant.now();
	private long lastEntityId;

	@HandleEvents
	public void refreshReminderCall(EventContext context, DelayedBuffCallout event) {
		BuffApplied originalEvent = event.originalEvent;
		BuffApplied mostRecentEvent = buffs.get(BuffTrackingKey.of(originalEvent));
		if (originalEvent == mostRecentEvent) {
			log.debug("Dot refresh callout still valid");
			Instant now = Instant.now();
			// BRD special case
			if (DotBuff.BRD_CombinedDots.matches(originalEvent.getBuff().getId())) {
				Duration delta = Duration.between(lastBrdCallout, now);
				long thisEntityId = originalEvent.getTarget().getId();
				if (delta.toMillis() > 3500 || thisEntityId != lastEntityId) {
					context.accept(new BasicCalloutEvent("Dots", enableFlyingText.get() ? "Dots" : null));
				}
				lastBrdCallout = now;
				lastEntityId = thisEntityId;
			}
			else {
				if (!suppressSpamCallouts || Duration.between(lastCallout, now).toMillis() > 500) {
					String name = originalEvent.getBuff().getName();
					String adjustedName = adjustDotName(name);
					context.accept(new BasicCalloutEvent(adjustedName, enableFlyingText.get() ? adjustedName : null));
				}
				lastCallout = now;
			}
		}
		else {
			log.debug("Not calling");
		}
	}

	private static String adjustDotName(String originalName) {
		// Special case for SGE
		if (originalName.startsWith("Eukrasian")) {
			return "Dosis";
		}
		else if (originalName.endsWith(" II") || originalName.endsWith(" IV")) {
			return originalName.substring(0, originalName.length() - 3);
		}
		else if (originalName.endsWith(" III")) {
			return originalName.substring(0, originalName.length() - 4);
		}
		else {
			return originalName;
		}
	}

	public Map<DotBuff, BooleanSetting> getEnabledDots() {
		return Collections.unmodifiableMap(enabledDots);
	}

	public LongSetting getDotRefreshAdvance() {
		return dotRefreshAdvance;
	}

	public List<BuffApplied> getCurrentDots() {
		synchronized (myDotsLock) {
			recheckMyDots();
			return new ArrayList<>(myDots.values());
		}
	}

	public IntSetting getNumberToDisplay() {
		return numberToDisplay;
	}

	public BooleanSetting getEnableTts() {
		return enableTts;
	}
}
