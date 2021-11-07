package gg.xp.events.actlines.events;

import gg.xp.events.BaseEvent;
import gg.xp.events.models.XivEntity;
import gg.xp.events.models.XivStatusEffect;

// TODO: track new application vs refresh
// Note that stacks decreasing (e.g. Embolden) still counts as "Application".
public class BuffApplied extends BaseEvent implements HasSourceEntity, HasTargetEntity {
	private final XivStatusEffect buff;
	private final double duration;
	private final XivEntity source;
	private final XivEntity target;
	private final long stacks;

	public BuffApplied(XivStatusEffect buff, double duration, XivEntity source, XivEntity target, long stacks) {
		this.buff = buff;
		this.duration = duration;
		this.source = source;
		this.target = target;
		this.stacks = stacks;
	}

	public XivStatusEffect getBuff() {
		return buff;
	}

	public double getDuration() {
		return duration;
	}

	@Override
	public XivEntity getSource() {
		return source;
	}

	@Override
	public XivEntity getTarget() {
		return target;
	}

	public long getStacks() {
		return stacks;
	}
}
