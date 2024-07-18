package gg.xp.xivsupport.timelines;

import java.util.HashMap;
import java.util.Map;

public class TimelineCustomizationExport {
	public Map<Long, TimelineCustomizations> timelineCustomizations;

	public static TimelineCustomizationExport empty() {
		TimelineCustomizationExport out = new TimelineCustomizationExport();
		out.timelineCustomizations = new HashMap<>();
		return out;
	}
}
