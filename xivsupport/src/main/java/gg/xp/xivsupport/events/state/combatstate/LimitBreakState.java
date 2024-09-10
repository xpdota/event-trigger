package gg.xp.xivsupport.events.state.combatstate;

import gg.xp.reevent.scan.Alias;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.LimitBreakGaugeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Alias("lbState")
public class LimitBreakState {

	private static final LimitBreakGaugeEvent DEFAULT = new LimitBreakGaugeEvent(0, 0);
	private LimitBreakGaugeEvent lastEvent;

	@HandleEvents
	public void limitBreak(LimitBreakGaugeEvent event) {
		lastEvent = event;
	}

	public @Nullable LimitBreakGaugeEvent getLastEvent() {
		return lastEvent;
	}

	public @NotNull LimitBreakGaugeEvent getLastEventOrDefault() {
		LimitBreakGaugeEvent last = lastEvent;
		if (last == null) {
			return DEFAULT;
		}
		return last;
	}

}
