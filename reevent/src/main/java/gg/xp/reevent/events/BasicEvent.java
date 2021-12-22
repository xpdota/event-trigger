package gg.xp.reevent.events;

import java.io.Serial;

public class BasicEvent extends BaseEvent {

	@Serial
	private static final long serialVersionUID = -3964771061111399357L;
	private final String value;

	public BasicEvent(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "BasicEvent{" +
				"value='" + value + '\'' +
				'}';
	}
}
