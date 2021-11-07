package gg.xp.speech;

import gg.xp.events.BaseEvent;

public class TtsCall extends BaseEvent {
	private final String callText;

	public TtsCall(String callText) {
		this.callText = callText;
	}

	public String getCallText() {
		return callText;
	}
}
