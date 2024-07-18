package gg.xp.xivsupport.timelines;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.timelines.cbevents.CbEventFmt;
import gg.xp.xivsupport.timelines.cbevents.CbEventType;
import gg.xp.xivsupport.timelines.intl.LanguageReplacements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomEventSyncController implements EventSyncController {

	private final Class<? extends Event> eventClass;
	private Map<String, List<String>> conditions;
	@JsonIgnore
	private EventSyncController wrapped;
	@JsonIgnore
	private final CbEventType eventType;

	@JsonCreator
	public CustomEventSyncController(@JsonProperty("type") CbEventType eventType, @JsonProperty("conditions") Map<String, List<String>> conditions) {
		this.eventType = eventType;
		this.eventClass = eventType.eventType();
		this.conditions = new HashMap<>(conditions);
		recalc();
	}

	public static CustomEventSyncController from(EventSyncController other) {
		return new CustomEventSyncController(other.getType(), other.getRawConditions());
	}

	private void recalc() {
		wrapped = CbEventFmt.parse(eventType, conditions);
	}

	public void setConditions(Map<String, List<String>> conditions) {
		this.conditions = TimelineUtils.cloneConditions(conditions);
		recalc();
	}

	@Override
	public boolean shouldSync(Event event) {
		return wrapped.shouldSync(event);
	}

	@Override
	public Class<? extends Event> eventType() {
		return eventClass;
	}

	@Override
	public EventSyncController translateWith(LanguageReplacements replacements) {
		// No translation for custom entries
		return this;
	}

	@Override
	public String toTextFormat() {
		return wrapped.toTextFormat();
	}

	@Override
	public CbEventType getType() {
		return eventType;
	}

	@Override
	public Map<String, List<String>> getRawConditions() {
		return TimelineUtils.cloneConditions(conditions);
	}

	@Override
	public String toString() {
		// TODO: there should be something to differentiate this from a builtin
		return wrapped.toString();
	}
}
