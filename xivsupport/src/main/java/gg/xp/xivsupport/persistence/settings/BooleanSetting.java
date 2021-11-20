package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

public class BooleanSetting {

	private final PersistenceProvider persistence;
	private final String settingKey;
	private final boolean dflt;
	private Boolean cached;

	public BooleanSetting(PersistenceProvider persistence, String settingKey, boolean dflt) {
		this.persistence = persistence;
		this.settingKey = settingKey;
		this.dflt = dflt;
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
	}
}
