package gg.xp.xivsupport.events.triggers.util;

import gg.xp.reevent.events.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record CalloutInitialValues(long ms, String tts, String text, @Nullable Event event) implements HasEvent {
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CalloutInitialValues that = (CalloutInitialValues) o;
		return ms == that.ms && Objects.equals(tts, that.tts) && Objects.equals(text, that.text);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ms, tts, text);
	}

	@Override
	public String toString() {
		if (event == null) {
			return String.format("call(%s, \"%s\", \"%s\")", ms, tts, text);
		}
		else {
			return String.format("call(%s, \"%s\", \"%s\") // from %s", ms, tts, text, event);

		}
	}
}
