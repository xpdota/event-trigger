package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.CurrentMaxPair;

import java.awt.*;

public class MpRenderer extends ResourceBarRenderer {
	private static final Color mpColor = new Color(128, 128, 255, 147);
	@Override
	protected Color getBarColor(double percent, CurrentMaxPair item) {
		return mpColor;
	}
}
