package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class HealEffect extends AbilityEffect {
	private final HitSeverity severity;
	private final long amount;

	public HealEffect(HitSeverity severity, long amount) {
		super(AbilityEffectType.HEAL);
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
}
