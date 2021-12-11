package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class NoEffect extends AbilityEffect {

	public NoEffect() {
		super(AbilityEffectType.NO_EFFECT);
	}

	@Override
	public String toString() {
		return "No Effect";
	}
}
