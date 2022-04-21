package gg.xp.xivsupport.speech;

import gg.xp.reevent.events.BaseEvent;

public class TtsRequest extends BaseEvent {
	private final String ttsString;

	public TtsRequest(String ttsString) {
		this.ttsString = ttsString;
	}

	public String getTtsString() {
		return ttsString;
	}

	@Override
	public String toString() {
		return String.format("TtsRequest('%s')", ttsString);
	}
}
