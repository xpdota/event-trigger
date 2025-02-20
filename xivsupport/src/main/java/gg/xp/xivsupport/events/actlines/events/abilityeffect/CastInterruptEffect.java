package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class CastInterruptEffect extends AbilityEffect {
	public CastInterruptEffect(long flags, long value) {
		super(flags, value, AbilityEffectType.INTERRUPT_CAST);
	}

	@Override
	protected String getBaseDescription() {
		return "Interrupted";
	}

	@Override
	public String toString() {
		return "Interrupt";
	}
}
