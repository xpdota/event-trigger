package gg.xp.xivsupport.events.actlines.parsers;

import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffect;
import gg.xp.xivsupport.events.actlines.events.abilityeffect.AbilityEffectType;
import gg.xp.xivsupport.models.XivStatusEffect;

public class StatusRemovedEffect extends AbilityEffect {

	private final XivStatusEffect status;

	public StatusRemovedEffect(long flags, long value, long status) {
		super(flags, value, AbilityEffectType.STATUS_REMOVED);
		this.status = new XivStatusEffect(status);
	}

	@Override
	public String toString() {
		return String.format("R(0x%x)", status.getId());
	}

	public XivStatusEffect getStatus() {
		return status;
	}

	@Override
	protected String getBaseDescription() {
		StatusEffectInfo sei = StatusEffectLibrary.forId(status.getId());
		return String.format("Removed Status 0x%x (%s) from Target", status.getId(), sei == null ? "Unknown" : sei.name());
	}

}
