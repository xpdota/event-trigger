package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

public class IntSetting {

	private final PersistenceProvider persistence;
	private final String settingKey;
	private final int dflt;
	private Integer cached;

	public IntSetting(PersistenceProvider persistence, String settingKey, int dflt) {
		this.persistence = persistence;
		this.settingKey = settingKey;
		this.dflt = dflt;
	}

	public int get() {
		if (cached == null) {
			return cached = persistence.get(settingKey, int.class, dflt);
		}
		else {
			return cached;
		}
	}

	public void set(int newValue) {
		cached = newValue;
		persistence.save(settingKey, newValue);
	}
}
