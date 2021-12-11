package gg.xp.xivsupport.gui.tables.renderers;

import javax.swing.*;
import java.awt.*;

public class RenderUtils {
	public static void setTooltip(Component component, String tooltip) {
		if (component instanceof JComponent) {
			((JComponent) component).setToolTipText(tooltip);
		}
	}
}
