package gg.xp.xivsupport.gui.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public final class ColorUtils {

	private ColorUtils() {
	}

	public static Color intToColor(int color) {
		return new Color(color, true);
	}

	public static int colorToInt(Color color) {
		return color.getRGB();
	}

	@Contract("null -> null; !null -> new")
	public static @Nullable Color modifiedSettingColor(Color base) {
		if (base == null) {
			return null;
		}
		// If color is very dark to begin with, lighten it more. Reducing the red component is insufficient.
		if (base.getRed() + base.getGreen() + base.getBlue() <= 30) {
			return new Color(Math.max(base.getRed(), 0), Math.min(base.getGreen() + 150, 255), Math.min(base.getBlue() + 96, 255));
		}
		return new Color(Math.max(base.getRed() - 48, 0), Math.min(base.getGreen() + 64, 255), Math.min(base.getBlue() + 48, 255));
	}
}
