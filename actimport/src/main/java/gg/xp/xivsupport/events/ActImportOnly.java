package gg.xp.xivsupport.events;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivsupport.events.state.XivState;

public interface ActImportOnly extends FilteredEventHandler {
	@Override
	default boolean enabled(EventContext context) {
		return context.getStateInfo().get(XivState.class).isActImport();
	};
}
