package gg.xp.xivsupport.events.actionresolution;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.AbilityResolvedEvent;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ActionSyncEvent;
import gg.xp.xivsupport.events.actlines.events.EntityKilledEvent;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffectType;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SequenceIdTracker {

	private static final Logger log = LoggerFactory.getLogger(SequenceIdTracker.class);
	private final XivState state;

	// TODO: investigate if this is performant
	private volatile List<AbilityUsedEvent> events = new ArrayList<>();

	// TODO: this has some potential for other uses
	private final Map<XivEntity, List<AbilityUsedEvent>> perTargetCache = new HashMap<>();

	private final Object lock = new Object();

	// Max unresolved events to track
	private static final int MAX_EVENTS = 10_000;
	// Max unresolved events to prune at a time
	private static final int EVENTS_TO_PRUNE = 1_000;
	// Max age in MS before considering an action to be ghosted
	private static final long MAX_AGE = 10_000;

	public SequenceIdTracker(XivState state) {
		this.state = state;
	}

	// TODO: make a unit dying clear unresolved actions from/to it

	// THIS COMMENT IS OUTDATED
	// TODO: current main problem with the idea of being able to use ActionEffects to get predicted data faster than
	// waiting for sync: we have to basically redo the entire way we get combatant data, since it looks something like
	// this:
	/*
		Enemy has 10/10 HP
		I snapshot ability that does 2 damage
		Enemy is predicted to have 8/10 HP (still has 10/10 in reality because it hasn't resolved).
		Ability resolves
		In reality enemy has 8/10 HP, but we haven't gotten new HP from WS yet, so 10/10 is shown.
		We get the real numbers on next combatants update, now shows 8/10.

		In other words, rather than seeing 10 > 8, we see 10 > 8 > 10 > 8.

		Two ideas for how to handle this.

		First: The heavy-handed idea.
		ACT lines should be the primary and authoritative source of combatants information.
		WS combatants data should supplement that for the fields for which ACT lines are unreliable or nonexistent
		(ACT lines have all the data, but the data would still be missing in a different way if you start the program
		after zoning in)
		WS combatants data should also be used when we haven't gotten an update for a particular combatant in a while
		(fixes the issue with fflogs replay where downtime mechanics just don't really show movement).

		Unfortunately this definitely has the ability to break stuff, and I'll also need new tests.

		Second: The lightweight idea

		Keep the current system, but also use ACT lines to complement the data from WS.
		I think for now, it would be sufficient to purely use the NetworkActionSync data, and yoink *just* HP from it,
		since NetworkActionSync is what would actually result in a unit's HP going down.

		Third idea: Combine the two

		This seems like the most reasonable. For replaying ACT log files, I already have some basic extra processing
		of 03/04 lines, so it wouldn't be a stretch to also apply those to ACT lines in general.
	 */

	@HandleEvents
	public void clearOnWipe(EventContext context, WipeEvent event) {
		synchronized (lock) {
			events = new ArrayList<>();
			perTargetCache.clear();
		}
	}

	@HandleEvents
	public void sqidDebug(EventContext context, DebugCommand cmd) {
		if (cmd.getCommand().equals("sqinfo")) {
			log.info("Unresolved actions: {}", events.size());

		}
	}

	@HandleEvents
	public void clearOnZoneChange(EventContext context, ZoneChangeEvent event) {
		synchronized (lock) {
			events = new ArrayList<>();
			perTargetCache.clear();
		}
	}

	private void pushToCache(AbilityUsedEvent event) {
		perTargetCache.computeIfAbsent(event.getTarget(), k -> new ArrayList<>()).add(event);
	}

	private void rebuildCache() {
		perTargetCache.clear();
		events.forEach(this::pushToCache);
	}

//	private void popFromCache(AbilityResolvedEvent event) {
//		perTargetCache.get(event.getTarget()).removeIf(usedEvent -> usedEvent.getSequenceId() == event.getSequenceId());
//	}
//
//	@HandleEvents(order = -2000)
//	public void removeFromCache(EventContext context, AbilityResolvedEvent event) {
//		synchronized (lock) {
//			popFromCache(event);
//		}
//	}

	@HandleEvents(order = -400)
	public void push(EventContext context, AbilityUsedEvent event) {
		XivCombatant target = event.getTarget();
		// "Environment" hits don't seem to ever actually resolve (why would they?)
		// Note that the *source* can still be environment.
		Instant happenedAt = event.getHappenedAt();
		Instant cutoff = happenedAt.minusMillis(MAX_AGE);
		synchronized (lock) {
			boolean dirty = events.removeIf(e -> e.getHappenedAt().isBefore(cutoff));
			if (shouldRecord(event)) {
				events.add(event);
				// TODO: the problem with lazy time based pruning is that often we never actually get anything
				// that would cause a prune for quite some time.
//				dirty |= events.removeIf(e -> {
//					if (!e.getSource().isEnvironment() && state.getCombatants().containsKey(e.getSource().getId())) {
//						return true;
//					}
//					if (!e.getTarget().isEnvironment() && state.getCombatants().containsKey(e.getTarget().getId())) {
//						return true;
//					}
//					return false;
//				});
				if (events.size() > MAX_EVENTS) {
					log.warn("Unresolved events too big, pruning");
					events = new ArrayList<>(events.subList(EVENTS_TO_PRUNE, events.size()));
					dirty = true;
				}
				if (dirty) {
					rebuildCache();
				}
				else {
					pushToCache(event);
				}
			}
		}
	}

	@HandleEvents(order = -400)
	public void pop(EventContext context, ActionSyncEvent event) {
		synchronized (lock) {
			Iterator<AbilityUsedEvent> iterator = events.iterator();
			if (event.getTarget() == null) {
				return;
			}
			while (iterator.hasNext()) {
				AbilityUsedEvent next = iterator.next();
				if (next.getSequenceId() == event.getSequenceId() && next.getTarget().getId() == event.getTarget().getId()) {
					AbilityResolvedEvent newEvent = new AbilityResolvedEvent(next, state.getLatestCombatantData(next.getSource()), state.getLatestCombatantData(next.getTarget()));
//					newEvent.setHappenedAt(event.getHappenedAt());
					context.accept(newEvent);
					iterator.remove();
					break;
				}
			}
			List<AbilityUsedEvent> cache = perTargetCache.get(event.getTarget());
			if (cache != null) {
				cache.removeIf(other -> other.getSequenceId() == event.getSequenceId());
			}
			// TODO: figure out more about whether this is actually a problem or not.
			// Sometimes, you get an ActionSync on both the caster and target.
			log.trace("Could not find ability for sequence ID {} and target {} ({} tracked)", Long.toString(event.getSequenceId(), 16), event.getTarget(), events.size());
		}
	}

	// TODO: find a better way of doing this...
	private boolean shouldRecord(AbilityUsedEvent event) {
		XivEntity target = event.getTarget();
		if (target == null || target.isEnvironment()) {
			return false;
		}
		if (event.getEffects().stream().noneMatch(effect -> {
			AbilityEffectType type = effect.getEffectType();
			return type == AbilityEffectType.DAMAGE || type == AbilityEffectType.APPLY_STATUS || type == AbilityEffectType.BLOCKED || type == AbilityEffectType.PARRIED || type == AbilityEffectType.HEAL;
		})) {
			return false;
		}
		long abilityId = event.getAbility().getId();
		// Ten/Chi/Jin never seem to resolve
		if (abilityId == 0x4976 || abilityId == 0x8D3 || abilityId == 0x4977) {
			return false;
		}
		return true;
	}

	@HandleEvents(order = 400)
	public void casterKilled(EventContext context, EntityKilledEvent event) {
		long targetId = event.getTarget().getId();
		synchronized (lock) {
			boolean removed = events.removeIf(e -> e.getTarget().getId() == targetId || e.getSource().getId() == targetId);
			if (removed) {
				rebuildCache();
			}
		}
	}


	public List<AbilityUsedEvent> getEvents() {
		synchronized (lock) {
			return new ArrayList<>(events);
		}
	}

	public boolean isEventStillPending(AbilityUsedEvent event) {
		synchronized (lock) {
			return events.contains(event);
		}
	}

	public List<AbilityUsedEvent> getEventsTargetedOnEntity(XivEntity target) {
		synchronized (lock) {
			List<AbilityUsedEvent> list = perTargetCache.get(target);
			if (list == null) {
				return Collections.emptyList();
			}
			return new ArrayList<>(list);
		}
	}

	public long unresolvedDamageOnEntity(XivEntity target) {
		synchronized (lock) {
			List<AbilityUsedEvent> events = getEventsTargetedOnEntity(target);
			long dmg = 0;
			for (AbilityUsedEvent event : events) {
				dmg += event.getDamage();
			}
			return dmg;
		}
	}
}
