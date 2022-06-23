package gg.xp.xivsupport.speech;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;

import java.io.Serial;

public class TtsRequest extends BaseEvent implements HasPrimaryValue {
	@Serial
	private static final long serialVersionUID = -9029735608941333470L;
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

	@Override
	public String getPrimaryValue() {
		return String.valueOf(ttsString);
	}
}
