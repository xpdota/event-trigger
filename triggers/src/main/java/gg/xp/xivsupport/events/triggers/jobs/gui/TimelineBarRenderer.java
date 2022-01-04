package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.events.triggers.duties.timelines.VisualTimelineEntry;
import gg.xp.xivsupport.gui.tables.renderers.ResourceBarRenderer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class TimelineBarRenderer extends ResourceBarRenderer<VisualTimelineEntry> {

	private static final Color colorActive = new Color(255, 0, 0, 192);
	private static final Color colorExpired = new Color(128, 0, 128, 192);
	private static final Color colorUpcoming = new Color(53, 134, 159, 192);

	public TimelineBarRenderer() {
		super(VisualTimelineEntry.class);
	}

	@Override
	protected void formatLabel(@NotNull VisualTimelineEntry item) {
		bar.setTextOptions(((LabelOverride) item).getLabel());
	}

	@Override
	protected Color getBarColor(double percent, @NotNull VisualTimelineEntry item) {
		if (item.remainingActiveTime() > 0) {
			return colorActive;
		}
		if (percent < 0.999d) {
			return colorUpcoming;
		}
		return colorExpired;
	}
}
