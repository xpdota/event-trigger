package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.jetbrains.annotations.Nullable;

public class BasicCustomSettingDefaultNull<X> extends ObservableSetting {

	private final Class<X> clazz;
	private final PersistenceProvider persistence;
	private final String propertyKey;
	private boolean hasCachedValue;
	private @Nullable X cached;

	public BasicCustomSettingDefaultNull(Class<X> clazz, PersistenceProvider pers, String key) {
		this.clazz = clazz;
		this.persistence = pers;
		this.propertyKey = key;
	}

	public X get() {
		if (!hasCachedValue) {
			cached = persistence.get(propertyKey, clazz, null);
			hasCachedValue = true;
		}
		return cached;
	}

	public void delete() {
		cached = null;
		hasCachedValue = true;
		persistence.delete(propertyKey);
		notifyListeners();
	}

	public void set(X newValue) {
		if (newValue == null) {
			delete();
			return;
		}
		cached = newValue;
		persistence.save(propertyKey, newValue);
		notifyListeners();
	}

}
