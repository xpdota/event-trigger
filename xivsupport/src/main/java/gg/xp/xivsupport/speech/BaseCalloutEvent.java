package gg.xp.xivsupport.speech;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.callouts.CalloutTrackingKey;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.Serial;
import java.time.Duration;

public abstract class BaseCalloutEvent extends BaseEvent implements CalloutEvent {

	@Serial
	private static final long serialVersionUID = -3290945522062716450L;
	private @Nullable Color colorOverride;
	private @Nullable HasCalloutTrackingKey replaces;
	private @Nullable CalloutTraceInfo trace;
	private boolean forceExpire;

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


	@Nullable
	@Override
	public CalloutTraceInfo getTrace() {
		return trace;
	}

	public void setTrace(CalloutTraceInfo trace) {
		this.trace = trace;
	}

	@Override
	public final boolean isExpired() {
		return forceExpire || isNaturallyExpired();
	}

	public abstract boolean isNaturallyExpired();

	@SuppressWarnings("unused") // used in scripts
	public void forceExpire() {
		this.forceExpire = true;
	}
}
