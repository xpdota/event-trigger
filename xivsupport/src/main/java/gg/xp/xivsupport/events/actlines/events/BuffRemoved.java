package gg.xp.xivsupport.events.actlines.events;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.xivdata.data.StatusEffectInfo;
import gg.xp.xivdata.data.StatusEffectLibrary;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivStatusEffect;

import java.io.Serial;

/**
 * Represents a buff being removed
 */
public class BuffRemoved extends BaseEvent implements HasSourceEntity, HasTargetEntity, HasStatusEffect {
	@Serial
	private static final long serialVersionUID = -5438212467951183512L;
	private final XivStatusEffect buff;
	private final double duration;
	private final XivCombatant source;
	private final XivCombatant target;
	private final long rawStacks;
	private final long stacks;

	public BuffRemoved(XivStatusEffect buff, double duration, XivCombatant source, XivCombatant target, long rawStacks) {
		this.buff = buff;
		this.duration = duration;
		this.source = source;
		this.target = target;
		this.rawStacks = rawStacks;
		long maxStacks;
		StatusEffectInfo statusEffectInfo = StatusEffectLibrary.forId(buff.getId());
		if (statusEffectInfo == null) {
			maxStacks = 16;
		}
		else {
			maxStacks = statusEffectInfo.maxStacks();
		}
		if (rawStacks >= 0 && rawStacks <= maxStacks) {
			stacks = rawStacks;
		}
		else {
			stacks = 0;
		}
	}

	@Override
	public XivStatusEffect getBuff() {
		return buff;
	}

	public double getDuration() {
		return duration;
	}

	@Override
	public XivCombatant getSource() {
		return source;
	}

	@Override
	public XivCombatant getTarget() {
		return target;
	}

	@Override
	public long getStacks() {
		return stacks;
	}

	@Override
	public long getRawStacks() {
		return rawStacks;
	}

	@Override
	public String toString() {
		return "BuffRemoved{" +
				"buff=" + buff +
				", duration=" + duration +
				", source=" + source +
				", target=" + target +
				", stacks=" + rawStacks +
				'}';
	}
}
