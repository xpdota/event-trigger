package gg.xp.xivsupport.models;

import java.io.Serializable;
import java.util.Objects;

public final class ManaPoints implements CurrentMaxPair, Serializable {
	private static final long serialVersionUID = 5725036718136891291L;
	private final long current;
	private final long max;

	private ManaPoints(long current, long max) {
		this.current = current;
		this.max = max;
	}

	private static final ManaPoints FULL = new ManaPoints(10_000, 10_000);

	public static ManaPoints of(long current, long max) {
		if (current == 10_000 && max == 10_000) {
			return FULL;
		}
		return new ManaPoints(current, max);
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
		return String.format("MP(%s / %s)", current, max);
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

	// Re-usable instance to save memory

}

