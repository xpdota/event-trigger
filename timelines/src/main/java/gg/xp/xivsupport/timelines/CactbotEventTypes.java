package gg.xp.xivsupport.timelines;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ChatLineEvent;
import gg.xp.xivsupport.events.actlines.events.NameIdPair;
import gg.xp.xivsupport.events.actlines.events.SystemLogMessageEvent;
import gg.xp.xivsupport.events.state.InCombatChangeEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public enum CactbotEventTypes {

	GameLog(ChatLineEvent.class, Map.of(
			"code", intConv(ChatLineEvent::getCode, 16),
			"line", strConv(ChatLineEvent::getLine),
			// TODO
			"message", strConv(ChatLineEvent::getLine),
			"echo", strConv(ChatLineEvent::getLine),
			"dialog", strConv(ChatLineEvent::getLine)
	)),
	StartsUsing(AbilityUsedEvent.class, Map.of(
			"sourceId", id(AbilityUsedEvent::getSource),
			"source", name(AbilityUsedEvent::getSource),
			"targetId", id(AbilityUsedEvent::getTarget),
			"target", name(AbilityUsedEvent::getTarget),
			"id", id(AbilityUsedEvent::getAbility),
			"ability", name(AbilityUsedEvent::getAbility)
	)),
	Ability(AbilityUsedEvent.class, Map.of(
			"sourceId", id(AbilityUsedEvent::getSource),
			"source", name(AbilityUsedEvent::getSource),
			"targetId", id(AbilityUsedEvent::getTarget),
			"target", name(AbilityUsedEvent::getTarget),
			"id", id(AbilityUsedEvent::getAbility),
			"ability", name(AbilityUsedEvent::getAbility)
	)),
	InCombat(InCombatChangeEvent.class, Map.of(
			// TODO: kind of fake
			"inACTCombat", boolToInt(InCombatChangeEvent::isInCombat),
			"inGameCombat", boolToInt(InCombatChangeEvent::isInCombat)
	)),
	SystemLogMessage(SystemLogMessageEvent.class, Map.of(
			"instance", intConv(SystemLogMessageEvent::getUnknown, 16),
			"id", intConv(SystemLogMessageEvent::getId, 16),
			"param0", intConv(SystemLogMessageEvent::getParam0, 16),
			"param1", intConv(SystemLogMessageEvent::getParam1, 16),
			"param2", intConv(SystemLogMessageEvent::getParam2, 16)
	))


	// TODO: the rest of the events
	;


	private final Holder<?> data;

	<X extends Event> CactbotEventTypes(Class<X> eventType, Map<String, ConvToCondition<? super X>> condMap) {
		this.data = new Holder<>(eventType, condMap);
	}

	public Class<? extends Event> eventType() {
		return data.eventType;
	}

	/**
	 * Represents a conversion from some field (possibly nested) on an event, to a predicate that matches events.
	 * All cactbot netregices use string values regardless of the underlying data type, so this always takes a string.
	 *
	 * @param <X> The event type.
	 */
	@FunctionalInterface
	private interface ConvToCondition<X> {
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

	/**
	 * Make a combined predicate based on the map of values.
	 *
	 * @param values The values
	 * @return The combined predicate
	 */
	public Predicate<Event> make(Map<String, String> values) {
		return this.data.make(values);
	}

	/**
	 * Convenience function for quickly making a ConvToCondition on an integer/long field.
	 * If the input string in the resulting ConvToCondition is a plain number (and not something that would require
	 * us to actually do regex), then the numbers will be compared directly.
	 *
	 * @param getter A function for getting the required value out of our event.
	 * @param base   The numerical base, typically 10 or 16
	 * @param <X>    The event type
	 * @return The condition matching the above requirements.
	 */
	private static <X> ConvToCondition<X> intConv(Function<X, Long> getter, int base) {
		return intConv(getter, base, 0);
	}

	/**
	 * Convenience function for quickly making a ConvToCondition on an integer/long field.
	 * If the input string in the resulting ConvToCondition is a plain number (and not something that would require
	 * us to actually do regex), then the numbers will be compared directly.
	 * <p>
	 * This version of the method allows you to specify that the number should be left-padded to a minimum number of
	 * characters, with zeroes. e.g. if the input is "00", and the value is "0", then in order for that to match, you
	 * would need to specify minDigits == 2.
	 *
	 * @param getter    A function for getting the required value out of our event.
	 * @param base      The numerical base, typically 10 or 16
	 * @param minDigits If ACT would left-pad the number with zeroes, then you should specify the minimum length
	 *                  of the number here so that the value can be similarly padded out.
	 * @param <X>       The event type
	 * @return The condition matching the above requirements.
	 */
	private static <X> ConvToCondition<X> intConv(Function<X, Long> getter, int base, int minDigits) {
		return str -> {
			try {
				// Fast path - input is a number literal, so do a direct number comparison
				long parsed = Long.parseLong(str, base);
				return item -> getter.apply(item) == parsed;
			}
			catch (NumberFormatException ignored) {
				// Slow path - input is a regex, so compile to regex first
				Pattern pattern = Pattern.compile(str, Pattern.CASE_INSENSITIVE);
				return item -> {
					String asString = Long.toString(getter.apply(item), base);
					if (minDigits > 1) {
						asString = StringUtils.leftPad(asString, minDigits, '0');
					}
					return pattern.matcher(asString).matches();
				};
			}
		};
	}

	private static <X> ConvToCondition<X> strConv(Function<X, String> getter) {
		return str -> {
			Pattern pattern = Pattern.compile(str);
			return item -> pattern.matcher(getter.apply(item)).matches();
		};
	}

	private static <X> ConvToCondition<X> id(Function<X, NameIdPair> getter) {
		return intConv(e -> getter.apply(e).getId(), 16);
	}

	private static <X> ConvToCondition<X> name(Function<X, NameIdPair> getter) {
		return strConv(e -> getter.apply(e).getName());
	}

	private static <X> ConvToCondition<X> boolToInt(Function<X, Boolean> getter) {
		return str -> switch (str) {
			case "0" -> (item -> !getter.apply(item));
			case "1" -> (getter::apply);
			default -> throw new IllegalArgumentException("Expected 0 or 1, got '%s'".formatted(str));
		};
	}


	private static class Holder<X extends Event> {
		private static final Logger log = LoggerFactory.getLogger(CactbotEventTypes.class);
		private final Class<X> eventType;
		private final Map<String, ConvToCondition<? super X>> condMap;

		Holder(Class<X> eventType, Map<String, ConvToCondition<? super X>> condMap) {
			this.eventType = eventType;
			this.condMap = condMap;
		}


		public Predicate<Event> make(Map<String, String> values) {
			Predicate<X> combined = eventType::isInstance;
			for (var entry : values.entrySet()) {
				ConvToCondition<? super X> convToCondition = this.condMap.get(entry.getKey());
				if (convToCondition == null) {
					throw new IllegalArgumentException("Unknown condition: " + entry);
				}
				Predicate<? super X> converted = convToCondition.convert(entry.getValue());
				combined = combined.and(converted);
			}
			//noinspection unchecked - the first check is the type check
			return (Predicate<Event>) combined;
		}
	}
}
