package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class NoEffect extends AbilityEffect {

	public NoEffect(long flags, long value) {
		super(flags, value, AbilityEffectType.NO_EFFECT);
	}

	@Override
	public String toString() {
		return "No Effect";
	}
}
