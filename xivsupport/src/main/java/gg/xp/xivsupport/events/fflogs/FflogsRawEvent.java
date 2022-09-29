package gg.xp.xivsupport.events.fflogs;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.HitSeverity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SystemEvent
public class FflogsRawEvent extends BaseEvent implements HasPrimaryValue {
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

	@Contract("_, _, !null -> !null")
	public @Nullable <X> X getTypedField(String key, Class<X> expectedType, @Nullable X dflt) {
		Object value = fields.get(key);
		if (value == null) {
			return dflt;
		}
		return mapper.convertValue(value, expectedType);
	}

	public @Nullable Long getHexField(String key) {
		String strVal = getTypedField(key, String.class);
		if (strVal == null || strVal.isEmpty()) {
			return null;
		}
		return Long.parseLong(strVal, 16);
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

	public @Nullable Long sourceInstance() {
		return getTypedField("sourceInstance", Long.class);
	}

	public @Nullable Long targetInstance() {
		return getTypedField("targetInstance", Long.class);
	}

	public @Nullable Long abilityId() {
		return getTypedField("abilityGameID", Long.class);
	}

	public @Nullable Long timestamp() {
		return getTypedField("timestamp", Long.class);
	}

	public HitSeverity severity() {
		boolean dhit = getTypedField("directHit", boolean.class, false);
		boolean chit = getTypedField("hitType", int.class, 1) == 2;
		return HitSeverity.of(chit, dhit);
	}

	@Override
	public String getPrimaryValue() {
		String type = type();
		return type == null ? "null" : type;
	}
}
