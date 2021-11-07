package gg.xp.events;

public class BasicEvent extends BaseEvent {

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
