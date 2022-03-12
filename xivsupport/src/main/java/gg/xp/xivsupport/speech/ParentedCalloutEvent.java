package gg.xp.xivsupport.speech;

import gg.xp.reevent.events.BaseEvent;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ParentedCalloutEvent<X> extends BaseEvent implements CalloutEvent {

	@Serial
	private static final long serialVersionUID = 6842512228516345067L;
	private final X event;
	private final String callText;
	private final Supplier<String> visualText;
	private final Predicate<X> expiryCheck;

	public ParentedCalloutEvent(X event, String callText, Supplier<String> visualText, Predicate<X> expiryCheck) {
		this.event = event;
		this.callText = callText;
		this.visualText = visualText;
		this.expiryCheck = expiryCheck;
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
		return expiryCheck.test(event);
	}
}
