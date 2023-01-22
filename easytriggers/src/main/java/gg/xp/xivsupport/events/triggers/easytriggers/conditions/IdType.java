package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// Remember to add stuff to IdPickerFactory
@Retention(RetentionPolicy.RUNTIME)
public @interface IdType {
	/**
	 * @return The class of item that the ID corresponds to.
	 */
	Class<?> value();

	/**
	 * @return True if a mapping from the given ID to a concrete item is required. False if you
	 * want to accept non-matched items.
	 */
	boolean matchRequired() default true;
}
