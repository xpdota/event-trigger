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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

	@SuppressWarnings("unchecked")
	public static EventSyncController parseRaw(String type, String conditionsRaw) {
		CbEventType eventDef = CbEventType.valueOf(type);
		// The values can be initially specified as either strings, or lists of strings
		Map<String, List<String>> conditions;
		try {
			Map<String, Object> conditionsAny = mapper.readValue(conditionsRaw, new TypeReference<>() {
			});
			conditions = conditionsAny.entrySet().stream()
					.collect(Collectors.toMap(
							Map.Entry::getKey,
							e -> {
								Object value = e.getValue();
								if (value instanceof List<?> listVal) {
									for (Object listItem : listVal) {
										if (listItem instanceof String) {
											continue;
										}
										throw new IllegalArgumentException("Non-string item in list! '%s'".formatted(listItem));
									}
									return (List<String>) listVal;
								}
								else if (value instanceof String strVal) {
									return Collections.singletonList(strVal);
								}
								else {
									throw new IllegalArgumentException("Expected String or List<String>, got '%s'".formatted(value));
								}
							}
					));

		}
		catch (Throwable e) {
			throw new RuntimeException("Error reading JSON: " + conditionsRaw, e);
		}
		Predicate<Event> condition = eventDef.make(conditions);
		return new FileEventSyncController(eventDef.eventType(), condition, eventDef, type, conditions);
	}

	public static EventSyncController parse(CbEventType eventDef, Map<String, List<String>> conditions) {
		Predicate<Event> condition = eventDef.make(conditions);
		return new FileEventSyncController(eventDef.eventType(), condition, eventDef, eventDef.name(), conditions);
	}

	public static Map<String, Object> flattenListMap(Map<String, List<String>> originalValues) {
		return originalValues.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> {
							List<String> value = e.getValue();
							if (value.size() == 1) {
								return value.get(0);
							}
							return value;
						}
				));
	}

	public static String format(String originalType, Map<String, List<String>> originalValues) {

		// Convert single values back to non-list form, keep multi-valued things as lists
		Map<String, Object> reformattedValues = flattenListMap(originalValues);
		try {
			return String.format("%s %s", originalType, mapper.writeValueAsString(reformattedValues));
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
