package gg.xp.speech;

import gg.xp.events.BaseEvent;
import gg.xp.events.Event;

public class TtsCall extends BaseEvent {
	private final String callText;

	public TtsCall(String callText) {
		this.callText = callText;
	}

	public String getCallText() {
		return callText;
	}
}
