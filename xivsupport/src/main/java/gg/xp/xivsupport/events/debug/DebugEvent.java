package gg.xp.xivsupport.events.debug;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;

public class DebugEvent extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = -1850407193779049324L;
	private final Object value;

	public DebugEvent(Object value) {
		this.value = value;
	}

	@Override
	public String getPrimaryValue() {
		return value.toString();
	}

	public Object getValue() {
		return value;
	}

}
