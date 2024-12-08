package gg.xp.xivsupport.events.actlines.events.abilityeffect;

import gg.xp.xivdata.data.StatusEffectInfo;
import gg.xp.xivdata.data.StatusEffectLibrary;
import gg.xp.xivsupport.models.XivStatusEffect;

import java.io.Serial;
import java.io.Serializable;

public class StatusAppliedEffect extends AbilityEffect implements Serializable {

	@Serial
	private static final long serialVersionUID = -8307994316154164152L;
	private final XivStatusEffect status;
	private final int rawStacks;
	private final int stacks;
	private final boolean onTarget;

	public StatusAppliedEffect(long flags, long value, long id, int rawStacks, boolean onTarget) {
		super(flags, value, AbilityEffectType.APPLY_STATUS);
		// TODO: get actual name
		this.status = new XivStatusEffect(id, "");
		this.rawStacks = rawStacks;
		this.onTarget = onTarget;
		this.stacks = StatusEffectLibrary.calcActualStacks(id, rawStacks);

	}

	@Override
	public String toString() {
		if (isOnTarget()) {
			return String.format("S(0x%x)", status.getId());
		}
		else {
			return String.format("Sb(0x%x)", status.getId());
		}
	}

	public XivStatusEffect getStatus() {
		return status;
	}

	public boolean isOnTarget() {
		return onTarget;
	}

	public int getRawStacks() {
		return rawStacks;
	}

	public int getStacks() {
		return stacks;
	}

	@Override
	public String getBaseDescription() {
		StatusEffectInfo sei = StatusEffectLibrary.forId(status.getId());
		String formatted = String.format("Applied Status 0x%x (%s) to %s (params: %s)", status.getId(), sei == null ? "Unknown" : sei.name(), onTarget ? "Target" : "Caster", getPreAppFlagsFormatted());
		if (stacks > 0) {
			formatted += String.format(" (%s stacks)", stacks);
		}
		return formatted;
	}

	public String getPreAppFlagsFormatted() {
		long flags = getFlags();
		byte param1 = (byte) (flags >> 24);
		byte param2 = (byte) (flags >> 16);
		byte param3 = (byte) (flags >> 8);
		return String.format("%s %s %s", param1, param2, param3);
	}

	public boolean buffIdMatches(long... expected) {
		long id = status.getId();
		for (long e : expected) {
			if (e == id) {
				return true;
			}
		}
		return false;
	}
}
