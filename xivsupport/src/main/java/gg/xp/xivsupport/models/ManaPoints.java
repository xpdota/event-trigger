package gg.xp.xivsupport.models;

import java.io.Serializable;
import java.util.Objects;

public final class ManaPoints implements ResourcePoints, Serializable {
	private static final long serialVersionUID = 5725036718136891291L;
	private final long current;
	private final long max;

	public ManaPoints(long current, long max) {
		this.current = current;
		this.max = max;
	}

	@Override
	public long getCurrent() {
		return current;
	}

	@Override
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
		ManaPoints manaPoints = (ManaPoints) o;
		return current == manaPoints.current && max == manaPoints.max;
	}

	@Override
	public int hashCode() {
		return Objects.hash(current, max);
	}
}

