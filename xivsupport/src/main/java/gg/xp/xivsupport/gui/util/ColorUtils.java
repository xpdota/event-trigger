package gg.xp.xivsupport.gui.util;

import org.jetbrains.annotations.Contract;

import java.awt.*;

public class ColorUtils {

	public static Color intToColor(int color) {
		return new Color(color, true);
	}

	public static int colorToInt(Color color) {
		return color.getRGB();
	}

	@Contract("null -> null; !null -> new")
	public static Color modifiedSettingColor(Color base) {
		if (base == null) {
			return null;
		}
		return new Color(Math.max(base.getRed() - 96, 0), Math.min(base.getGreen() + 128, 255), Math.min(base.getBlue() + 96, 255));
	}
}
