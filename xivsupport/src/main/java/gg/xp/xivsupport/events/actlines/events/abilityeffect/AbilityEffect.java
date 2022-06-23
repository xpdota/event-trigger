package gg.xp.xivsupport.events.actlines.events.abilityeffect;

public abstract class AbilityEffect {

	private final long flags;
	private final long value;
	private final AbilityEffectType effectType;

	protected AbilityEffect(long flags, long value, AbilityEffectType effectType) {
		this.flags = flags;
		this.value = value;
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
		return String.format("%s (raw: %08x %08x)", getBaseDescription(), flags, value);
	}
}
