package gg.xp.xivsupport.models;

import java.io.Serializable;
import java.util.Objects;

public final class HitPoints implements Serializable {
	private static final long serialVersionUID = 5725036718136891291L;
	private final long current;
	private final long max;

	public HitPoints(long current, long max) {
		this.current = current;
		this.max = max;
	}

	public long getCurrent() {
		return current;
	}

	public long getMax() {
		return max;
	}

	@Override
	public String toString() {
		return String.format("HP( %s / %s )", current, max);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		HitPoints hitPoints = (HitPoints) o;
		return current == hitPoints.current && max == hitPoints.max;
	}

	@Override
	public int hashCode() {
		return Objects.hash(current, max);
	}
}

