package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface Action<X> {
	void accept(EasyTriggerContext context, X event);

	String fixedLabel();

	String dynamicLabel();

	default void recalc() {
	}
}
