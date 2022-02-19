package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.TickEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TickTracker {

	private static final Duration TICK_INTERVAL = Duration.ofSeconds(3);
	private final Map<XivCombatant, TickEvent> map = new ConcurrentHashMap<>();

	@HandleEvents
	public void tickEvent(EventContext context, TickEvent tick) {
		TickEvent old = map.put(tick.getTarget(), tick);
		if (old == null) {
			context.accept(new TickUpdatedEvent());
		}
	}

	@HandleEvents
	public void changeZone(EventContext context, ZoneChangeEvent zoneChange) {
		map.clear();
		context.accept(new TickUpdatedEvent());
	}

	public @Nullable TickInfo getTick(XivCombatant cbt) {
		TickEvent tickEvent = map.get(cbt);
		if (tickEvent == null) {
			return null;
		}
		return new TickInfo(tickEvent, TICK_INTERVAL);
	}

}
