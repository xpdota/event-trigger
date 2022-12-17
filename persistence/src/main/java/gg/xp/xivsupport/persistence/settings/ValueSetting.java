package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

public class ValueSetting<X> extends ObservableSetting {

	private final PersistenceProvider persistence;
	private final String settingKey;
	private final X dflt;
	private X cached;

	private Class<X> clazz;

	public ValueSetting(PersistenceProvider persistence, String settingKey, X dflt) {
		this.persistence = persistence;
		this.settingKey = settingKey;
		this.dflt = dflt;
		this.clazz = (Class<X>) dflt.getClass();
	}

	public X get() {
		if (cached == null) {
			return cached = persistence.get(settingKey, clazz, dflt);
		}
		else {
			return cached;
		}
	}

	public void set(X newValue) {
		cached = newValue;
		persistence.save(settingKey, newValue);
		notifyListeners();
	}
}