package gg.xp.xivsupport.speech;

import gg.xp.xivsupport.callouts.CalloutTrackingKey;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.Duration;

public class BasicCalloutEvent extends BaseCalloutEvent {
	@Serial
	private static final long serialVersionUID = 7956006620675927571L;
	private final String callText;
	private final String visualText;
	private final Duration hangTime;
	// TODO lower this
	private static final long DEFAULT_DURATION = 15000;

	public BasicCalloutEvent(String callText) {
		this(callText, callText, DEFAULT_DURATION);
	}

	public BasicCalloutEvent(String callText, String visualText) {
		this(callText, visualText, DEFAULT_DURATION);
	}

	public BasicCalloutEvent(String callText, String visualText, long hangTime) {
		this.callText = callText;
		this.visualText = visualText;
		// TODO: when I get fake time implemented, this will need to be changed
		this.hangTime = Duration.ofMillis(hangTime);
	}

	@Override
	public @Nullable String getVisualText() {
		return visualText;
	}

	@Override
	public @Nullable String getCallText() {
		return callText;
	}

	@Override
	public boolean isExpired() {
		return getTimeSinceCall().compareTo(hangTime) > 0;
	}

}
