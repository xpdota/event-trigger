package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.time.TimeUtils;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.cdsupport.CustomCooldown;
import gg.xp.xivsupport.cdsupport.CustomCooldownManager;
import gg.xp.xivsupport.cdsupport.CustomCooldownsUpdated;
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
import gg.xp.xivsupport.speech.BasicCalloutEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
	private final Map<Cooldown, CooldownSetting> personalCdsBuiltin = new LinkedHashMap<>();
	private final Map<Cooldown, CooldownSetting> partyCdsBuiltin = new LinkedHashMap<>();
	private final Map<ExtendedCooldownDescriptor, CooldownSetting> personalCdsCustom = new LinkedHashMap<>();
	private final Map<ExtendedCooldownDescriptor, CooldownSetting> partyCdsCustom = new LinkedHashMap<>();
	private Map<ExtendedCooldownDescriptor, CooldownSetting> personalCds = Collections.emptyMap();
	private Map<ExtendedCooldownDescriptor, CooldownSetting> partyCds = Collections.emptyMap();
	private List<ExtendedCooldownDescriptor> allCds = Collections.emptyList();
	private final XivState state;
	private final CustomCooldownManager customCdManager;
	private final PersistenceProvider persistence;

	public CdTracker(PersistenceProvider persistence, XivState state, CustomCooldownManager customCdManager) {
		this.state = state;
		for (Cooldown cd : Cooldown.values()) {
			personalCdsBuiltin.put(cd, new CooldownSetting(persistence, getKey(cd), cd.defaultPersOverlay(), false));
			partyCdsBuiltin.put(cd, new CooldownSetting(persistence, getKey(cd) + ".party", false, false));
		}
		this.persistence = persistence;
		this.customCdManager = customCdManager;
		refreshCustoms();

		enableTtsPersonal = new BooleanSetting(persistence, "cd-tracker.enable-tts", true);
		enableTtsParty = new BooleanSetting(persistence, "cd-tracker.enable-tts.party", false);
		enableFlyingText = new BooleanSetting(persistence, "cd-tracker.enable-flying-text", false);
		cdTriggerAdvancePersonal = new LongSetting(persistence, "cd-tracker.pre-call-ms", 5000L);
		cdTriggerAdvanceParty = new LongSetting(persistence, "cd-tracker.pre-call-ms.party", 5000L);
		overlayMaxPersonal = new IntSetting(persistence, "cd-tracker.overlay-max", 8, 1, 32);
		overlayMaxParty = new IntSetting(persistence, "cd-tracker.overlay-max.party", 8, 1, 32);
	}

	private static String getKey(ExtendedCooldownDescriptor buff) {
		return cdKeyStub + buff.getSettingKeyStub();
	}

	private synchronized void refreshCustoms() {
		log.info("Refreshing custom CDs");
		List<CustomCooldown> customs = customCdManager.getCooldowns();
		personalCdsCustom.clear();
		partyCdsCustom.clear();
		for (CustomCooldown custom : customs) {
			ExtendedCooldownDescriptor cd;
			try {
				cd = custom.buildCd();
			}
			catch (Throwable t) {
				log.error("Error loading custom cooldown ({}, {})", custom.nameOverride, String.format("0x%X", custom.primaryAbilityId));
				continue;
			}
			personalCdsCustom.put(cd, new CooldownSetting(persistence, getKey(cd), cd.defaultPersOverlay(), false));
			partyCdsCustom.put(cd, new CooldownSetting(persistence, getKey(cd) + ".party", false, false));
		}
		Map<ExtendedCooldownDescriptor, CooldownSetting> partyCds = new LinkedHashMap<>();
		Map<ExtendedCooldownDescriptor, CooldownSetting> personalCds = new LinkedHashMap<>();
		partyCds.putAll(partyCdsCustom);
		partyCds.putAll(partyCdsBuiltin);
		personalCds.putAll(personalCdsCustom);
		personalCds.putAll(personalCdsBuiltin);
		this.partyCds = partyCds;
		this.personalCds = personalCds;
		List<ExtendedCooldownDescriptor> all = new ArrayList<>(Arrays.asList(Cooldown.values()));
		for (CustomCooldown custom : customs) {
			all.add(custom.buildCd());
		}
		allCds = all;
		log.info("Number of CDs: {} builtin, {} custom", Cooldown.values().length, customs.size());
	}

	// To be incremented on wipe or other event that would reset cooldowns
	private volatile int cdResetKey;

	private final Object cdLock = new Object();
	private final Map<CdTrackingKey, AbilityUsedEvent> cds = new HashMap<>();
	private final Map<CdTrackingKey, Instant> chargesReplenishedAt = new HashMap<>();

	// TODO: pre-filter ability IDs
	private @Nullable ExtendedCooldownDescriptor getCdInfo(long id) {
		return allCds.stream()
				.filter(b -> b.abilityIdMatches(id))
				.findFirst()
				.orElse(null);
	}

	private record CdAuxUsage(ExtendedCooldownDescriptor cd, CdAuxAbility aux) {
	}

	private List<CdAuxUsage> getAuxInfo(long id) {
		return allCds.stream()
				.map(cd -> {
					CdAuxAbility cdAuxAbility = cd.auxMatch(id);
					if (cdAuxAbility == null) {
						return null;
					}
					return new CdAuxUsage(cd, cdAuxAbility);
				})
				.filter(Objects::nonNull)
				.toList();
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
		final CdTrackingKey key;
		final AbilityUsedEvent originalEvent;
		final int originalResetKey;
		final Instant replenishedAt;

		protected DelayedCdCallout(AbilityUsedEvent originalEvent, Instant replenishedAt, CdTrackingKey key, int originalResetKey, long delayMs) {
			super(delayMs);
			this.originalEvent = originalEvent;
			this.replenishedAt = replenishedAt;
			this.key = key;
			this.originalResetKey = originalResetKey;
		}

		@Override
		public String toString() {
			return "DelayedCdCallout{" +
					"key=" + key +
					", originalEvent=" + originalEvent +
					", originalResetKey=" + originalResetKey +
					'}';
		}
	}

	private boolean isEnabledForPersonalTts(ExtendedCooldownDescriptor cd) {
		CooldownSetting personalCdSetting = personalCds.get(cd);
		return personalCdSetting != null && personalCdSetting.getTtsReady().get();
	}

	private boolean isEnabledForPartyTts(ExtendedCooldownDescriptor cd) {
		CooldownSetting partyCdSetting = partyCds.get(cd);
		return partyCdSetting != null && partyCdSetting.getTtsReady().get();
	}

	private boolean isEnabledForPersonalTtsOnUse(ExtendedCooldownDescriptor cd) {
		CooldownSetting personalCdSetting = personalCds.get(cd);
		return personalCdSetting != null && personalCdSetting.getTtsOnUse().get();
	}

	private boolean isEnabledForPartyTtsOnUse(ExtendedCooldownDescriptor cd) {
		CooldownSetting partyCdSetting = partyCds.get(cd);
		return partyCdSetting != null && partyCdSetting.getTtsOnUse().get();
	}

	@SuppressWarnings({"SuspiciousMethodCalls"})
	@HandleEvents
	public void cdUsed(EventContext context, AbilityUsedEvent event) {
		// Ignore AoE except the first target
		if (!event.isFirstTarget()) {
			return;
		}
		long id = event.getAbility().getId();
		List<CdAuxUsage> aux = getAuxInfo(id);
		{
			ExtendedCooldownDescriptor cd = getCdInfo(id);
			if (cd == null && aux.isEmpty()) {
				// Nothing to do
				return;
			}
			if (cd != null) {
				final Instant newReplenishedAt;
				CdTrackingKey key;
				synchronized (cdLock) {
					key = CdTrackingKey.of(event, cd);
					cds.put(key, event);
					Instant existing = chargesReplenishedAt.get(key);
					// Logic - track when the CD will be fully replenished
					// If there is no existing tracking info, or the existing info says that the CDs would be fully
					// replenished in the past, then set the "replenished at" to now + cooldown time.
					if (existing == null || existing.isBefore(event.effectiveTimeNow())) {
						chargesReplenishedAt.put(key, newReplenishedAt = event.effectiveTimeNow().plus(cd.getCooldownAsDuration()));
					}
					// If there is an existing tracker, just add the duration to that.
					else {
						chargesReplenishedAt.put(key, newReplenishedAt = existing.plus(cd.getCooldownAsDuration()));
					}
				}
				Duration delta = Duration.between(event.effectiveTimeNow(), newReplenishedAt);
				log.trace("Delta: {}", delta);
				// TODO: there's some duplicate whitelist logic
				boolean isSelf = event.getSource().isThePlayer();
				if (enableTtsPersonal.get() && isEnabledForPersonalTts(cd) && isSelf) {
					log.trace("Personal CD delayed: {}", event);
					context.enqueue(new DelayedCdCallout(event, newReplenishedAt, key, cdResetKey, delta.minusMillis(cdTriggerAdvancePersonal.get()).toMillis()));
				}
				else if (enableTtsParty.get() && isEnabledForPartyTts(cd) && state.getPartyList().contains(event.getSource())) {
					log.trace("Party CD delayed: {}", event);
					context.enqueue(new DelayedCdCallout(event, newReplenishedAt, key, cdResetKey, delta.minusMillis(cdTriggerAdvanceParty.get()).toMillis()));
				}
				if (enableTtsPersonal.get() && isEnabledForPersonalTtsOnUse(cd) && isSelf) {
					log.trace("Personal CD immediate: {}", event);
					context.accept(makeCallout(event.getAbility()));
				}
				else if (enableTtsParty.get() && isEnabledForPartyTtsOnUse(cd) && state.getPartyList().contains(event.getSource())) {
					log.trace("Party CD immediate: {}", event);
					context.accept(makeCallout(event.getAbility()));
				}
			}
		}
		if (aux.isEmpty()) {
			return;
		}
		log.info("aux: {}", aux);
		for (CdAuxUsage cdAuxUsage : aux) {
			ExtendedCooldownDescriptor cd = cdAuxUsage.cd;
			Instant newReplenishedAt;
			CdTrackingKey key;
			AbilityUsedEvent keyAbility;
			AbilityUsedEvent existingAbility;
			synchronized (cdLock) {
				// This works because the ability ID actually doesn't matter for the CD tracking key's hash/equals
				key = CdTrackingKey.of(event, cd);
				existingAbility = cds.get(key);
				Instant existing = chargesReplenishedAt.get(key);
//				cds.put(key, event);
				// Logic is slightly different here vs the base case
				// If no existing time for this CD, assume it is available right now.
				if (existing == null || existing.isBefore(event.effectiveTimeNow())) {
					newReplenishedAt = event.effectiveTimeNow();
				}
				else {
					newReplenishedAt = existing;
				}
				// Now add our modification
				newReplenishedAt = newReplenishedAt.plus(cdAuxUsage.aux.getAsDuration());
				// But then clamp the bounds - it can't be before the current time, nor after the longest possible CD
				newReplenishedAt = TimeUtils.clampInstant(newReplenishedAt, event.effectiveTimeNow(), event.effectiveTimeNow().plus(cd.getCooldownAsDuration().multipliedBy(cd.getMaxCharges())));
				chargesReplenishedAt.put(key, newReplenishedAt);
			}
			if (existingAbility != null) {
				keyAbility = existingAbility;
			}
			else {
				keyAbility = event;
			}
			Duration delta = Duration.between(event.effectiveTimeNow(), newReplenishedAt);
			log.info("Delta: {}", delta);
			// TODO: there's some duplicate whitelist logic
			boolean isSelf = event.getSource().isThePlayer();
			if (enableTtsPersonal.get() && isEnabledForPersonalTts(cd) && isSelf) {
				log.info("Personal CD delayed: {}", event);
				context.enqueue(new DelayedCdCallout(keyAbility, newReplenishedAt, key, cdResetKey, delta.minusMillis(cdTriggerAdvancePersonal.get()).toMillis()));
			}
			else if (enableTtsParty.get() && isEnabledForPartyTts(cd) && state.getPartyList().contains(event.getSource())) {
				log.info("Party CD delayed: {}", event);
				context.enqueue(new DelayedCdCallout(keyAbility, newReplenishedAt, key, cdResetKey, delta.minusMillis(cdTriggerAdvanceParty.get()).toMillis()));
			}
			// Ignore "on use" for these
		}
	}

	private void reset() {
		//noinspection NonAtomicOperationOnVolatileField
		cdResetKey++;
		synchronized (cdLock) {
			log.debug("Clearing {} cds", cds.size());
			cds.clear();
			chargesReplenishedAt.clear();
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
		CdTrackingKey key = event.key;
		Instant replenishedAt;
		synchronized (cdLock) {
			replenishedAt = getReplenishedAt(key);
		}
		if (Objects.equals(replenishedAt, event.replenishedAt)) {
			log.info("CD callout still valid");
			context.accept(makeCallout(originalAbility));
		}
		else {
			log.info("Not calling {} - no longer valid", originalAbility.getName());
		}
	}

	private BasicCalloutEvent makeCallout(XivAbility ability) {
		return new BasicCalloutEvent(ability.getName(), enableFlyingText.get() ? ability.getName() : null);
	}

	// TODO: this is only being used for testing
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

	public boolean isEnabledForPersonalOverlay(ExtendedCooldownDescriptor cd) {
		return personalCds.get(cd).getOverlay().get();
	}

	public boolean isEnabledForPartyOverlay(ExtendedCooldownDescriptor cd) {
		return partyCds.get(cd).getOverlay().get();
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

	public @Nullable Instant getReplenishedAt(CdTrackingKey key) {
		synchronized (cdLock) {
			return chargesReplenishedAt.get(key);
		}
	}

	public Map<ExtendedCooldownDescriptor, CooldownSetting> getPersonalCdSettings() {
		return Collections.unmodifiableMap(personalCds);
	}

	public Map<ExtendedCooldownDescriptor, CooldownSetting> getPartyCdSettings() {
		return Collections.unmodifiableMap(partyCds);
	}

	@HandleEvents(order = -100)
	public void cooldownsUpdated(EventContext context, CustomCooldownsUpdated event) {
		refreshCustoms();
		// TODO: would be ideal if there were a way to do this without resetting, but the problem is that
		// the map *keys* still contain obsolete CD information for custom CDs
		reset();
	}
}
