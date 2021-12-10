package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class OtherEffect extends AbilityEffect {

	private final long flags;
	private final long value;

	public OtherEffect(long flags, long value) {
		super(AbilityEffectType.OTHER);
		this.flags = flags;
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("Other(%x,%x)", flags, value);
	}
}
