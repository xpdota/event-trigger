package gg.xp.xivsupport.speech;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public class DynamicCalloutEvent extends BaseCalloutEvent {

	private final String callText;
	private final Supplier<String> visualText;
	private final Duration hangTime;

	public DynamicCalloutEvent(String callText, Supplier<String> visualText, long hangTime) {
		this.callText = callText;
		this.visualText = visualText;
		this.hangTime = Duration.ofMillis(hangTime);
	}

	@Override
	public @Nullable String getVisualText() {
		return visualText.get();
	}

	@Override
	public @Nullable String getCallText() {
		// TTS text does not need to be dynamic since it only happens once
		return callText;
	}

	@Override
	public boolean isExpired() {
		return getTimeSinceCall().compareTo(hangTime) > 0;
	}

}
