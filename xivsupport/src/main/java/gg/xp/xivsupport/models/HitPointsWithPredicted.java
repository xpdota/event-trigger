package gg.xp.xivsupport.models;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public final class HitPointsWithPredicted implements CurrentMaxPredicted, Serializable {
	@Serial
	private static final long serialVersionUID = 5725036718136891291L;
	private final long current;
	private final long predicted;
	private final long max;

	public HitPointsWithPredicted(long current, long predicted, long max) {
		this.current = current;
		this.predicted = predicted;
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
	public long getPredicted() {
		return predicted;
	}

	@Override
	public String toString() {
		return String.format("HP(%s (-> %s) / %s)", current, predicted, max);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		HitPointsWithPredicted that = (HitPointsWithPredicted) o;
		return current == that.current && predicted == that.predicted && max == that.max;
	}

	@Override
	public int hashCode() {
		return Objects.hash(current, predicted, max);
	}
}

