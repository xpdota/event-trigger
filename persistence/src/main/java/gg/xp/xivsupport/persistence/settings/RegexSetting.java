package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class RegexSetting extends ObservableSetting implements Resettable {
	private final PersistenceProvider persistence;
	private final String settingKey;
	private final Pattern dflt;
	private @Nullable Pattern cached;
	private boolean hasCachedValue;
	
	public RegexSetting(PersistenceProvider persistence, String settingKey, Pattern dflt) {
		this.persistence = persistence;
		this.settingKey = settingKey;
		this.dflt = dflt;
	}
	
	public Pattern get() {
		if (!hasCachedValue) {
			hasCachedValue = true;
			return cached = persistence.get(settingKey, Pattern.class, dflt);
		}
		else {
			return cached;
		}
	}

	public Pattern getDefault() {
		return dflt;
	}

	public void set(Pattern newValue) {
		hasCachedValue = true;
		cached = newValue;
		persistence.save(settingKey, newValue);
		notifyListeners();
	}

	public void resetToDefault() {
		cached = dflt;
		hasCachedValue = true;
		persistence.delete(settingKey);
		notifyListeners();
	}

	@Override
	public boolean isSet() {
		return persistence.get(settingKey, Pattern.class, null) != null;
	}

	@Override
	public void delete() {
		resetToDefault();
	}
}
