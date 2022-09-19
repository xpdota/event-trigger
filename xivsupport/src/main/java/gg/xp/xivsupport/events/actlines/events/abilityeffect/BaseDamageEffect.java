package gg.xp.xivsupport.events.actlines.events.abilityeffect;

import org.jetbrains.annotations.Nullable;

public abstract class BaseDamageEffect extends AbilityEffect implements DamageEffect {

	protected final long amount;
	protected final HitSeverity severity;

	protected BaseDamageEffect(long flags, long value, long amount, HitSeverity severity, AbilityEffectType type) {
		super(flags, value, type);
		this.amount = amount;
		this.severity = severity;
	}

	protected int getRawModifierByte() {
		return (byte) (getFlags() >> 24);
	}

	@Override
	public long getAmount() {
		return amount;
	}

	@Override
	public HitSeverity getSeverity() {
		return severity;
	}

	public DamageAspect getDamageAspect() {
		return DamageAspect.forByte((int) (getFlags() >> 20) % 16);
	}

	public DamageType getDamageType() {
		return DamageType.forByte((int) (getFlags() >> 16) % 16);
	}

	protected abstract String shortName();

	protected abstract String longName();

	protected @Nullable String describeModification() {
		return null;
	}

	@Override
	public String toString() {
		return String.format("%s(%s%s)", shortName(), severity.getSymbol(), amount);
	}

	@Override
	protected String getBaseDescription() {
		StringBuilder sb = new StringBuilder(longName()).append(": ");
		sb.append(amount);
		if (severity != HitSeverity.NORMAL) {
			sb.append(' ').append(severity.getFriendlyName());
		}
		@Nullable String mod = describeModification();
		if (mod != null) {
			sb.append(" (").append(mod).append(')');
		}
		sb.append(" (").append(getDamageAspect()).append(' ').append(getDamageType()).append(')');
		return sb.toString();
	}
}
