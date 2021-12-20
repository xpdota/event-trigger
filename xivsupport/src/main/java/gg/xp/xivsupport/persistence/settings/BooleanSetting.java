package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

public class BooleanSetting extends ObservableSetting {

	private final PersistenceProvider persistence;
	private final String settingKey;
	private final boolean dflt;
	private Boolean cached;

	public BooleanSetting(PersistenceProvider persistence, String settingKey, boolean dflt) {
		this.persistence = persistence;
		this.settingKey = settingKey;
		this.dflt = dflt;
	}

	public boolean isSet() {
		return persistence.get(settingKey, Boolean.class, null) != null;
	}

	public boolean get() {
		if (cached == null) {
			return cached = persistence.get(settingKey, boolean.class, dflt);
		}
		else {
			return cached;
		}
	}

	public void set(boolean newValue) {
		cached = newValue;
		persistence.save(settingKey, newValue);
		notifyListeners();
	}
}
