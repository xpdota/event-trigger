package gg.xp.telestosupport;

import com.fasterxml.jackson.databind.JsonNode;
import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;

import java.io.Serial;
import java.util.Collections;
import java.util.Map;

public class TelestoResponse extends BaseEvent {
	@Serial
	private static final long serialVersionUID = 4158223908340512712L;
	private final Map<String, Object> data;
	private final long id;

	public TelestoResponse(Map<String, Object> data) {
		this.data = data;
		id = Long.parseLong(data.get("id").toString());
	}

	public Map<String, Object> getData() {
		return Collections.unmodifiableMap(data);
	}

	public long getId() {
		return id;
	}

	public Object getResponse() {
		return data.get("response");
	}
}
