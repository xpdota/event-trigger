package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

public class DoubleSetting extends ObservableSetting implements Resettable {

	private final PersistenceProvider persistence;
	private final String settingKey;
	private final double dflt;
	// TODO: make these actually do stuff
	private final double min;
	private final double max;
	private Double cached;

	public DoubleSetting(PersistenceProvider persistence, String settingKey, double dflt, double min, double max) {
		this.persistence = persistence;
		this.settingKey = settingKey;
		this.dflt = dflt;
		this.min = min;
		this.max = max;
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
		if (newValue > max) {
			throw new IllegalArgumentException(String.format("Value too large: %s (max %s)", newValue, max));
		}
		if (newValue < min) {
			throw new IllegalArgumentException(String.format("Value too small: %s (min %s)", newValue, min));
		}
		cached = newValue;
		persistence.save(settingKey, newValue);
		notifyListeners();
	}

	public void reset() {
		cached = dflt;
		persistence.delete(settingKey);
		notifyListeners();
	}

	public double getMax() {
		return max;
	}

	public double getMin() {
		return min;
	}

	@Override
	public boolean isSet() {
		return persistence.get(settingKey, Double.class, null) != null;
	}

	@Override
	public void delete() {
		reset();
	}
}
