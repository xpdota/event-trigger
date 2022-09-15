package gg.xp.xivsupport.events.misc;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;

public class EchoEvent extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = -6661787415932481263L;
	private final String line;

	public EchoEvent(String line) {
		this.line = line;
	}

	public String getLine() {
		return line;
	}

	@Override
	public String getPrimaryValue() {
		return line;
	}
}
