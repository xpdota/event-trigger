package gg.xp.xivsupport.timelines;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.timelines.cbevents.CbEventType;
import gg.xp.xivsupport.timelines.intl.LanguageReplacements;

import java.util.List;
import java.util.Map;

public interface EventSyncController {
	boolean shouldSync(Event event);

	Class<? extends Event> eventType();

	EventSyncController translateWith(LanguageReplacements replacements);

	String toTextFormat();

	@JsonProperty("type")
	CbEventType getType();

	@JsonProperty("conditions")
	Map<String, List<String>> getRawConditions();
}
