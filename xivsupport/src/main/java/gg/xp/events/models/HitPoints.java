package gg.xp.events.models;

import java.io.Serializable;

public class HitPoints implements Serializable {
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
}

