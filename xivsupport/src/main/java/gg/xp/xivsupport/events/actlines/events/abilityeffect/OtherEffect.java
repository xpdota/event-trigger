package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class OtherEffect extends AbilityEffect {

	public OtherEffect(long flags, long value) {
		super(flags, value, AbilityEffectType.OTHER);
	}

	@Override
	public String toString() {
		return String.format("Other(%x,%x)", getFlags(), getValue());
	}

	@Override
	protected String getBaseDescription() {
		return "Other";
	}
}
