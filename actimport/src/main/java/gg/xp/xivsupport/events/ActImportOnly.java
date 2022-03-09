package gg.xp.xivsupport.events;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.sys.PrimaryLogSource;

public interface ActImportOnly extends FilteredEventHandler {
	@Override
	default boolean enabled(EventContext context) {
		return context.getStateInfo().get(PrimaryLogSource.class).isActImport();
	};
}
