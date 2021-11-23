package gg.xp.xivsupport.events.triggers.jobs;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.actlines.events.RawRemoveCombatantEvent;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.actlines.events.XivBuffsUpdatedEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.delaytest.BaseDelayedEvent;
import gg.xp.xivsupport.models.BuffTrackingKey;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
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


	@HandleEvents(order = -500)
	public void buffApplication(EventContext context, BuffApplied event) {
		// TODO: should fakes still be tracked somewhere?
		if (event.getTarget().isFake()) {
			return;
		}
		BuffApplied previous = buffs.put(
				BuffTrackingKey.of(event),
				event
		);
		if (previous != null) {
			event.setIsRefresh(true);
		}
		log.debug("Buff applied: {} applied {} to {}. Tracking {} buffs.", event.getSource().getName(), event.getBuff().getName(), event.getTarget().getName(), buffs.size());
		context.accept(new XivBuffsUpdatedEvent());
	}

	// TODO: this doesn't actually work as well as I'd like - if the advance timing is too small and/or we're behind on
	// processing, we might hit the remove before the callout.
	@HandleEvents(order = -500)
	public void buffRemove(EventContext context, BuffRemoved event) {
		BuffApplied removed = buffs.remove(BuffTrackingKey.of(event));
		if (removed != null) {
			log.debug("Buff removed: {} removed {} from {}. Tracking {} buffs.", event.getSource().getName(), event.getBuff().getName(), event.getTarget().getName(), buffs.size());
			context.accept(new XivBuffsUpdatedEvent());
		}
	}

	// TODO: issues with buffs that persist through wipes (like tank stance)
	@HandleEvents
	public void wipe(EventContext context, WipeEvent wipe) {
		log.debug("Wipe, clearing {} buffs", buffs.size());
		buffs.clear();
		context.accept(new XivBuffsUpdatedEvent());
	}

	@HandleEvents
	public void wipe(EventContext context, ZoneChangeEvent wipe) {
		log.debug("Zone change, clearing {} buffs", buffs.size());
		buffs.clear();
		context.accept(new XivBuffsUpdatedEvent());
	}

	@HandleEvents(order = -500)
	public void removeCombatant(EventContext context, RawRemoveCombatantEvent event) {
		long idToRemove = event.getEntity().getId();
		Iterator<Map.Entry<BuffTrackingKey, BuffApplied>> iterator = buffs.entrySet().iterator();
		Map.Entry<BuffTrackingKey, BuffApplied> current;
		boolean anyRemoved = false;
		while (iterator.hasNext()) {
			current = iterator.next();
			BuffTrackingKey key = current.getKey();
			if (key.getTarget().getId() == idToRemove) {
				log.debug("Buff removed: {} removed {} from {} due to removal of target. Tracking {} buffs.", key.getSource().getName(), key.getBuff().getName(), key.getTarget().getName(), buffs.size());
				iterator.remove();
				anyRemoved = true;
			}
		}
		if (anyRemoved) {
			context.accept(new XivBuffsUpdatedEvent());
		}
	}

	public List<BuffApplied> getBuffs() {
		return new ArrayList<>(buffs.values());
	}

	public @Nullable BuffApplied get(BuffTrackingKey key) {
		return buffs.get(key);
	}
}