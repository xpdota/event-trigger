package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

public class EnumSetting<X extends Enum<X>> extends ObservableSetting {
	
	private final PersistenceProvider persistence;

	private final String settingKey;
	private final X dflt;
	private final Class<X> enumCls;
	private X cached;

	public EnumSetting(PersistenceProvider persistence, String settingKey, Class<X> enumCls, X dflt) {
		this.persistence = persistence;
		this.settingKey = settingKey;
		this.dflt = dflt;
		this.enumCls = enumCls;
	}

	public X get() {
		if (cached == null) {
			return cached = persistence.get(settingKey, enumCls, dflt);
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
