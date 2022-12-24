package gg.xp.xivsupport.timelines.intl;

import java.util.Collections;
import java.util.Map;

public record TimelineReplacements(Map<String, LanguageReplacements> langs) {

	public static TimelineReplacements empty()  {
		return new TimelineReplacements(Collections.emptyMap());
	}

}
