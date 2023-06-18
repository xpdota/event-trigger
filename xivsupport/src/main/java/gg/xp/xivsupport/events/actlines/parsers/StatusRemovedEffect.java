package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffectType;
import gg.xp.xivsupport.models.XivStatusEffect;

public class StatusRemovedEffect extends AbilityEffect {

	private final XivStatusEffect status;
	private final int rawStacks;
	private final int stacks;

	public StatusRemovedEffect(long flags, long value, long status, int rawStacks) {
		super(flags, value, AbilityEffectType.STATUS_REMOVED);
		this.status = new XivStatusEffect(status);
		this.rawStacks = rawStacks;
		this.stacks = StatusEffectLibrary.calcActualStacks(status, rawStacks);
	}

	@Override
	public String toString() {
		return String.format("R(0x%x)", status.getId());
	}

	public XivStatusEffect getStatus() {
		return status;
	}

	public int getRawStacks() {
		return rawStacks;
	}

	public int getStacks() {
		return stacks;
	}

	@Override
	protected String getBaseDescription() {
		StatusEffectInfo sei = StatusEffectLibrary.forId(status.getId());
		String formatted = String.format("Removed Status 0x%x (%s) from Target", status.getId(), sei == null ? "Unknown" : sei.name());
		if (stacks > 0) {
			formatted += String.format(" (%s stacks)", stacks);
		}
		return formatted;
	}

}
