package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.gui.tables.renderers.ResourceBarRenderer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class DotBarRenderer extends ResourceBarRenderer<VisualDotInfo> {

	private static final Color colorExpired = new Color(255, 0, 0, 192);
	private static final Color colorGood = new Color(53, 134, 159, 192);

	public DotBarRenderer() {
		super(VisualDotInfo.class);
	}

	@Override
	protected void formatLabel(@NotNull VisualDotInfo item) {
		bar.setTextOptions(((LabelOverride) item).getLabel());
	}

	@Override
	protected Color getBarColor(double percent, @NotNull VisualDotInfo item) {
		if (percent > 0.999d) {
			return colorExpired;
		}
		return colorGood;
	}
}
