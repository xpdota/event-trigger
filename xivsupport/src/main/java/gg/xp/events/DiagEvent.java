package gg.xp.events;

public class DiagEvent extends BaseEvent {
	private final String text;
	private final int source;

	public DiagEvent(String text, int source) {
		this.text = text;
		this.source = source;
	}

	public String getText() {
		return text;
	}

	public int getSource() {
		return source;
	}
}
