package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public abstract class AbilityEffect {

	protected final long flags;
	protected final long value;
	protected final AbilityEffectType effectType;

	protected AbilityEffect(long flags, long value, AbilityEffectType effectType) {
		this.flags = flags;
		this.value = value;
		this.effectType = effectType;
	}

	protected AbilityEffect(AbilityEffectType effectType) {
		this.flags = 0;
		this.value = 0;
		this.effectType = effectType;
	}

	public AbilityEffectType getEffectType() {
		return effectType;
	}

	public final long getFlags() {
		return flags;
	}

	public final long getValue() {
		return value;
	}

	protected String getBaseDescription() {
		return toString();
	};

	public final String getDescription() {
		if (flags == 0 && value == 0) {
			return getBaseDescription();
		}
		return String.format("%s (raw: %08x %08x)", getBaseDescription(), flags, value);
	}
}
