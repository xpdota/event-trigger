package gg.xp.xivsupport.minimap;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.ActorControlEvent;
import gg.xp.xivsupport.events.actlines.events.SubMapChangeEvent;

@SuppressWarnings("UtilityClassWithoutPrivateConstructor")
public final class SubMapChangeHandler {
	@HandleEvents
	public static void handleMinimapActorControl(EventContext ctx, ActorControlEvent e) {
		if (e.getCommand() == 0x8000_001FL) {
			long id = e.getData0();
			ctx.accept(new SubMapChangeEvent(id == 0 ? null : MapLibrary.forId(id)));
		}
	}
}
