package gg.xp.xivsupport.events.triggers.jobs;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.jobs.Cooldown;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.delaytest.BaseDelayedEvent;
import gg.xp.xivsupport.events.state.PlayerChangedJobEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.CdTrackingKey;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.CooldownSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.persistence.settings.LongSetting;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CdTracker {

	private static final Logger log = LoggerFactory.getLogger(CdTracker.class);
	private static final String cdKeyStub = "cd-tracker.enable-cd.";

	private final BooleanSetting enableTtsPersonal;
	private final BooleanSetting enableTtsParty;
	private final BooleanSetting enableFlyingText;
	private final LongSetting cdTriggerAdvancePersonal;
	private final LongSetting cdTriggerAdvanceParty;
	private final IntSetting overlayMaxPersonal;
	private final IntSetting overlayMaxParty;
	private final Map<Cooldown, CooldownSetting> personalCds = new LinkedHashMap<>();
	private final Map<Cooldown, CooldownSetting> partyCds = new LinkedHashMap<>();
	private final XivState state;

	public CdTracker(PersistenceProvider persistence, XivState state) {
		this.state = state;
		for (Cooldown cd : Cooldown.values()) {
			personalCds.put(cd, new CooldownSetting(persistence, getKey(cd), cd.defaultPersOverlay(), false));
			partyCds.put(cd, new CooldownSetting(persistence, getKey(cd) + ".party", false, false));
		}
		enableTtsPersonal = new BooleanSetting(persistence, "cd-tracker.enable-tts", true);
		enableTtsParty = new BooleanSetting(persistence, "cd-tracker.enable-tts.party", false);
		enableFlyingText = new BooleanSetting(persistence, "cd-tracker.enable-flying-text", false);
		cdTriggerAdvancePersonal = new LongSetting(persistence, "cd-tracker.pre-call-ms", 5000L);
		cdTriggerAdvanceParty = new LongSetting(persistence, "cd-tracker.pre-call-ms.party", 5000L);
		overlayMaxPersonal = new IntSetting(persistence, "cd-tracker.overlay-max", 8);
		overlayMaxParty = new IntSetting(persistence, "cd-tracker.overlay-max.party", 8);
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

	public IntSetting getOverlayMaxPersonal() {
		return overlayMaxPersonal;
	}

	public IntSetting getOverlayMaxParty() {
		return overlayMaxParty;
	}

	@SystemEvent
	static class DelayedCdCallout extends BaseDelayedEvent {

		@Serial
		private static final long serialVersionUID = 6817565445334081296L;
		final AbilityUsedEvent originalEvent;
		final int originalKey;

		protected DelayedCdCallout(AbilityUsedEvent originalEvent, int originalKey, long delayMs) {
			super(delayMs);
			this.originalEvent = originalEvent;
			this.originalKey = originalKey;
		}
	}

	private boolean isEnabledForPersonalTts(Cooldown cd) {
		CooldownSetting personalCdSetting = personalCds.get(cd);
		return personalCdSetting != null && personalCdSetting.getTts().get();
	}

	private boolean isEnabledForPartyTts(Cooldown cd) {
		CooldownSetting partyCdSetting = partyCds.get(cd);
		return partyCdSetting != null && partyCdSetting.getTts().get();
	}

	@HandleEvents
	public void cdUsed(EventContext context, AbilityUsedEvent event) {
		Cooldown cd;
		// target index == 0 ensures that for abilities that can hit multiple enemies, we only start the CD tracking
		// once.
		if (event.getTargetIndex() == 0 && (cd = getCdInfo(event.getAbility().getId())) != null) {
			// TODO: there's some duplicate whitelist logic
			boolean isSelf = event.getSource().isThePlayer();
			if (enableTtsPersonal.get() && isEnabledForPersonalTts(cd) && isSelf) {
				log.info("Personal CD used: {}", event);
				//noinspection NumericCastThatLosesPrecision
				context.enqueue(new DelayedCdCallout(event, cdResetKey, (long) (cd.getCooldown() * 1000) - cdTriggerAdvancePersonal.get()));
			}
			// TODO: party check
			else //noinspection SuspiciousMethodCalls
				if (enableTtsParty.get() && isEnabledForPartyTts(cd) && state.getPartyList().contains(event.getSource())) {
				log.info("Party CD used: {}", event);
				context.enqueue(new DelayedCdCallout(event, cdResetKey, (long) (cd.getCooldown() * 1000) - cdTriggerAdvanceParty.get()));
			}
			synchronized (cdLock) {
				cds.put(CdTrackingKey.of(event, cd), event);
			}
		}
	}

	private void reset() {
		//noinspection NonAtomicOperationOnVolatileField
		cdResetKey++;
		synchronized (cdLock) {
			log.debug("Clearing {} cds", cds.size());
			cds.clear();
		}
	}

	@HandleEvents
	public void wiped(EventContext context, WipeEvent event) {
		reset();
	}

	@HandleEvents
	public void zoneChange(EventContext context, ZoneChangeEvent wipe) {
		reset();
	}

	@HandleEvents
	public void jobChange(EventContext context, PlayerChangedJobEvent job) {
		// TODO: job change should only clear your own CDs
		reset();
	}

	@HandleEvents
	public void refreshReminderCall(EventContext context, DelayedCdCallout event) {
		XivAbility originalAbility = event.originalEvent.getAbility();
		if (event.originalKey == cdResetKey) {
			log.info("CD callout still valid");
			context.accept(new CalloutEvent(originalAbility.getName(), enableFlyingText.get() ? originalAbility.getName() : null));
		}
		else {
			log.info("Not calling {} - no longer valid", originalAbility.getName());
		}
	}

	public Map<CdTrackingKey, AbilityUsedEvent> getOverlayPersonalCds() {
		return getCds(entry -> {
			CooldownSetting cdSetting = personalCds.get(entry.getKey().getCooldown());
			if (cdSetting == null) {
				return false;
			}
			if (!cdSetting.getOverlay().get()) {
				return false;
			}
			return entry.getValue().getSource().walkParentChain().isThePlayer();
		});
	}

	public Map<CdTrackingKey, AbilityUsedEvent> getOverlayPartyCds() {
		return getCds(entry -> {
			CooldownSetting cdSetting = partyCds.get(entry.getKey().getCooldown());
			if (cdSetting == null) {
				return false;
			}
			if (!cdSetting.getOverlay().get()) {
				return false;
			}
			//noinspection SuspiciousMethodCalls
			return state.getPartyList().contains(entry.getValue().getSource().walkParentChain());
		});
	}

	// TODO: just combine these and use predicates
	public Map<CdTrackingKey, AbilityUsedEvent> getCds(Predicate<Map.Entry<CdTrackingKey, AbilityUsedEvent>> cdFilter) {
		synchronized (cdLock) {
			return cds.entrySet()
					.stream()
					.filter(cdFilter)
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}
	}

	public BooleanSetting getEnableTtsPersonal() {
		return enableTtsPersonal;
	}

	public LongSetting getCdTriggerAdvancePersonal() {
		return cdTriggerAdvancePersonal;
	}

	public BooleanSetting getEnableTtsParty() {
		return enableTtsParty;
	}

	public LongSetting getCdTriggerAdvanceParty() {
		return cdTriggerAdvanceParty;
	}


	public Map<Cooldown, CooldownSetting> getPersonalCdSettings() {
		return Collections.unmodifiableMap(personalCds);
	}

	public Map<Cooldown, CooldownSetting> getPartyCdSettings() {
		return Collections.unmodifiableMap(partyCds);
	}
}
