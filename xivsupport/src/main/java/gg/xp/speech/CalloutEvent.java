package gg.xp.speech;

import gg.xp.events.BaseEvent;

public class CalloutEvent extends BaseEvent {
	private static final long serialVersionUID = 7956006620675927571L;
	private final String callText;

	public CalloutEvent(String callText) {
		this.callText = callText;
	}

	public String getCallText() {
		return callText;
	}
}
