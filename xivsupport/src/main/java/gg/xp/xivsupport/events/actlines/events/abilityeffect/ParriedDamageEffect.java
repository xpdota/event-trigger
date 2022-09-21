package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class ParriedDamageEffect extends BaseDamageEffect implements HasDamageModifier {
	private final long amount;

	public ParriedDamageEffect(long flags, long value, long amount, HitSeverity severity) {
		super(flags, value, amount, severity, AbilityEffectType.PARRIED);
		this.amount = amount;
	}

	@Override
	public long getAmount() {
		return amount;
	}

	@Override
	protected String shortName() {
		return "Parry";
	}

	@Override
	protected String longName() {
		return "Parried";
	}

	@Override
	public int getModifier() {
		return getRawModifierByte();
	}
}
