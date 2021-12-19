package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class DamageTakenEffect extends AbilityEffect implements DamageEffect {
	private final HitSeverity severity;
	private final long amount;

	public DamageTakenEffect(HitSeverity severity, long amount) {
		super(AbilityEffectType.DAMAGE);
		this.severity = severity;
		this.amount = amount;
	}

	public HitSeverity getSeverity() {
		return severity;
	}

	@Override
	public long getAmount() {
		return amount;
	}

	@Override
	public String toString() {
		return String.format("D(%s%s)", severity.getSymbol(), amount);
	}

	@Override
	public String getDescription() {
		if (severity == HitSeverity.NORMAL) {
			return String.format("Damage Taken: %s", amount);
		}
		else {
			return String.format("Damage Taken: %s (%s)", amount, severity.getFriendlyName());
		}
	}
}
