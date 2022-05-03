package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.xivsupport.events.state.combatstate.CastResult;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.gui.tables.renderers.ResourceBar;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class CastBarComponent extends ResourceBar {

	private @Nullable CastTracker tracker;

	public CastBarComponent() {
		setOpaque(false);
		setColor3(new Color(20, 20, 20));
	}

	public void setData(@Nullable CastTracker tracker) {
		this.tracker = tracker;
		if (tracker != null) {
			long duration = tracker.getCast().getInitialDuration().toMillis();
			long current = tracker.getCast().getEstimatedElapsedDuration().toMillis();
			double pct = ((double) current) / duration;
			setPercent1(pct);
			CastResult result = tracker.getResult();
			// TODO: colors
			setColor1(switch (result) {
				case IN_PROGRESS -> new Color(47, 82, 26);
				case SUCCESS -> new Color(9, 46, 115);
				case INTERRUPTED -> new Color(94, 10, 10);
				case UNKNOWN -> new Color(100, 71, 9);
			});
			setTextOptions(tracker.getCast().getAbility().getName());
			setTextColor(Color.WHITE);
			revalidate();
		}
	}

	@Override
	public void paint(Graphics g) {
		// Just do nothing if there's no active cast
		if (tracker == null) {
//			g.clearRect(0, 0, getWidth(), getHeight());
			return;
		}
		super.paint(g);
	}
}
