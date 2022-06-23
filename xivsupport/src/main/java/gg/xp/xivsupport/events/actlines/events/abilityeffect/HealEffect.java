package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class HealEffect extends AbilityEffect {
	private final HitSeverity severity;
	private final long amount;

	public HealEffect(long flags, long value, HitSeverity severity, long amount) {
		super(flags, value, AbilityEffectType.HEAL);
		this.severity = severity;
		this.amount = amount;
	}

	public HitSeverity getSeverity() {
		return severity;
	}

	public long getAmount() {
		return amount;
	}

	@Override
	public String toString() {
		return String.format("H(%s%s)", severity.getSymbol(), amount);
	}


	@Override
	public String getBaseDescription() {
		if (severity == HitSeverity.NORMAL) {
			return String.format("Heal: %s (%s %s)", amount, getDamageAspect(), getDamageType());
		}
		else {
			return String.format("Heal: %s (%s) (%s %s)", amount, severity.getFriendlyName(), getDamageAspect(), getDamageType());
		}
	}

	public DamageAspect getDamageAspect() {
		return DamageAspect.forByte((int) (getFlags() >> 20) % 16);
	}

	public DamageType getDamageType() {
		return DamageType.forByte((int) (getFlags() >> 16) % 16);
	}

}
