package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actionresolution.SequenceIdTracker;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.RawRemoveCombatantEvent;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.actlines.events.XivBuffsUpdatedEvent;
import gg.xp.xivsupport.events.actlines.events.XivStateRecalculatedEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.StatusAppliedEffect;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.BuffTrackingKey;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		synchronized (lock) {
			for (StatusAppliedEffect preApp : newPreApps) {
				BuffApplied fakeValue = new BuffApplied(event, preApp);
				fakeValue.setParent(event);
				fakeValue.setHappenedAt(Instant.now());
				this.preApps.put(new BuffTrackingKey(event.getSource(), preApp.isOnTarget() ? event.getTarget() : event.getSource(), preApp.getStatus()), fakeValue);
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
			preApps.remove(key);
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

	// TODO: issues with buffs that persist through wipes (like tank stance)
	@HandleEvents
	public void zoneChange(EventContext context, WipeEvent wipe) {
		log.debug("Wipe, clearing {} buffs", buffs.size());
		synchronized (lock) {
			buffs.clear();
			onTargetCache.clear();
		}
		context.accept(new XivBuffsUpdatedEvent());
	}

	@HandleEvents
	public void zoneChange(EventContext context, ZoneChangeEvent wipe) {
		log.debug("Zone change, clearing {} buffs", buffs.size());
		synchronized (lock) {
			buffs.clear();
			onTargetCache.clear();
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
		synchronized (lock) {
			buffs.keySet().removeIf(key -> !combatantsThatExist.contains(key.getTarget().getId()));
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

	private void prunePreApps() {
		synchronized (lock) {
			preApps.values().removeIf(v -> {
				// Cap to 5 seconds - assume ghost otherwise
				if (v.getEstimatedElapsedDuration().toMillis() > 5000) {
					return true;
				}
				Event parent = v.getParent();
				if (parent instanceof AbilityUsedEvent) {
					return !sqid.isEventStillPending((AbilityUsedEvent) parent);
				}
				return false;
			});
		}
	}


	public List<BuffApplied> statusesOnTarget(XivEntity entity) {
		if (entity == null) {
			return Collections.emptyList();
		}
		synchronized (lock) {
			Map<BuffTrackingKey, BuffApplied> cached = onTargetCache.get(entity);
			if (cached == null) {
				return Collections.emptyList();
			}
			return new ArrayList<>(cached.values());
		}
	}
}