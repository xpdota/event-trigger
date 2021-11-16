package gg.xp.xivsupport.events;

import gg.xp.reevent.events.BaseEvent;

public class DiagEvent extends BaseEvent {
	private static final long serialVersionUID = 103691663668521872L;
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
