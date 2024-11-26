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

}
