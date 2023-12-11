package gg.xp.xivsupport.timelines.cbevents;

import java.util.function.Predicate;

/**
 * Represents a conversion from some field (possibly nested) on an event, to a predicate that matches events.
 * All cactbot netregices use string values regardless of the underlying data type, so this always takes a string.
 *
 * @param <X> The event type.
 */
@FunctionalInterface
interface CbConversion<X> {
	/**
	 * Example: on a 21-line, we want to check if the ability ID is "12AB".
	 * We would call this with "12AB" as the argument, and it should return a predicate that checks that
	 * a given AbilityUsedEvent has an ability ID of 0x12AB.
	 *
	 * @param input The input string.
	 * @return The resulting predicate.
	 */
	Predicate<X> convert(String input);
}
