package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class DamageEffect extends AbilityEffect {
	private final HitSeverity severity;
	private final long amount;

	public DamageEffect(HitSeverity severity, long amount) {
		super(AbilityEffectType.DAMAGE);
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
		return String.format("D(%s%s)", severity.getSymbol(), amount);
	}
}
