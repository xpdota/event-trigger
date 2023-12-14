package gg.xp.xivsupport.timelines.cbevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.timelines.EventSyncController;
import gg.xp.xivsupport.timelines.FileEventSyncController;

import java.util.Map;
import java.util.function.Predicate;

public final class CbEventFmt {

	private static final ObjectMapper mapper = JsonMapper.builder()
			// get as close as possible to json5
			.configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES, true)
			.configure(JsonReadFeature.ALLOW_TRAILING_COMMA, true)
			.configure(JsonReadFeature.ALLOW_SINGLE_QUOTES, true)
			.configure(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
			.configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS, true)
			.configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
			.configure(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS, true)
			.configure(JsonWriteFeature.QUOTE_FIELD_NAMES, false)
			.build();

	private CbEventFmt() {
	}

	public static EventSyncController parseRaw(String type, String conditionsRaw) {
		CbEventTypes eventDef = CbEventTypes.valueOf(type);
		Map<String, String> conditions;
		try {
			conditions = mapper.readValue(conditionsRaw, new TypeReference<>() {
			});
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Error reading JSON: " + conditionsRaw, e);
		}
		Predicate<Event> condition = eventDef.make(conditions);
		return new FileEventSyncController(eventDef.eventType(), condition, type, conditions);
	}

	public static EventSyncController parse(String type, Map<String, String> conditions) {
		CbEventTypes eventDef = CbEventTypes.valueOf(type);
		Predicate<Event> condition = eventDef.make(conditions);
		return new FileEventSyncController(eventDef.eventType(), condition, type, conditions);
	}

	public static String format(String originalType, Map<String, String> originalValues) {
		try {
			return String.format("%s %s", originalType, mapper.writeValueAsString(originalValues));
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
