package gg.xp.speech;

import gg.xp.events.Event;

public class TtsCall implements Event {
	private final String callText;

	public TtsCall(String callText) {
		this.callText = callText;
	}

	public String getCallText() {
		return callText;
	}
}
