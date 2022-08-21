package gg.xp.xivsupport.models;

public interface CurrentMaxPair {
	long current();

	long max();

	default double getPercent() {
		return current() / (double) max();
	}

	default String getShortString() {
		return String.format("%s / %s", current(), max());
	}

	default boolean isEmpty() {
		return current() == 0;
	}

	default boolean isFull() {
		return current() == max();
	}
}
