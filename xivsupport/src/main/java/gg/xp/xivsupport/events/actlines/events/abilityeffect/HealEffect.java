package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class HealEffect extends AbilityEffect implements HasSeverity {
	private final HitSeverity severity;
	private final long amount;
	private final boolean onTarget;

	public HealEffect(long flags, long value, HitSeverity severity, long amount) {
		super(flags, value, AbilityEffectType.HEAL);
		this.severity = severity;
		this.amount = amount;
		onTarget = ((flags >> 8) & 1) == 0;
	}

	@Override
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
		String target = isOnTarget() ? "Target" : "Caster";
		if (severity == HitSeverity.NORMAL) {
			return String.format("Healed %s: %s", target, amount);
		}
		else {
			return String.format("Healed %s: %s (%s)", target, amount, severity.getFriendlyName());
		}
	}

	public boolean isOnTarget() {
		return onTarget;
	}

}
