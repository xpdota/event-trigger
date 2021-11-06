package gg.xp.events;

public class DiagEvent implements Event {
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
