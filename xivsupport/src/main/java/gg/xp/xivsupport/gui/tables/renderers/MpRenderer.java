package gg.xp.xivsupport.gui.tables.renderers;

import java.awt.*;

public class MpRenderer extends ResourceBarRenderer {
	private static final Color mpColor = new Color(128, 128, 255);
	@Override
	protected Color getBarColor(double percent) {
		return mpColor;
	}
}
