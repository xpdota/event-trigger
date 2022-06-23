package gg.xp.xivdata.data;

import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public final class ActionUtils {
	private ActionUtils() {
	}

	/**
	 * Given a name, if there are Roman numerals at the end, truncate them
	 *
	 * @param originalName The original name
	 * @return The truncated name
	 */
	public static String adjustName(String originalName) {
		if (originalName.endsWith(" II") || originalName.endsWith(" IV")) {
			return originalName.substring(0, originalName.length() - 3);
		}
		else if (originalName.endsWith(" III")) {
			return originalName.substring(0, originalName.length() - 4);
		}
		else {
			return originalName;
		}
	}

	/**
	 * A function that, given names, will keep the first name intact, but chop everything other than the Roman numerals
	 * at the end off of subsequent names.
	 *
	 * @return A function that returns the original name, or the Roman numerals
	 */
	public static Function<String, String> adjustNameReverse() {
		AtomicBoolean afterFirst = new AtomicBoolean();
		return originalName -> {
			boolean truncate = afterFirst.compareAndExchange(false, true);
			if (!truncate) {
				return originalName;
			}
			if (originalName.endsWith(" II") || originalName.endsWith(" IV")) {
				return originalName.substring(0, originalName.length() - 3);
			}
			else if (originalName.endsWith(" III")) {
				return originalName.substring(0, originalName.length() - 4);
			}
			else {
				return originalName;
			}
		};
	}
}
