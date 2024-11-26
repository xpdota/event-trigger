package gg.xp.xivsupport.triggers.ultimate;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.events.state.XivState;

public class FRU extends AutoChildEventHandler implements FilteredEventHandler {
	private final XivState state;

	public FRU(XivState state) {
		this.state = state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.FRU);
	}

	/*
	 * P1:
	 * Proteans (baited), check lightning/fire
	 * Alternates 4 sets
	 * 1. Baits on player
	 * 2. Hits where #1 set was (i.e. dodge)
	 * 3. Move back
	 * 4. ?
	 * Spread if lightning, stack if fire
	 *
	 * Ilusion mechanic
	 * Tank thing at the start
	 * Stack/spread based on last
	 */
}
