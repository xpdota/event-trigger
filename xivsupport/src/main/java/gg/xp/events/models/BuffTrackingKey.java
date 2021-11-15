package gg.xp.events.models;

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
		this.buff = buff;
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
}
