package gg.xp.xivsupport.events.triggers.jobs.joboverlays;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;

import javax.swing.*;

public abstract class BaseJobOverlay extends JPanel implements FilteredEventHandler {

	protected BaseJobOverlay() {
		setVisible(false);
		setLayout(null);
		setOpaque(false);
	}

	private boolean wasVisible;

	@Override
	public boolean enabled(EventContext context) {
		boolean nowVisible = isShowing();
		if (!wasVisible && nowVisible) {
			SwingUtilities.invokeLater(this::onBecomeVisible);
		}
		return wasVisible = nowVisible;
	}

	@Override
	public void setVisible(boolean vis) {
		if (vis) {
			onBecomeVisible();
		}
	}

	protected void onBecomeVisible() {

	}

	protected abstract void periodicRefresh();

}
