package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

public class DoubleSetting {

	private final PersistenceProvider persistence;
	private final String settingKey;
	private final double dflt;
	private Double cached;

	public DoubleSetting(PersistenceProvider persistence, String settingKey, double dflt) {
		this.persistence = persistence;
		this.settingKey = settingKey;
		this.dflt = dflt;
	}

	public double get() {
		if (cached == null) {
			return cached = persistence.get(settingKey, double.class, dflt);
		}
		else {
			return cached;
		}
	}

	public void set(double newValue) {
		cached = newValue;
		persistence.save(settingKey, newValue);
	}
}
