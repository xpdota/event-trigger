package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.Alias;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actionresolution.SequenceIdTracker;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.actlines.events.RawRemoveCombatantEvent;
import gg.xp.xivsupport.events.actlines.events.StatusEffectList;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.actlines.events.XivBuffsUpdatedEvent;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.StatusAppliedEffect;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.BuffTrackingKey;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@Alias("buffs")
@Alias("statuses")
public class StatusEffectRepository {

	private static final Logger log = LoggerFactory.getLogger(StatusEffectRepository.class);


	// Buffs are actually kind of complicated in terms of what does/doesn't stack on the same
	// target, so I'll need to revisit. IIRC buffs that get kicked off due to a similar buff
	// DO in fact explicitly remove the first one, while refreshes don't, so it might not be
	// that bad.
	// For now, just use the event objects as valuessince they contain everything we need.
	private final Map<BuffTrackingKey, BuffApplied> buffs = new LinkedHashMap<>();
	private final Map<BuffTrackingKey, BuffApplied> preApps = new LinkedHashMap<>();
	private final Map<XivEntity, Map<BuffTrackingKey, BuffApplied>> onTargetCache = new HashMap<>();
	private final Map<XivEntity, Map<BuffTrackingKey, BuffApplied>> preappsOnTargetCache = new HashMap<>();
	private final Object lock = new Object();
	private final XivState state;
	private final SequenceIdTracker sqid;

	public StatusEffectRepository(XivState state, SequenceIdTracker sqid) {
		this.state = state;
		this.sqid = sqid;
	}

	@HandleEvents(order = -500)
	public void buffPreApplication(EventContext context, AbilityUsedEvent event) {
		List<StatusAppliedEffect> newPreApps = event.getEffects().stream()
				.filter(StatusAppliedEffect.class::isInstance).map(StatusAppliedEffect.class::cast).toList();
		if (!newPreApps.isEmpty()) {
			synchronized (lock) {
				for (StatusAppliedEffect preApp : newPreApps) {
					BuffApplied fakeEvent = new BuffApplied(event, preApp);
					fakeEvent.setParent(event);
					fakeEvent.setHappenedAt(event.getEffectiveHappenedAt());
					this.preApps.put(new BuffTrackingKey(event.getSource(), fakeEvent.getTarget(), preApp.getStatus()), fakeEvent);
					var key = BuffTrackingKey.of(fakeEvent);
					this.preappsOnTargetCache.computeIfAbsent(fakeEvent.getTarget(), k -> new LinkedHashMap<>()).put(key, fakeEvent);
				}
			}
		}
	}

	@HandleEvents(order = -500)
	public void buffApplication(EventContext context, BuffApplied event) {
		// TODO: should fakes still be tracked somewhere?
		XivCombatant target = event.getTarget();
		if (target.isFake()) {
			return;
		}
		BuffTrackingKey key = BuffTrackingKey.of(event);
		BuffApplied previous;
		synchronized (lock) {
			previous = buffs.put(
					key,
					event
			);
			BuffApplied preapp = preApps.remove(key);
			preappsOnTargetCache.computeIfPresent(target, (k, v) -> {
				v.remove(key);
				return v;
			});
			if (preapp != null) {
				event.setPreAppInfo(preapp.getPreAppAbility(), preapp.getPreAppInfo());
			}
			onTargetCache.computeIfAbsent(target, k -> new LinkedHashMap<>()).put(key, event);
		}
		if (previous != null) {
			event.setIsRefresh(true);
		}
		log.trace("Buff applied: {} applied {} to {}. Tracking {} buffs.", event.getSource().getName(), event.getBuff().getName(), target.getName(), buffs.size());
		context.accept(new XivBuffsUpdatedEvent());
	}

	// TODO: this doesn't actually work as well as I'd like - if the advance timing is too small and/or we're behind on
	// processing, we might hit the remove before the callout.
	@HandleEvents(order = -500)
	public void buffRemove(EventContext context, BuffRemoved event) {
		BuffApplied removed;
		synchronized (lock) {
			BuffTrackingKey key = BuffTrackingKey.of(event);
			removed = buffs.remove(key);
			onTargetCache.getOrDefault(event.getTarget(), Collections.emptyMap()).remove(key);
		}
		if (removed != null) {
			log.trace("Buff removed: {} removed {} from {}. Tracking {} buffs.", event.getSource().getName(), event.getBuff().getName(), event.getTarget().getName(), buffs.size());
			context.accept(new XivBuffsUpdatedEvent());
		}
	}

	@HandleEvents(order = -500)
	public void statusList(EventContext context, StatusEffectList list) {
		XivCombatant target = list.getTarget();
		if (targetHasAnyStatus(target)) {
			return;
		}
		// TODO: is this still desired behavior?
		if (target.isFake()) {
			return;
		}
		synchronized (lock) {
			for (BuffApplied event : list.getStatusEffects()) {
				BuffTrackingKey key = BuffTrackingKey.of(event);
				// Previous is never present since we specifically only allow this for entities with no existing
				// buffs, but leaving it here in case I decide to change this later.
//				BuffApplied previous;
//				previous = buffs.put(
//						key,
//						event
//				);
				BuffApplied preapp = preApps.remove(key);
				preappsOnTargetCache.computeIfPresent(target, (k, v) -> {
					v.remove(key);
					return v;
				});
				if (preapp != null) {
					event.setPreAppInfo(preapp.getPreAppAbility(), preapp.getPreAppInfo());
				}
				onTargetCache.computeIfAbsent(target, k -> new LinkedHashMap<>()).put(key, event);
//				if (previous != null) {
//					event.setIsRefresh(true);
//				}
			}
		}
		context.accept(new XivBuffsUpdatedEvent());
	}

	// TODO: issues with buffs that persist through wipes (like tank stance)
	@HandleEvents
	public void zoneChange(EventContext context, WipeEvent wipe) {
		log.debug("Wipe, clearing {} buffs", buffs.size());
		synchronized (lock) {
			buffs.clear();
			onTargetCache.clear();
			preApps.clear();
			preappsOnTargetCache.clear();
		}
		context.accept(new XivBuffsUpdatedEvent());
	}

	@HandleEvents
	public void zoneChange(EventContext context, ZoneChangeEvent wipe) {
		log.debug("Zone change, clearing {} buffs", buffs.size());
		synchronized (lock) {
			buffs.clear();
			onTargetCache.clear();
			preApps.clear();
			preappsOnTargetCache.clear();
		}
		context.accept(new XivBuffsUpdatedEvent());
	}

	@HandleEvents(order = -500)
	public void removeCombatant(EventContext context, RawRemoveCombatantEvent event) {
		long idToRemove = event.getEntity().getId();
		Iterator<Map.Entry<BuffTrackingKey, BuffApplied>> iterator = buffs.entrySet().iterator();
		Map.Entry<BuffTrackingKey, BuffApplied> current;
		boolean anyRemoved = false;
		synchronized (lock) {
			while (iterator.hasNext()) {
				current = iterator.next();
				BuffTrackingKey key = current.getKey();
				if (key.getTarget().getId() == idToRemove) {
					log.trace("Buff removed: {} removed {} from {} due to removal of target. Tracking {} buffs.", key.getSource().getName(), key.getBuff().getName(), key.getTarget().getName(), buffs.size());
					iterator.remove();
					anyRemoved = true;
					onTargetCache.remove(key.getTarget());
					preappsOnTargetCache.remove(key.getTarget());
				}
			}
		}
		if (anyRemoved) {
			context.accept(new XivBuffsUpdatedEvent());
		}
	}

	@HandleEvents
	public void workaroundForActNotRemovingCombatants(EventContext context, XivStateRecalculatedEvent event) {
		Set<Long> combatantsThatExist = state.getCombatants().keySet();
		// Size will be 0 in situations such as unit testing where we'd rather not have this behavior
		// TODO: we track dead stuff, just do that instead?
		if (!combatantsThatExist.isEmpty()) {
			synchronized (lock) {
				buffs.keySet().removeIf(key -> !combatantsThatExist.contains(key.getTarget().getId()));
				preApps.keySet().removeIf(key -> !combatantsThatExist.contains(key.getTarget().getId()));
			}
		}
	}

	public List<BuffApplied> getBuffs() {
		synchronized (lock) {
			return new ArrayList<>(buffs.values());
		}
	}

	public List<BuffApplied> getPreApps() {
		synchronized (lock) {
			prunePreApps();
			return new ArrayList<>(preApps.values());
		}
	}

	public List<BuffApplied> getBuffsAndPreapps() {
		synchronized (lock) {
			prunePreApps();
			List<BuffApplied> out = new ArrayList<>(buffs.values());
			out.addAll(preApps.values());
			return out;
		}
	}

	public @Nullable BuffApplied get(BuffTrackingKey key) {
		synchronized (lock) {
			return buffs.get(key);
		}
	}

	public @Nullable BuffApplied getPreApp(BuffTrackingKey key) {
		synchronized (lock) {
			prunePreApps();
			return preApps.get(key);
		}
	}

	private boolean shouldRemovePreapp(BuffApplied preapp) {
		// Cap to 5 seconds - assume ghost otherwise
		if (preapp.getEstimatedElapsedDuration().toMillis() > 5000) {
			return true;
		}
		Event parent = preapp.getParent();
		if (parent instanceof AbilityUsedEvent originalAbility) {
			return !sqid.isEventStillPending(originalAbility);
		}
		return false;

	}

	private void prunePreApps() {
		synchronized (lock) {
			preApps.values().removeIf(preapp -> {
				boolean shouldRemove = shouldRemovePreapp(preapp);
				if (shouldRemove) {
					preappsOnTargetCache.computeIfPresent(preapp.getTarget(), (k, preapps) -> {
						var trackingKey = BuffTrackingKey.of(preapp);
						var removed = preapps.remove(trackingKey);
						if (removed == null) {
							log.warn("Did not remove preapp for {}", trackingKey);
						}
						return preapps;
					});
				}
				return shouldRemove;
			});
		}
	}

	public List<BuffApplied> statusesOnTarget(XivEntity entity) {
		return statusesOnTarget(entity, false);
	}

	public List<BuffApplied> statusesOnTarget(XivEntity entity, boolean includePreapps) {
		if (entity == null) {
			return Collections.emptyList();
		}
		synchronized (lock) {
			Map<BuffTrackingKey, BuffApplied> cached = onTargetCache.get(entity);
			if (includePreapps) {
				Map<BuffTrackingKey, BuffApplied> cachedPreapps = preappsOnTargetCache.get(entity);
				if (cachedPreapps != null && !cachedPreapps.isEmpty()) {
					if (cached == null || cached.isEmpty()) {
						return new ArrayList<>(cachedPreapps.values());
					}
					else {
						var out = new ArrayList<>(cached.values());
						out.addAll(cachedPreapps.values());
						return out;
					}
				}
				// Else, fall through to non-preapp case
			}
			if (cached == null || cached.isEmpty()) {
				return Collections.emptyList();
			}
			return new ArrayList<>(cached.values());
		}
	}


	public boolean targetHasAnyStatus(XivEntity entity) {
		if (entity == null) {
			return false;
		}
		synchronized (lock) {
			Map<BuffTrackingKey, BuffApplied> cached = onTargetCache.get(entity);
			return cached != null && !cached.isEmpty();
		}
	}

	public @Nullable BuffApplied findStatusOnTarget(XivEntity entity, long buffId) {
		return findStatusOnTarget(entity, ba -> ba.buffIdMatches(buffId));
	}

	public boolean isStatusOnTarget(XivEntity entity, long buffId) {
		return findStatusOnTarget(entity, ba -> ba.buffIdMatches(buffId)) != null;
	}

	public @Nullable BuffApplied findStatusOnTarget(XivEntity entity, Predicate<BuffApplied> filter) {
		return statusesOnTarget(entity).stream().filter(filter).findAny().orElse(null);
	}

	public @Nullable BuffApplied findBuff(Predicate<BuffApplied> filter) {
		return getBuffs().stream().filter(filter).findFirst().orElse(null);
	}

	public @Nullable BuffApplied findBuffById(long id) {
		return findBuff(ba -> ba.buffIdMatches(id));
	}

	public @NotNull List<BuffApplied> findBuffsById(long id) {
		return findBuffs(ba -> ba.buffIdMatches(id));
	}

	public @NotNull List<BuffApplied> findBuffs(Predicate<BuffApplied> filter) {
		return getBuffs().stream().filter(filter).toList();
	}

	/**
	 * Given an entity and a buff ID, return how many stacks the buff has if present.
	 *
	 * @param entity The entity to check
	 * @param buffId The buff ID
	 * @return The number of stacks, or 0 if it is stackless, or -1 if the buff was not present at all.
	 */
	public int buffStacksOnTarget(XivEntity entity, long buffId) {
		return statusesOnTarget(entity).stream().filter(ba -> ba.buffIdMatches(buffId))
				.findFirst()
				.map(ba -> (int) ba.getStacks())
				.orElse(-1);
	}

	/**
	 * Given an entity and a buff ID, return how many raw stacks the buff has if present.
	 * <p>
	 * The difference between this and {@link #buffStacksOnTarget(XivEntity, long)} is that this one uses the
	 * 'raw' stacks. Some status effects use the 'stacks' field to convey mechanical differences rather than an actual
	 * stack count. 'Stacks' tries to filter these out and instead report a stack count of zero, whereas raw stacks
	 * keeps the original value intact.
	 *
	 * @param entity The entity to check
	 * @param buffId The buff ID
	 * @return The number of stacks, or 0 if it is stackless, or -1 if the buff was not present at all.
	 */
	public int rawBuffStacksOnTarget(XivEntity entity, long buffId) {
		return statusesOnTarget(entity).stream().filter(ba -> ba.buffIdMatches(buffId))
				.findFirst()
				.map(ba -> (int) ba.getRawStacks())
				.orElse(-1);
	}

	public List<BuffApplied> sortedStatusesOnTarget(XivEntity entity) {
		List<BuffApplied> list = statusesOnTarget(entity);
		list.sort(standardPartyFrameSort);
		return list;
	}

	public static final Comparator<BuffApplied> standardPartyFrameSort = Comparator.comparing(ba -> {
		StatusEffectInfo statusEffectInfo = ba.getBuff().getInfo();
		if (statusEffectInfo == null) {
			return 0;
		}
		return -1 * statusEffectInfo.partyListPriority();
	});

	public List<BuffApplied> filteredSortedStatusesOnTarget(XivEntity entity, Predicate<BuffApplied> filter) {
		return filteredSortedStatusesOnTarget(entity, filter, false);
	}

	public List<BuffApplied> filteredSortedStatusesOnTarget(XivEntity entity, Predicate<BuffApplied> filter, boolean includePreapps) {
		return statusesOnTarget(entity, includePreapps)
				.stream()
				.filter(filter)
				.sorted(standardPartyFrameSort)
				.toList();
	}

	public <X extends HasSourceEntity & HasTargetEntity & HasStatusEffect> @Nullable BuffApplied getLatest(X buff) {
		return get(BuffTrackingKey.of(buff));
	}

	public StatusEffectCurrentStatus statusOf(BuffApplied buff) {
		BuffApplied latest = getLatest(buff);
		if (latest == buff) {
			return StatusEffectCurrentStatus.ACTIVE;
		}
		else if (latest != null) {
			return StatusEffectCurrentStatus.REPLACED;
		}
		else {
			return StatusEffectCurrentStatus.GONE;
		}
	}

	public boolean originalStatusActive(BuffApplied buff) {
		StatusEffectCurrentStatus status = statusOf(buff);
//		log.info("Buff status {} for {}", status, buff);
		return status == StatusEffectCurrentStatus.ACTIVE;
	}

	public boolean statusOrRefreshActive(BuffApplied buff) {
		return statusOf(buff) != StatusEffectCurrentStatus.GONE;
	}
}