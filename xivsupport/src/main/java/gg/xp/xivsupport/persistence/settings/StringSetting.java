package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

public class StringSetting {

	private final PersistenceProvider persistence;
	private final String settingKey;
	private final String dflt;
	private String cached;

	public StringSetting(PersistenceProvider persistence, String settingKey, String dflt) {
		this.persistence = persistence;
		this.settingKey = settingKey;
		this.dflt = dflt;
	}

	public String get() {
		if (cached == null) {
			return cached = persistence.get(settingKey, String.class, dflt);
		}
		else {
			return cached;
		}
	}

	public void set(String newValue) {
		cached = newValue;
		persistence.save(settingKey, newValue);
	}
}
