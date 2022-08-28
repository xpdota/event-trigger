package gg.xp.xivsupport.gui.util;

import java.awt.*;

public class ColorUtils {

	public static Color intToColor(int color) {
		return new Color(color, true);
	}

	public static int colorToInt(Color color) {
		return color.getRGB();
	}
}
