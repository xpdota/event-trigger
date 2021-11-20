package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

public class LongSetting {

	private final PersistenceProvider persistence;
	private final String settingKey;
	private final long dflt;
	private Long cached;

	public LongSetting(PersistenceProvider persistence, String settingKey, long dflt) {
		this.persistence = persistence;
		this.settingKey = settingKey;
		this.dflt = dflt;
	}

	public long get() {
		if (cached == null) {
			return cached = persistence.get(settingKey, long.class, dflt);
		}
		else {
			return cached;
		}
	}

	public void set(long newValue) {
		cached = newValue;
		persistence.save(settingKey, newValue);
	}
}
