package gg.xp.xivsupport.gui.tables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.function.Consumer;

public class AutoBottomScrollHelper extends JScrollPane {

	private static final Logger log = LoggerFactory.getLogger(AutoBottomScrollHelper.class);
	private static final boolean oldBehavior = "true".equals(System.getProperty("bottom-scroll.old-behavior"));
	private final Consumer<Boolean> stateCallback;
	private boolean autoScrollEnabled;
	private boolean atBottom;
	private volatile int oldMax;
	private volatile int oldValue;
	private volatile int oldExtent;

	// TODO: technically, this no longer needs a table
	public AutoBottomScrollHelper(JTable table, Consumer<Boolean> stateCallback) {
		super(table);
		// I think the best, but most-work solution would be to have the table update event turn on a scrollbar event listener,
		// and then have the event scroll down then remove itself.
		// This isn't perfect, but it's good enough for now
		setPreferredSize(getMaximumSize());
		setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		atBottom = true;
		this.stateCallback = val -> {
			boolean old = autoScrollEnabled;
			stateCallback.accept(val);
			autoScrollEnabled = old;
		};

	}

	@Override
	public JScrollBar createVerticalScrollBar() {
		return new JScrollPane.ScrollBar(JScrollBar.VERTICAL) {
			@Override
			public void setValues(int newValue, int newExtent, int newMin, int newMax) {
//				log.info("Value: {} -> {} ; Max: {} -> {}", oldValue, newValue, oldMax, newMax);
				// This branch means more data has been added to the table
				if (newMax != oldMax || newExtent != oldExtent) {
					if (atBottom && autoScrollEnabled) {
						newValue = newMax - newExtent;
					}
				}
				// This branch means that the user has scrolled
				else {
					// User scrolled to bottom
					if (newValue + newExtent >= newMax) {
						if (!oldBehavior && autoScrollEnabled) {
							stateCallback.accept(true);
							atBottom = true;
						}
					}
					// User scrolled up
					else if (newValue < oldValue) {
						atBottom = false;
						stateCallback.accept(false);
					}
				}
				oldMax = newMax;
				oldValue = newValue;
				oldExtent = newExtent;
				super.setValues(newValue, newExtent, newMin, newMax);
			}
		};
	}

	public boolean isAutoScrollEnabled() {
		return autoScrollEnabled;
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
		atBottom = true;
	}

	public void setAutoScrollEnabled(boolean autoScrollEnabled) {
		this.autoScrollEnabled = autoScrollEnabled;
		doScrollIfEnabled();
	}
}
