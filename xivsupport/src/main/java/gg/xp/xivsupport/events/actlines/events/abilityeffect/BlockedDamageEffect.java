package gg.xp.xivsupport.events.actlines.events.abilityeffect;

import org.jetbrains.annotations.Nullable;

public class BlockedDamageEffect extends BaseDamageEffect implements HasDamageModifier {

	public BlockedDamageEffect(long flags, long value, long amount, HitSeverity severity) {
		super(flags, value, amount, severity, AbilityEffectType.BLOCKED);
	}

	@Override
	protected String shortName() {
		return "Block";
	}

	@Override
	protected String longName() {
		return "Blocked Damage";
	}

	@Override
	public int getModifier() {
		return getRawModifierByte();
	}

	@Override
	protected @Nullable String describeModification() {
		return "%d%% blocked".formatted(getModifier());
	}
}
