package gg.xp.xivsupport.persistence.settings;

import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class ColorSetting extends ObservableSetting implements Resettable {


	private final PersistenceProvider persistence;

	private final String settingKey;
	private final @Nullable Color dflt;
	private Color cached;
	private boolean hasCachedValue;

	public ColorSetting(PersistenceProvider persistence, String settingKey, @Nullable Color dflt) {
		this.persistence = persistence;
		this.settingKey = settingKey;
		this.dflt = dflt;
	}

	public @Nullable Color get() {
		if (hasCachedValue) {
			return cached;
		}
		else {
			Integer colorAsInt = persistence.get(settingKey, Integer.class, null);
			Color value;
			if (colorAsInt == null) {
				value = dflt;
			}
			else {
				value = intToColor(colorAsInt);
			}
			cached = value;
			hasCachedValue = true;
			return value;
		}
	}

	public void set(Color newValue) {
		cached = newValue;
		hasCachedValue = true;
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
		hasCachedValue = false;
		notifyListeners();
	}

	public Color getDefault() {
		return dflt;
	}
}
