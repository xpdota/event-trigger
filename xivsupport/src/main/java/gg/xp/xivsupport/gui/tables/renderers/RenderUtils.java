package gg.xp.xivsupport.gui.tables.renderers;

import javax.swing.*;
import java.awt.*;

public final class RenderUtils {
	private RenderUtils() {
	}

	public static void setTooltip(Component component, String tooltip) {
		if (component instanceof JComponent jc) {
			jc.setToolTipText(tooltip);
		}
	}

	public static Color withAlpha(Color base, int alpha) {
		return new Color(base.getRGB() & 0xffffff + (alpha << 24), true);
	}
}
