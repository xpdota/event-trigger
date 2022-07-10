package gg.xp.xivsupport.models;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public record ManaPoints(long current, long max) implements CurrentMaxPair, Serializable {
	@Serial
	private static final long serialVersionUID = 5725036718136891291L;

	// Re-usable instance to save memory
	private static final ManaPoints FULL = new ManaPoints(10_000, 10_000);
	private static final ManaPoints EMPTY = new ManaPoints(0, 10_000);

	public static ManaPoints of(long current, long max) {
		if (current == 10_000 && max == 10_000) {
			return FULL;
		}
		else if (current == 0 && max == 10_000) {
			return EMPTY;
		}
		return new ManaPoints(current, max);
	}

	@Override
	public long current() {
		return current;
	}

	@Override
	public long max() {
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


}

