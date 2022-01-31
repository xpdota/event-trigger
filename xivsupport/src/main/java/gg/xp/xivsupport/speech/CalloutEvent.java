package gg.xp.xivsupport.speech;

import gg.xp.reevent.events.Event;
import org.jetbrains.annotations.Nullable;

public interface CalloutEvent extends Event {
	@Nullable String getVisualText();

	@Nullable String getCallText();

	boolean isExpired();
}
