package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.gui.tables.renderers.ResourceBarRenderer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class CdBarRenderer extends ResourceBarRenderer<VisualCdInfo> {

	private static final Color colorActive = new Color(19, 8, 201, 192);
	private static final Color colorReady = new Color(55, 182, 67, 192);
	private static final Color colorOnCd = new Color(192, 0, 0, 192);

	public CdBarRenderer() {
		super(VisualCdInfo.class);
	}

	@Override
	protected void formatLabel(@NotNull VisualCdInfo item) {
		bar.setTextOptions(((LabelOverride) item).getLabel());
	}

	@Override
	protected Color getBarColor(double percent, @NotNull VisualCdInfo item) {
		if (item.getBuffApplied() != null) {
			return colorActive;
		}
		if (percent > 0.999d) {
			return colorReady;
		}
		else {
			return colorOnCd;
		}

	}
}
