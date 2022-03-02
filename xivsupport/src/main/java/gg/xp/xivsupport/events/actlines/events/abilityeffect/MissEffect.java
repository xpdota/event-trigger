package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class MissEffect extends AbilityEffect {

	public MissEffect(long flags, long value) {
		super(flags, value, AbilityEffectType.MISS);
	}

	@Override
	public String toString() {
		return "Miss";
	}
}
