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
import gg.xp.xivsupport.persistence.settings.IntSetting;
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
import java.util.stream.Collectors;

public class CdTracker {

	private static final Logger log = LoggerFactory.getLogger(CdTracker.class);
	private static final String cdKeyStub = "cd-tracker.enable-cd.";
	// TODO: have these be tristate rather than on/off:
	// Never call, call only my own, call in party
	// Or just two checkboxes

	private final BooleanSetting enableTtsPersonal;
	private final BooleanSetting enableTtsParty;
	private final LongSetting cdTriggerAdvancePersonal;
	private final LongSetting cdTriggerAdvanceParty;
	private final IntSetting overlayMaxPersonal;
	private final IntSetting overlayMaxParty;
	private final Map<Cooldown, BooleanSetting> personalCds = new LinkedHashMap<>();
	private final Map<Cooldown, BooleanSetting> partyCds = new LinkedHashMap<>();
	private final XivState state;

	public CdTracker(PersistenceProvider persistence, XivState state) {
		this.state = state;
		for (Cooldown cd : Cooldown.values()) {
			personalCds.put(cd, new BooleanSetting(persistence, getKey(cd), false));
			partyCds.put(cd, new BooleanSetting(persistence, getKey(cd) + ".party", false));
		}
		enableTtsPersonal = new BooleanSetting(persistence, "cd-tracker.enable-tts", true);
		enableTtsParty = new BooleanSetting(persistence, "cd-tracker.enable-tts.party", false);
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

	private boolean isEnabledForPersonal(Cooldown cd) {
		BooleanSetting personalCdSetting = personalCds.get(cd);
		return personalCdSetting != null && personalCdSetting.get();
	}

	private boolean isEnabledForParty(Cooldown cd) {
		BooleanSetting partyCdSetting = partyCds.get(cd);
		return partyCdSetting != null && partyCdSetting.get();
	}

	private boolean isWhitelisted(AbilityUsedEvent event) {
		long id = event.getAbility().getId();
		// Find applicable CD setting
		Cooldown cooldown = Arrays.stream(Cooldown.values())
				.filter(b -> b.abilityIdMatches(id))
				// Then, see if it's enabled
				.findFirst()
				.orElse(null);
		if (cooldown == null) {
			return false;
		}
		if (event.getSource().isThePlayer()) {
			if (isEnabledForPersonal(cooldown)) {
				return true;
			}
		}
		if (state.getPartyList().contains(event.getSource())) {
			return isEnabledForParty(cooldown);
		}
		return false;
	}

	@HandleEvents
	public void cdUsed(EventContext context, AbilityUsedEvent event) {
		Cooldown cd;
		if ((cd = getCdInfo(event.getAbility().getId())) != null) {
			// TODO: there's some duplicate whitelist logic
			boolean isSelf = event.getSource().isThePlayer();
			log.info("CD used: {}", event);
			if (enableTtsPersonal.get() && isEnabledForPersonal(cd) && isSelf) {
				//noinspection NumericCastThatLosesPrecision
				context.enqueue(new DelayedCdCallout(event, cdResetKey, (long) (cd.getCooldown() * 1000) - cdTriggerAdvancePersonal.get()));
			}
			else if (enableTtsParty.get() && isEnabledForParty(cd)) {
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
		reset();
	}

	@HandleEvents
	public void refreshReminderCall(EventContext context, DelayedCdCallout event) {
		XivAbility originalAbility = event.originalEvent.getAbility();
		if (event.originalKey == cdResetKey) {
			log.info("CD callout still valid");
			context.accept(new CalloutEvent(originalAbility.getName()));
		}
		else {
			log.info("Not calling {} - no longer valid", originalAbility.getName());
		}
	}

	public Map<CdTrackingKey, AbilityUsedEvent> getMyCooldowns() {
		synchronized (cdLock) {
			return cds.entrySet()
					.stream()
					.filter(entry -> isEnabledForPersonal(entry.getKey().getCooldown()))
					.filter(entry -> entry.getValue().getSource().walkParentChain().isThePlayer())
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}
	}

	public Map<CdTrackingKey, AbilityUsedEvent> getPartyCooldowns() {
		synchronized (cdLock) {
			return cds.entrySet()
					.stream()
					.filter(entry -> isEnabledForParty(entry.getKey().getCooldown()))
					.filter(entry -> state.getPartyList().contains(entry.getValue().getSource().walkParentChain()))
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


	public Map<Cooldown, BooleanSetting> getPersonalCdSettings() {
		return Collections.unmodifiableMap(personalCds);
	}

	public Map<Cooldown, BooleanSetting> getPartyCdSettings() {
		return Collections.unmodifiableMap(partyCds);
	}
}
