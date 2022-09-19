package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public class InvulnBlockedDamageEffect extends BaseDamageEffect {

	public InvulnBlockedDamageEffect(long flags, long value, long amount, HitSeverity severity) {
		super(flags, value, amount, severity, AbilityEffectType.INVULN);
	}

	@Override
	protected String shortName() {
		return "Invuln";
	}

	@Override
	protected String longName() {
		return "Invulnerable";
	}
}
