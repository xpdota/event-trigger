package gg.xp.xivsupport.speech;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.events.actlines.events.HasPrimaryValue;
import org.jetbrains.annotations.Nullable;

public interface CalloutEvent extends Event, HasPrimaryValue {
	@Nullable String getVisualText();

	@Nullable String getCallText();

	boolean isExpired();

	@Override
	default String getPrimaryValue() {
		return getCallText();
	}

	@Nullable CalloutEvent replaces();

	void setReplaces(CalloutEvent replaces);
}
