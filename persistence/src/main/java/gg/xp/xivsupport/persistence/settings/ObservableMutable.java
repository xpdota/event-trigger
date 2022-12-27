package gg.xp.xivsupport.persistence.settings;

public interface ObservableMutable<X> extends ObservableValue<X> {
	void set(X value);
}
