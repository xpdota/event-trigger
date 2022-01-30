package gg.xp.xivsupport.speech;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.BasicEvent;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.function.Supplier;

public class DynamicCalloutEvent extends BaseEvent implements CalloutEvent {

	private final String callText;
	private final Supplier<String> visualText;
	private final Instant expiresAt;

	public DynamicCalloutEvent(String callText, Supplier<String> visualText, long hangTime) {
		this.callText = callText;
		this.visualText = visualText;
		expiresAt = Instant.now().plusMillis(hangTime);
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
		return expiresAt.isBefore(Instant.now());
	}
}
