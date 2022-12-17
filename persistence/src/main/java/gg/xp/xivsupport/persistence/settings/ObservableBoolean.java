package gg.xp.xivsupport.persistence.settings;

import java.util.function.Consumer;

public interface ObservableBoolean {

	boolean get();

	void addListener(Runnable listener);

	void addAndRunListener(Runnable listener);

	void removeListener(Runnable listener);

	default Runnable addValueListener(Consumer<Boolean> listener) {
		Runnable realListener = () -> listener.accept(get());
		addListener(realListener);
		return realListener;
	};

	default Runnable addAndRunValueListener(Consumer<Boolean> listener) {
		Runnable realListener = () -> listener.accept(get());
		addAndRunListener(realListener);
		return realListener;
	};

}
