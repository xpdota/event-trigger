package gg.xp.reevent.context;

public final class NoOpStateStore implements StateStore {
	public static final NoOpStateStore INSTANCE = new NoOpStateStore();

	private NoOpStateStore() {
	}

	@Override
	public <X> X get(Class<X> clazz) {
		throw new UnsupportedOperationException("This state store does not do anything");
	}

	@Override
	public <X> void putCustom(Class<X> clazz, X instance) {
		throw new UnsupportedOperationException("This state store does not do anything");
	}
}
