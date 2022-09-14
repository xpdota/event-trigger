package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import gg.xp.reevent.events.EventContext;

import java.util.function.Predicate;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface Condition<X> {
	@SuppressWarnings("unused")
	boolean test(EasyTriggerContext context, X event);
	String fixedLabel();
	String dynamicLabel();

	default void recalc() {}

	default int sortOrder() {
		return 0;
	};
}
