package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class DamageTakenEffect extends AbilityEffect implements DamageEffect, HasSeverity {
	private final HitSeverity severity;
	private final long amount;

	public DamageTakenEffect(long flags, long value, HitSeverity severity, long amount) {
		super(flags, value, AbilityEffectType.DAMAGE);
		this.severity = severity;
		this.amount = amount;
	}

	@Override
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
	public String getBaseDescription() {
		StringBuilder sb = new StringBuilder("Damage Taken: ");
		sb.append(amount);
		if (severity != HitSeverity.NORMAL) {
			sb.append(' ').append(severity.getFriendlyName());
		}
		int cb = getComboBonus();
		if (cb != 0) {
			sb.append(" (").append(cb).append("% from combo/positional)");
		}
		sb.append(" (").append(getDamageAspect()).append(' ').append(getDamageType()).append(')');
		return sb.toString();
	}

	public int getComboBonus() {
		return (byte) (getFlags() >> 24);
	}

	public DamageAspect getDamageAspect() {
		return DamageAspect.forByte((int) (getFlags() >> 20) % 16);
	}

	public DamageType getDamageType() {
		return DamageType.forByte((int) (getFlags() >> 16) % 16);
	}
}
