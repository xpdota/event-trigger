package gg.xp.xivsupport.events.triggers.easytriggers.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.function.Predicate;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public interface Condition<X> extends Predicate<X> {
	String fixedLabel();
	String dynamicLabel();
}
