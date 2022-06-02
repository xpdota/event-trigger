package gg.xp.xivsupport.speech;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Duration;

public abstract class BaseCalloutEvent extends BaseEvent implements CalloutEvent {

	private @Nullable Color colorOverride;
	private @Nullable CalloutEvent replaces;

	@Override
	public @Nullable Color getColorOverride() {
		return colorOverride;
	}

	public void setColorOverride(@Nullable Color colorOverride) {
		this.colorOverride = colorOverride;
	}

	protected Duration getTimeSinceCall() {
		// TODO: is this necessary?
		Event parent = getParent();
		if (parent instanceof BaseEvent be) {
			return be.getEffectiveTimeSince();
		}
		else {
			return getEffectiveTimeSince();
		}
	}

	@Override
	public @Nullable CalloutEvent replaces() {
		return replaces;
	}

	@Override
	public void setReplaces(CalloutEvent replaces) {
		this.replaces = replaces;
	}
}
