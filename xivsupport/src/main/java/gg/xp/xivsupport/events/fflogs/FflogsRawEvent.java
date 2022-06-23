package gg.xp.xivsupport.events.fflogs;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SystemEvent
public class FflogsRawEvent extends BaseEvent {
	private static final ObjectMapper mapper = new ObjectMapper();
	@Serial
	private static final long serialVersionUID = -2965533757777832824L;
	private final Map<String, Object> fields;

	public FflogsRawEvent(Map<String, Object> fields) {
		this.fields = new HashMap<>(fields);
	}

	public Map<String, Object> getFields() {
		return Collections.unmodifiableMap(fields);
	}

	public @Nullable Object getField(String key) {
		return fields.get(key);
	}

	public @Nullable <X> X getTypedField(String key, Class<X> expectedType) {
		return getTypedField(key, expectedType, null);
	}

	public @Nullable <X> X getTypedField(String key, Class<X> expectedType, @Nullable X dflt) {
		Object value = fields.get(key);
		if (value == null) {
			return dflt;
		}
		return mapper.convertValue(value, expectedType);
	}

	// Some common fields
	public @Nullable String type() {
		return getTypedField("type", String.class);
	}

	public @Nullable Long sourceID() {
		return getTypedField("sourceID", Long.class);
	}

	public @Nullable Long targetID() {
		return getTypedField("targetID", Long.class);
	}

	public @Nullable Long abilityId() {
		return getTypedField("abilityGameID", Long.class);
	}

	public @Nullable Long timestamp() {
		return getTypedField("timestamp", Long.class);
	}
}
