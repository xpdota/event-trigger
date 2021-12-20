package gg.xp.xivsupport.gui.tables.renderers;

import gg.xp.xivsupport.models.CurrentMaxPair;

import java.awt.*;

public class HpRenderer extends ResourceBarRenderer {
	@Override
	protected Color getBarColor(double percent, CurrentMaxPair item) {
		return Color.getHSBColor((float) (0.33f * percent), 0.36f, 0.52f);
	}
}
