package gg.xp.xivsupport.gui.tables.filters;

import java.util.function.Predicate;

public interface QuickFilter<X> extends Predicate<X> {

	Class<X> filterClass();
	String name();

	default boolean appliesToType(Class<?> otherType) {
		return filterClass().isAssignableFrom(otherType);
	}
}
