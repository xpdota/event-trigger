package gg.xp.xivsupport.gui.tables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

// TODO: the new logic is pretty annoying because you can alter your filter to be more restrictive, while live
// watching a log, and then it deactivates.
public class AutoBottomScrollHelper extends JScrollPane {

	private static final Logger log = LoggerFactory.getLogger(AutoBottomScrollHelper.class);
	private static final boolean oldBehavior = "true".equals(System.getProperty("bottom-scroll.old-behavior"));
	/**
	 * Callback for when auto bottom scroll is currently active or not. Wraps the user-supplied stateCallback to
	 * reset the value of autoScrollEnabled back to what it was before the callback. Thus, the callback is not allowed
	 * to modify the state of autoScrollEnabled.
	 */
	private final Consumer<Boolean> stateCallback;
	/**
	 * Whether auto scroll is enabled or not. Note that in order for auto-scrolling to actually be **active**,
	 * {@link #atBottom} must also be true. For example, if you auto-scroll is active, but you scroll up, then auto
	 * scrolling will be interrupted (atBottom = false), but this will remain true.
	 */
	private boolean autoScrollEnabled;
	/**
	 * Whether we are currently (or *should* be) at the bottom of the scrollable area.
	 */
	private boolean atBottom;

	// TODO: technically, this no longer needs a table
	public AutoBottomScrollHelper(JTable table, Consumer<Boolean> stateCallback) {
		super(table);
		// I think the best, but most-work solution would be to have the table update event turn on a scrollbar event listener,
		// and then have the event scroll down then remove itself.
		// This isn't perfect, but it's good enough for now
		setPreferredSize(getMaximumSize());
		setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
		atBottom = true;
		this.stateCallback = val -> {
			boolean old = autoScrollEnabled;
			stateCallback.accept(val);
			autoScrollEnabled = old;
		};
		// Workaround to allow you to re-activate bottom scrolling by mouse wheeling down when the scrollable area
		// is already all the way at the bottom.
		addMouseWheelListener(e -> {
			if (e.getWheelRotation() > 0 && autoScrollEnabled && !atBottom) {
				JScrollBar sb = getVerticalScrollBar();
				BoundedRangeModel brm = sb.getModel();
				// Is the scrollbar already at the bottom?
				if (brm.getValue() + brm.getExtent() == brm.getMaximum()) {
					atBottom = true;
					stateCallback.accept(true);
				}
			}
		});

	}

	/**
	 * A lock to prevent JTable's default configuration from overriding our custom header setup.
	 */
	private boolean headerLock;

	@Override
	public void setColumnHeaderView(Component view) {
		if (headerLock) {
			log.trace("setColumnHeaderView blocked by lock");
			return;
		}
		super.setColumnHeaderView(view);
	}

	@Override
	public void setColumnHeader(JViewport columnHeader) {
		if (headerLock) {
			log.trace("setColumnHeader blocked by lock");
			return;
		}
		super.setColumnHeader(columnHeader);
	}

	/**
	 * Sets the column header view and locks it, preventing JTable from automatically resetting
	 * it to a standard JTableHeader via configureEnclosingScrollPane.
	 *
	 * @param view the component to set as the column header view
	 */
	public void setColumnHeaderViewLocked(Component view) {
		headerLock = false;
		setColumnHeaderView(view);
		headerLock = true;
	}

	private record ScrollPosition(int value, int extent, int min, int max) {
	}

	private ScrollPosition previous = new ScrollPosition(0, 0, 0, 0);
	private ScrollPosition pending;

	@Override
	public JScrollBar createVerticalScrollBar() {
		return new JScrollPane.ScrollBar(JScrollBar.VERTICAL) {

			private void processAsync() {
				var newScroll = pending;
				if (newScroll == null) {
					return;
				}
				int newValue = newScroll.value;
				int newExtent = newScroll.extent;
				int newMin = newScroll.min;
				int newMax = newScroll.max;
				ScrollPosition oldScroll = previous;
				int oldValue = oldScroll.value;
				int oldMax = oldScroll.max;
				int oldExtent = oldScroll.extent;
//				log.info("Old: {}, New, {}", oldScroll, newScroll);
//				log.info("Value: {} -> {} ; Max: {} -> {}", oldValue, newValue, oldMax, newMax);
				// This branch means more data has been added to the table.
				if (newMax != oldMax || newExtent != oldExtent) {
					if (atBottom && autoScrollEnabled) {
						// Do the auto scroll
						newValue = newMax - newExtent;
//						log.info("newValue modified to {}", newValue);
					}
				}
				// Not an actual scroll - just quirks of how the scrolling works
				else if (newValue == oldValue && newMax == oldMax) {
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
					// This branch seems to get hit if the filtering becomes more restrictive
					// It seems the value updates before the max?
					// Gets something like this:
					/*
					08:34:03.658 [AWT-EventQueue-0] INFO  g.x.x.g.t.AutoBottomScrollHelper:108 - Value: 20939 -> 2579 ; Max: 21600 -> 21600
					08:34:03.658 [AWT-EventQueue-0] INFO  g.x.x.g.t.AutoBottomScrollHelper:108 - Value: 2579 -> 2579 ; Max: 21600 -> 3240
					08:34:03.683 [AWT-EventQueue-0] INFO  g.x.x.g.t.AutoBottomScrollHelper:108 - Value: 2579 -> 2579 ; Max: 3240 -> 3240
					 */
					else if (newValue < oldValue) {
						atBottom = false;
						stateCallback.accept(false);
					}
				}
				previous = new ScrollPosition(newValue, newExtent, newMin, newMax);
				super.setValues(newValue, newExtent, newMin, newMax);

			}

			@Override
			public void setValues(int newValue, int newExtent, int newMin, int newMax) {
				// For some reason, if the table becomes shorter, we get the "scroll up" part before the "table shortened" part,
				// so collapse multiple scroll requests.
				pending = new ScrollPosition(newValue, newExtent, newMin, newMax);
				SwingUtilities.invokeLater(this::processAsync);
			}
		};
	}

	/**
	 * @return Whether auto-scroll is desired. This will return true even if auto-scrolling is paused due to the user
	 * scrolling up.
	 */
	public boolean isAutoScrollEnabled() {
		return autoScrollEnabled;
	}

	/**
	 * @return Whether we are currently at the bottom of the scroll viewport.
	 */
	public boolean isAtBottom() {
		return atBottom;
	}

	/**
	 * @return Whether or not auto-scrolling is currently active, i.e. {@link #isAutoScrollEnabled()} &amp;&amp;
	 * {@link #isAtBottom()}.
	 */
	public boolean isAutoScrollActive() {
		return isAutoScrollEnabled() && isAtBottom();
	}

	private void doScrollIfEnabled() {
//		log.trace("doScrollIfEnabled");
		if (autoScrollEnabled) {
			doScroll();
		}
	}

	private void doScroll() {
//		log.trace("doScroll");
		JScrollBar scrollBar = getVerticalScrollBar();
//		int max = scrollBar.getMaximum();
//		log.trace("Max: {}", max);
		scrollBar.setValue(Integer.MAX_VALUE);
		atBottom = true;
	}

	public void setAutoScrollEnabled(boolean autoScrollEnabled) {
		this.autoScrollEnabled = autoScrollEnabled;
		doScrollIfEnabled();
	}
}
