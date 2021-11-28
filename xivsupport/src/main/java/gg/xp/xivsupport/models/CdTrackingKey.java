package gg.xp.xivsupport.models;

import gg.xp.xivdata.jobs.Cooldown;
import gg.xp.xivsupport.events.actlines.events.HasAbility;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;

import java.util.Objects;

/**
 * Hashes the things that make a buff unique, as in, if the new buff matches the old buff,
 * and the old buff is not expired, then this is a buff refresh.
 */
public final class CdTrackingKey {
	private final XivEntity source;
	private final XivAbility ability;
	private final Cooldown cooldown;

	private CdTrackingKey(XivEntity source, XivAbility ability, Cooldown cooldown) {
		this.source = source;
		this.ability = ability;
		this.cooldown = cooldown;
	}

	public static <X extends HasSourceEntity & HasAbility> CdTrackingKey of(X event, Cooldown cooldown) {
		return new CdTrackingKey(event.getSource(), event.getAbility(), cooldown);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CdTrackingKey that = (CdTrackingKey) o;
		return Objects.equals(source, that.source) && Objects.equals(ability, that.ability);
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, ability);
	}

	public XivEntity getSource() {
		return source;
	}

	public XivAbility getAbility() {
		return ability;
	}

	public Cooldown getCooldown() {
		return cooldown;
	}
}
