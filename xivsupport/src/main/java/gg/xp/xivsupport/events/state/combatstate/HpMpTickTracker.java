package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.HpMpTickEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.models.XivCombatant;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracker for DoT/HoT ticks.
 */
public class HpMpTickTracker {

	private static final Duration TICK_INTERVAL = Duration.ofSeconds(3);
	private final Map<XivCombatant, HpMpTickEvent> map = new ConcurrentHashMap<>();

	@HandleEvents
	public void HpMpTickEvent(EventContext context, HpMpTickEvent tick) {
		// Non-zero indicates a ground effect, which is not what we're interested in.
		HpMpTickEvent old = map.put(tick.getTarget(), tick);
//		if (old == null) {
//			context.accept(new TickUpdatedEvent());
//		}
	}

	@HandleEvents
	public void changeZone(EventContext context, ZoneChangeEvent zoneChange) {
		map.clear();
//		context.accept(new TickUpdatedEvent());
	}

	public @Nullable TickInfo getTick(XivCombatant cbt) {
		HpMpTickEvent HpMpTickEvent = map.get(cbt);
		if (HpMpTickEvent == null) {
			return null;
		}
		return new TickInfo(HpMpTickEvent, TICK_INTERVAL);
	}

}
