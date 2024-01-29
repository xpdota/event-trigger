package gg.xp.xivsupport.events.actlines.events.abilityeffect;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.models.XivStatusEffect;

public class StatusNoEffect extends AbilityEffect {
	private final XivStatusEffect status;
	private final int rawStacks;
	private final int stacks;

	// TODO: target
	public StatusNoEffect(long flags, long value, long id, int rawStacks) {
		super(flags, value, AbilityEffectType.STATUS_NO_EFFECT);
		this.status = new XivStatusEffect(id, "");
		this.rawStacks = rawStacks;
		this.stacks = StatusEffectLibrary.calcActualStacks(id, rawStacks);
	}

	public int getStacks() {
		return stacks;
	}

	public int getRawStacks() {
		return rawStacks;
	}

	@Override
	public String toString() {
		return String.format("SNE(%s)", status.getId());
	}

	@Override
	public String getBaseDescription() {
		StatusEffectInfo sei = StatusEffectLibrary.forId(status.getId());
		String formatted = String.format("Status 0x%x (%s) has no effect", status.getId(), sei == null ? "Unknown" : sei.name());
		if (stacks > 0) {
			formatted += String.format(" (%s stacks)", stacks);
		}
		return formatted;
	}

	public XivStatusEffect getStatus() {
		return status;
	}
}
