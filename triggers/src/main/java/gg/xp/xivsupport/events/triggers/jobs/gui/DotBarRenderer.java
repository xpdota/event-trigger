package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.combatstate.TickInfo;
import gg.xp.xivsupport.gui.tables.renderers.ResourceBarRenderer;
import gg.xp.xivsupport.gui.tables.renderers.TickRenderInfo;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;

public class DotBarRenderer extends ResourceBarRenderer<VisualDotInfo> {

	private static final Color colorExpired = new Color(255, 0, 0, 192);
	private static final Color colorGood = new Color(53, 134, 159, 192);

	public DotBarRenderer() {
		super(VisualDotInfo.class);
	}

	@Override
	// TODO: rename this method?
	protected void formatLabel(@NotNull VisualDotInfo item) {
		bar.setTextOptions(((LabelOverride) item).getLabel());
		TickInfo tick = item.getTick();
		BuffApplied event = item.getEvent();
		if (tick == null || event.isPreApp()) {
			bar.setTicks(null);
		}
		else {
			Instant dotAppliedAt = event.getHappenedAt();
			long duration = event.getInitialDuration().toMillis();
			int interval = tick.getIntervalMs();
			double normalizedInterval = ((double) interval) / duration;
			double offset = ((double) tick.getMsToNextTick(dotAppliedAt)) / duration;
			bar.setTicks(new TickRenderInfo(offset, normalizedInterval));
		}
	}

	@Override
	protected Color getBarColor(double percent, @NotNull VisualDotInfo item) {
		if (percent > 0.999d) {
			return colorExpired;
		}
		return colorGood;
	}
}
