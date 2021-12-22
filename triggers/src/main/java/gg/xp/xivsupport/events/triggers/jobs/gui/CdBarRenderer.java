package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.gui.tables.renderers.ResourceBarRenderer;
import gg.xp.xivsupport.models.CurrentMaxPair;

import java.awt.*;

public class CdBarRenderer extends ResourceBarRenderer {

	private static final Color colorActive = new Color(19, 8, 201, 192);
	private static final Color colorReady = new Color(55, 182, 67, 192);
	private static final Color colorOnCd = new Color(192, 0, 0, 192);


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
		if (item instanceof VisualCdInfo) {
			if (((VisualCdInfo) item).getBuffApplied() != null) {
				return colorActive;
			}
			if (percent > 0.999d) {
				return colorReady;
			}
			else {
				return colorOnCd;
			}

		}
		if (percent > 0.999d) {
			return colorOnCd;
		}
		return colorActive;
	}
}
