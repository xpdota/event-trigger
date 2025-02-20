package gg.xp.xivsupport.events.fflogs;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.SystemEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;
import java.util.Map;

@SystemEvent
public class FflogsUnsupportedEvent extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = 4035284471223214151L;
	private final Map<String, Object> fields;
	private final String type;

	public FflogsUnsupportedEvent(FflogsRawEvent raw) {
		fields = raw.getFields();
		type = raw.type();
	}

	@Override
	public String getPrimaryValue() {
		return type;
	}

	public String getType() {
		return type;
	}
}
