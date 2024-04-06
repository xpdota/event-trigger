package gg.xp.xivsupport.models;

import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasStatusEffect;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;

import java.util.Objects;

/**
 * Hashes the things that make a buff unique, as in, if the new buff matches the old buff,
 * and the old buff is not expired, then this is a buff refresh.
 */
public class BuffTrackingKey {
	private final XivEntity source;
	private final XivEntity target;
	private final XivStatusEffect buff;

	public BuffTrackingKey(XivEntity source, XivEntity target, XivStatusEffect buff) {
		this.source = source;
		this.target = target;
		// TODO: this is really not the place for this logic
		// Goring Blade and Blade of Valor are mutually exclusive, so they should be tracked as if they were the same
		// for dot timing purposes
		if (buff.getId() == 0xaa1) {
			this.buff = new XivStatusEffect(0x2D5);
		}
		else {
			this.buff = buff;
		}
	}

	public static <X extends HasSourceEntity & HasTargetEntity & HasStatusEffect> BuffTrackingKey of(X event) {
		return new BuffTrackingKey(event.getSource(), event.getTarget(), event.getBuff());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BuffTrackingKey that = (BuffTrackingKey) o;
		return Objects.equals(source.getId(), that.source.getId()) && Objects.equals(target.getId(), that.target.getId()) && Objects.equals(buff.getId(), that.buff.getId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(source.getId(), target.getId(), buff.getId());
	}

	public XivEntity getSource() {
		return source;
	}

	public XivEntity getTarget() {
		return target;
	}

	public XivStatusEffect getBuff() {
		return buff;
	}

	@Override
	public String toString() {
		return String.format("BuffTrackingKey(%s on %s from %s)", buff, target, source);
	}
}
