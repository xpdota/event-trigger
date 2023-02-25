package gg.xp.xivsupport.persistence.settings;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

public interface WeakRunnable extends Runnable, WeakItem {

	static <X> WeakRunnable of(X item, Consumer<? super X> action) {
		WeakReference<X> ref = new WeakReference<>(item);
		return new WeakRunnable() {
			@Override
			public boolean isGone() {
				return ref.get() == null;
			}

			@Override
			public void run() {
				X item = ref.get();
				if (item != null) {
					action.accept(item);
				}
			}
		};
	}

}
