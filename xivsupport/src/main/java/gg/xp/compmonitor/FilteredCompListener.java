package gg.xp.compmonitor;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class FilteredCompListener<T> implements CompListener {
	private final Class<T> type;
	private final Predicate<InstantiatedItem<T>> filter;
	private final Consumer<InstantiatedItem<T>> action;

	public FilteredCompListener(Class<T> type, Consumer<InstantiatedItem<T>> action) {
		this.type = type;
		this.filter = unused -> true;
		this.action = action;
	}

	public FilteredCompListener(Class<T> type, Predicate<InstantiatedItem<T>> filter, Consumer<InstantiatedItem<T>> action) {
		this.type = type;
		this.filter = filter;
		this.action = action;
	}


	@Override
	public void added(InstantiatedItem<?> item) {
		if (type.isInstance(item.cls()) && filter.test((InstantiatedItem<T>) item)) {
			action.accept((InstantiatedItem<T>) item);
		}
	}

}
