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

	@Override
	public boolean enabled(EventContext context) {
		return isVisible();
	}

	public void setVisible(boolean vis) {
		if (vis) {
			onBecomeVisible();
		}
	}

	protected void onBecomeVisible() {

	}

	protected abstract void periodicRefresh();

}
