package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import gg.xp.reevent.events.EventContext;

import java.util.function.Predicate;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface Condition<X> extends Predicate<X> {
	@SuppressWarnings("unused")
	default boolean test(EventContext context, X event) {
		return test(event);
	}
	String fixedLabel();
	String dynamicLabel();
}
