package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;

import java.awt.*;

public class ColorSetting extends ObservableSetting implements Resettable {


	private final PersistenceProvider persistence;

	private final String settingKey;
	private final Color dflt;
	private Color cached;

	public ColorSetting(PersistenceProvider persistence, String settingKey, Color dflt) {
		this.persistence = persistence;
		this.settingKey = settingKey;
		this.dflt = dflt;
	}

	public Color get() {
		if (cached == null) {
			return cached = intToColor(persistence.get(settingKey, Integer.class, colorToInt(dflt)));
		}
		else {
			return cached;
		}
	}

	public void set(Color newValue) {
		cached = newValue;
		persistence.save(settingKey, colorToInt(newValue));
		notifyListeners();
	}

	private static Color intToColor(int color) {
		return new Color(color, true);
	}

	private static int colorToInt(Color color) {
		return color.getRGB();
	}

	@Override
	public boolean isSet() {
		return persistence.get(settingKey, Integer.class, null) != null;
	}

	@Override
	public void delete() {
		persistence.delete(settingKey);
		cached = null;
		notifyListeners();
	}

	public Color getDefault() {
		return dflt;
	}
}
