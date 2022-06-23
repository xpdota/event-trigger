package gg.xp.xivsupport.events.fflogs;

import gg.xp.reevent.events.BaseEvent;

import java.io.Serial;
import java.util.Map;

public class FflogsUnsupportedEvent extends BaseEvent {

	@Serial
	private static final long serialVersionUID = 4035284471223214151L;
	private final Map<String, Object> fields;
	private final String type;

	public FflogsUnsupportedEvent(FflogsRawEvent raw) {
		fields = raw.getFields();
		type = raw.type();
	}

}
