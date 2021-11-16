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

	private static final long dotRefreshAdvance = 5000L;

	// Buffs are actually kind of complicated in terms of what does/doesn't stack on the same
	// target, so I'll need to revisit. IIRC buffs that get kicked off due to a similar buff
	// DO in fact explicitly remove the first one, while refreshes don't, so it might not be
	// that bad.
	// For now, just use the event objects as valuessince they contain everything we need.
	private final Map<BuffTrackingKey, BuffApplied> buffs = new LinkedHashMap<>();


	// WL of buffs to track
	private enum WhitelistedBuffs {
		// JLS/javac being dumb, had to put the L there to make it a long
		Dia(0x8fL, 0x90L, 0x74fL),
		Biolysis(0xb3L, 0xbdL, 0x767L),
		GoringBlade(0x2d5L);

		private final Set<Long> buffIds;

		WhitelistedBuffs(Long... buffIds) {
			this.buffIds = Set.of(buffIds);
		}

		boolean matches(long id) {
			return buffIds.contains(id);
		}
	}

	private static boolean isWhitelisted(long id) {
		return Arrays.stream(WhitelistedBuffs.values())
				.anyMatch(b -> b.matches(id));
	}

	private static class DelayedBuffCallout extends BaseDelayedEvent {

		private static final long serialVersionUID = 499685323334095132L;
		private final BuffApplied originalEvent;

		protected DelayedBuffCallout(BuffApplied originalEvent, long delayMs) {
			super(delayMs);
			this.originalEvent = originalEvent;
		}
	}

	private static <X extends HasSourceEntity & HasTargetEntity & HasStatusEffect> BuffTrackingKey getKey(X event) {
		return new BuffTrackingKey(event.getSource(), event.getTarget(), event.getBuff());
	}

	// TODO: handle buff removal, enemy dying before buff expires, etc

	@HandleEvents(order = -500)
	public void buffApplication(EventContext<Event> context, BuffApplied event) {
		// TODO: should fakes still be tracked somewhere?
		if (event.getTarget().isFake()) {
			return;
		}
		BuffApplied previous = buffs.put(
				getKey(event),
				event
		);
		if (previous != null) {
			event.setIsRefresh(true);
		}
		log.debug("Buff applied: {} applied {} to {}. Tracking {} buffs.", event.getSource().getName(), event.getBuff().getName(), event.getTarget().getName(), buffs.size());
		if (event.getSource().isThePlayer() && isWhitelisted(event.getBuff().getId()) && !event.getTarget().isFake()) {
			context.enqueue(new DelayedBuffCallout(event, (long) (event.getDuration() * 1000L - dotRefreshAdvance)));
		}
		context.accept(new XivBuffsUpdatedEvent());
	}

	// TODO: this doesn't actually work as well as I'd like - if the advance timing is too small and/or we're behind on
	// processing, we might hit the remove before the callout.
	@HandleEvents(order = -500)
	public void buffRemove(EventContext<Event> context, BuffRemoved event) {
		BuffApplied removed = buffs.remove(getKey(event));
		if (removed != null) {
			log.debug("Buff removed: {} removed {} from {}. Tracking {} buffs.", event.getSource().getName(), event.getBuff().getName(), event.getTarget().getName(), buffs.size());
			context.accept(new XivBuffsUpdatedEvent());
		}
	}

	@HandleEvents
	public void wipe(EventContext<Event> context, WipeEvent wipe) {
		log.debug("Wipe, clearing {} buffs", buffs.size());
		buffs.clear();
		context.accept(new XivBuffsUpdatedEvent());
	}

	@HandleEvents
	public void wipe(EventContext<Event> context, ZoneChangeEvent wipe) {
		log.debug("Zone change, clearing {} buffs", buffs.size());
		buffs.clear();
		context.accept(new XivBuffsUpdatedEvent());
	}

	@HandleEvents(order = -500)
	public void removeCombatant(EventContext<Event> context, RawRemoveCombatantEvent event) {
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

	@HandleEvents
	public void refreshReminderCall(EventContext<Event> context, DelayedBuffCallout event) {
		BuffApplied originalEvent = event.originalEvent;
		BuffApplied mostRecentEvent = buffs.get(getKey(originalEvent));
		if (originalEvent == mostRecentEvent) {
			log.debug("Dot refresh callout still valid");
			context.accept(new CalloutEvent(originalEvent.getBuff().getName()));
		}
		else {
			log.debug("Not calling");
		}
	}

	public List<BuffApplied> getBuffs() {
		// TODO: thread safety
		return new ArrayList<>(buffs.values());
	}
}