package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.gui.tables.renderers.ResourceBarRenderer;
import gg.xp.xivsupport.models.CurrentMaxPair;

import javax.swing.*;
import java.awt.*;

public class DotBarRenderer extends ResourceBarRenderer {

	private static final Color colorExpired = new Color(255, 0, 0, 192);
	private static final Color colorGood = new Color(53, 134, 159, 192);

	@Override
	protected void formatLabel(CurrentMaxPair item) {
		if (item instanceof LabelOverride) {
			bar.setTextOptions(((LabelOverride) item).getLabel());
		}
		else {
			super.formatLabel(item);
		}
	}

	@Override
	protected Color getBarColor(double percent, CurrentMaxPair item) {
		if (percent > 0.999d) {
			return colorExpired;
		}
		return colorGood;
	}
}
