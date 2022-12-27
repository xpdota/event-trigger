package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

public class StringSetting extends ObservableSetting implements Resettable {

	private final PersistenceProvider persistence;
	private final String settingKey;
	private final String dflt;
	private String cached;

	public StringSetting(PersistenceProvider persistence, String settingKey, String dflt) {
		this.persistence = persistence;
		this.settingKey = settingKey;
		this.dflt = dflt;
	}

	@Override
	public boolean isSet() {
		return persistence.get(settingKey, String.class, null) != null;
	}

	public String get() {
		if (cached == null) {
			return cached = persistence.get(settingKey, String.class, dflt);
		}
		else {
			return cached;
		}
	}

	@Override
	public void delete() {
		persistence.delete(settingKey);
		cached = null;
		notifyListeners();
	}

	public void set(String newValue) {
		cached = newValue;
		persistence.save(settingKey, newValue);
		notifyListeners();
	}

	public String getDefault() {
		return dflt;
	}
}
