package gg.xp.reevent.scan;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventHandler;

import java.lang.reflect.Field;
import java.util.Arrays;

public abstract class AutoChildEventHandler {

	protected AutoChildEventHandler() {
//		for (Field field : getClass().getFields()) {
//			if (EventHandler.class.isAssignableFrom(field.getType())) {
//				if (field.getType().equals(EventHandler.class)) {
//					field.getGenericType();
//				}
//				if ()
//			}
//		}
//		Arrays.stream(getClass().getFields())
//				.filter()
	}
	// TODO: finish this

	@HandleEvents
	public void childEventHandlers(EventContext ctx, Event event) {

	}

}
