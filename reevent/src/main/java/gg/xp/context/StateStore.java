package gg.xp.context;

public interface StateStore {
	<X extends SubState> X get(Class<X> clazz);

	<X> void putCustom(Class<X> clazz, X instance);
}
