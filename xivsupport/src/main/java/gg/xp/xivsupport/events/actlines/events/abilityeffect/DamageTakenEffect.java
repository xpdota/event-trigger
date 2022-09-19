package gg.xp.xivsupport.events.actlines.events.abilityeffect;

import org.jetbrains.annotations.Nullable;

public class DamageTakenEffect extends BaseDamageEffect {
	public DamageTakenEffect(long flags, long value, HitSeverity severity, long amount) {
		super(flags, value, amount, severity, AbilityEffectType.DAMAGE);
	}

	@Override
	protected String shortName() {
		return "D";
	}

	@Override
	protected String longName() {
		return "Damage Taken";
	}

	@Override
	protected @Nullable String describeModification() {
		int cb = getComboBonus();
		if (cb != 0) {
			return "+" + cb + "%s from combo/positional";
		}
		else {
			return null;
		}
	}

	public int getComboBonus() {
		return getRawModifierByte();
	}
}
