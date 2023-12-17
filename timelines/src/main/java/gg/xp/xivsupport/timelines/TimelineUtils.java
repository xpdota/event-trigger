package gg.xp.xivsupport.timelines;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TimelineUtils {

	private TimelineUtils() {
	}

	public static Map<String, List<String>> cloneConditions(Map<String, List<String>> in) {
		return in.entrySet().stream().collect(Collectors.toMap(
				Map.Entry::getKey,
				e -> new ArrayList<>(e.getValue())
		));
	}

}
