package gg.xp.xivsupport.timelines.cbevents;

import gg.xp.xivsupport.events.actlines.events.NameIdPair;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

class CbConversions {

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
	static <X> CbConversion<X> intConv(Function<X, Long> getter, int base) {
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
	static <X> CbConversion<X> intConv(Function<X, Long> getter, int base, int minDigits) {
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

	private static final Pattern simpleString = Pattern.compile("[A-Za-z0-9-='\",~`!@#%&]*");

	static <X> CbConversion<X> strConv(Function<X, String> getter) {
		return str -> {
			if (simpleString.matcher(str).matches()) {
				return item -> Objects.equals(getter.apply(item), str);
			}
			Pattern pattern = Pattern.compile(str);
			return item -> pattern.matcher(getter.apply(item)).matches();
		};
	}

	static <X> CbConversion<X> id(Function<X, NameIdPair> getter) {
		return intConv(e -> getter.apply(e).getId(), 16);
	}

	static <X> CbConversion<X> named(Function<X, NameIdPair> getter) {
		return strConv(e -> getter.apply(e).getName());
	}

	static <X> CbConversion<X> boolToInt(Function<X, Boolean> getter) {
		return str -> switch (str) {
			case "0", "00", "000", "0000" -> (item -> !getter.apply(item));
			case "1", "01", "001", "0001" -> (getter::apply);
			default -> throw new IllegalArgumentException("Expected 0 or 1, got '%s'".formatted(str));
		};
	}
}
