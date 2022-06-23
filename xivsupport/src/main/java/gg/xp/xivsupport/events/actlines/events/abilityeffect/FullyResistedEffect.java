package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class FullyResistedEffect extends AbilityEffect {

	public FullyResistedEffect(long flags, long value) {
		super(flags, value, AbilityEffectType.MISS);
	}

	@Override
	public String toString() {
		return "Fully Resisted";
	}
}
