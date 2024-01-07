package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class ReflectFlagEffect extends AbilityEffect {
	protected ReflectFlagEffect(long flags, long value) {
		super(flags, value, AbilityEffectType.REFLECT_DUMMY);
	}

	@Override
	public boolean isDisplayed() {
		return false;
	}

	@Override
	protected String getBaseDescription() {
		return "Next damage effect represents reflected damage";
	}

	@Override
	public String toString() {
		return "Reflected Damage Flag";
	}
}
