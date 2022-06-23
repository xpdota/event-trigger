package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.jetbrains.annotations.Nullable;

public class IntSetting extends ObservableSetting {

	private final PersistenceProvider persistence;
	private final String settingKey;
	private final int dflt;
	private Integer cached;
	private final Integer min;
	private final Integer max;

	public IntSetting(PersistenceProvider persistence, String settingKey, int dflt) {
		this(persistence, settingKey, dflt, null, null);
	}

	public IntSetting(PersistenceProvider persistence, String settingKey, int dflt, Integer min, Integer max) {
		this.persistence = persistence;
		this.settingKey = settingKey;
		this.dflt = dflt;
		this.min = min;
		this.max = max;
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
		if (min != null && newValue < min) {
			throw new IllegalArgumentException(String.format("Setting %s has a minimum of %s, and you tried to set it to %s", settingKey, min, newValue));
		}
		if (max != null && newValue > max) {
			throw new IllegalArgumentException(String.format("Setting %s has a maximum of %s, and you tried to set it to %s", settingKey, max, newValue));
		}
		cached = newValue;
		persistence.save(settingKey, newValue);
		notifyListeners();
	}

	public @Nullable Integer getMin() {
		return min;
	}

	public @Nullable Integer getMax() {
		return max;
	}
}
