package gg.xp.xivsupport.speech;

import gg.xp.reevent.events.BaseEvent;
import org.jetbrains.annotations.Nullable;

public class CalloutEvent extends BaseEvent {
	private static final long serialVersionUID = 7956006620675927571L;
	private final String callText;
	private final String visualText;

	public CalloutEvent(String callText) {
		this(callText, callText);
	}

	public CalloutEvent(String callText, String visualText) {
		this.callText = callText;
		this.visualText = visualText;
	}

	public @Nullable String getVisualText() {
		return visualText;
	}

	public @Nullable String getCallText() {
		return callText;
	}
}
