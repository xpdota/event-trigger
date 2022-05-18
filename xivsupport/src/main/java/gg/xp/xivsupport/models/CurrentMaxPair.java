package gg.xp.xivsupport.models;

public interface CurrentMaxPair {
	long getCurrent();

	long getMax();

	default String getShortString() {
		return String.format("%s / %s", getCurrent(), getMax());
	}
}
