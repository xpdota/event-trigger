package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

import java.net.URI;

public class WsURISetting extends ObservableSetting implements Resettable {
	private final PersistenceProvider persistence;
	private final String settingKey;
	private final URI dflt;
	private URI cached;

	public WsURISetting(PersistenceProvider persistence, String settingKey, URI dflt) {
		this.persistence = persistence;
		this.settingKey = settingKey;
		this.dflt = dflt;
	}
	public URI get() {
		if (cached == null) {
			return cached = persistence.get(settingKey, URI.class, dflt);
		}
		else {
			return cached;
		}
	}

	public URI getDefault() {
		return dflt;
	}

	public void set(URI newValue) {
		cached = newValue;
		persistence.save(settingKey, newValue);
		notifyListeners();
	}

	public void resetToDefault() {
		persistence.delete(settingKey);
		cached = null;
		notifyListeners();
	}

	@Override
	public boolean isSet() {
		return persistence.get(settingKey, URI.class, null) != null;
	}

	@Override
	public void delete() {
		resetToDefault();
	}
}
