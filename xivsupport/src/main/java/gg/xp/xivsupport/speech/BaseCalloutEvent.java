package gg.xp.xivsupport.speech;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.callouts.CalloutTrackingKey;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.Duration;

public abstract class BaseCalloutEvent extends BaseEvent implements CalloutEvent {

	private @Nullable Color colorOverride;
	private @Nullable HasCalloutTrackingKey replaces;

	private final CalloutTrackingKey key;

	protected BaseCalloutEvent() {
		this.key = new CalloutTrackingKey();
	}

	protected BaseCalloutEvent(CalloutTrackingKey key) {
		this.key = key;
	}

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
	public @Nullable HasCalloutTrackingKey replaces() {
		return replaces;
	}

	@Override
	public void setReplaces(@Nullable HasCalloutTrackingKey replaces) {
		this.replaces = replaces;
	}

	@Override
	public CalloutTrackingKey trackingKey() {
		return key;
	}
}
