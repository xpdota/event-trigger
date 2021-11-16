package gg.xp.xivsupport.gui.tables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class AutoBottomScrollHelper extends JScrollPane {

	private static final Logger log = LoggerFactory.getLogger(AutoBottomScrollHelper.class);
	private boolean autoScrollEnabled;
	private volatile int oldMax;
	private volatile int oldValue;

	// TODO: technically, this no longer needs a table
	public AutoBottomScrollHelper(JTable table, Runnable forceOffCallback) {
		super(table);
		// I think the best, but most-work solution would be to have the table update event turn on a scrollbar event listener,
		// and then have the event scroll down then remove itself.
		// This isn't perfect, but it's good enough for now
		// TODO: when changing the filter so that less stuff shows up, it disables auto scroll
		setPreferredSize(getMaximumSize());
		JScrollBar bar = getVerticalScrollBar();
		setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		bar.addAdjustmentListener(event -> SwingUtilities.invokeLater(() -> {
			int newMax = bar.getMaximum();
			int newValue = bar.getValue();
			if (newMax != oldMax) {
				doScrollIfEnabled();
			}
			else {
				if (newValue < oldValue) {
					forceOffCallback.run();
				}
			}
			log.trace("Value: {} -> {} ; Max: {} -> {}", oldValue, newValue, oldMax, newMax);
			oldMax = newMax;
			oldValue = newValue;
		}));

	}

	private void doScrollIfEnabled() {
		log.trace("doScrollIfEnabled");
		if (autoScrollEnabled) {
			doScroll();
		}
	}

	private void doScroll() {
		log.trace("doScroll");
		JScrollBar scrollBar = getVerticalScrollBar();
		int max = scrollBar.getMaximum();
		log.trace("Max: {}", max);
		scrollBar.setValue(Integer.MAX_VALUE);
	}

	public void setAutoScrollEnabled(boolean autoScrollEnabled) {
		this.autoScrollEnabled = autoScrollEnabled;
		doScrollIfEnabled();
	}
}
