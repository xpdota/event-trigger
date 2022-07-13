package gg.xp.xivsupport.models;

import gg.xp.xivdata.data.Cooldown;
import gg.xp.xivdata.data.ExtendedCooldownDescriptor;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;

import java.util.Objects;

public final class CdTrackingKey {
	private final XivCombatant source;
	private final XivAbility ability;
	private final ExtendedCooldownDescriptor cooldown;

	private CdTrackingKey(XivCombatant source, XivAbility ability, ExtendedCooldownDescriptor cooldown) {
		this.source = source;
		this.ability = ability;
		this.cooldown = cooldown;
	}

	public static <X extends HasSourceEntity & HasAbility> CdTrackingKey of(X event, ExtendedCooldownDescriptor cooldown) {
		return new CdTrackingKey(event.getSource(), event.getAbility(), cooldown);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CdTrackingKey that = (CdTrackingKey) o;
		return Objects.equals(source, that.source) && Objects.equals(cooldown, that.cooldown);
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, cooldown);
	}

	public XivCombatant getSource() {
		return source;
	}

	public XivAbility getAbility() {
		return ability;
	}

	public ExtendedCooldownDescriptor getCooldown() {
		return cooldown;
	}
}
