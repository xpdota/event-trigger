package gg.xp.xivsupport.events.ws;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;

public class ActWsVersionEvent extends BaseEvent implements HasPrimaryValue {

	@Serial
	private static final long serialVersionUID = 1965983177533519092L;
	private final String version;

	public ActWsVersionEvent(String version) {
		this.version = version;
	}

	public String getVersion() {
		return version;
	}

	@Override
	public String getPrimaryValue() {
		return version;
	}
}
