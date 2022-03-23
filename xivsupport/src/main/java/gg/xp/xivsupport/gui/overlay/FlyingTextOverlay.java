package gg.xp.xivsupport.gui.overlay;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.gui.CommonGuiSetup;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.speech.BasicCalloutEvent;
import gg.xp.xivsupport.speech.CalloutEvent;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FlyingTextOverlay extends XivOverlay {

	private final List<VisualCalloutItem> currentCallouts = new ArrayList<>();
	private final Font font;
	private final SimpleAttributeSet attribs;
	private volatile List<VisualCalloutItem> currentCalloutsTmp = Collections.emptyList();
	private final Object lock = new Object();
	// Will be rubber-stamped like a table cell renderer
	private static final Color color = new Color(255, 255, 64, 255);
	private static final Color backdropColor = new Color(20, 21, 22, 128);
	private static final Color transparentColor = new Color(0, 0, 0, 0);
	private final InnerPanel innerPanel;

	public FlyingTextOverlay(PersistenceProvider pers) {
		super("Callout Text", "callout-text-overlay", pers);
		font = new JLabel().getFont().deriveFont(new AffineTransform(2, 0, 0, 2, 0, 0));
		RefreshLoop<FlyingTextOverlay> refresher = new RefreshLoop<>("CalloutOverlay", this, FlyingTextOverlay::refresh, unused -> 100L);
		innerPanel = new InnerPanel();
		innerPanel.setPreferredSize(new Dimension(400, 220));
		getPanel().add(innerPanel);
		attribs = new SimpleAttributeSet();
		StyleConstants.setAlignment(attribs, StyleConstants.ALIGN_CENTER);
		refresher.start();
	}

	private final class VisualCalloutItem {
		private final CalloutEvent event;
		private final JTextPane text;
		private String prevText = "";
		private final int leftGradientBound;
		private final int rightGradientBound;
		private final int heightOfThisItem;
		private final int leftTextBound;
		private final int rightTextBound;

		private VisualCalloutItem(CalloutEvent event) {
			this.event = event;
			text = new JTextPane();
			text.setParagraphAttributes(attribs, false);
			recheckText();
			text.setAlignmentX(Component.CENTER_ALIGNMENT);
			text.setOpaque(false);
			text.setFont(font);
			text.setEditable(false);
			int width = innerPanel.getWidth();
			int preferredWidth = text.getPreferredSize().width;
			text.setBorder(null);
			text.setFocusable(false);
			int centerX = width >> 1;
			this.text.setBounds(0, 0, width, 1);
			int preferredHeight = text.getPreferredSize().height;
			text.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
			this.text.setBounds(0, 0, width, preferredHeight);
			heightOfThisItem = this.text.getPreferredSize().height;
			this.text.setForeground(color);
			int preferredTextWidth = this.text.getPreferredSize().width;
			// TODO: these aren't the actual bounds
			leftTextBound = Math.max(centerX - (preferredTextWidth >> 1), 10);
			rightTextBound = Math.min(centerX + (preferredTextWidth >> 1), width - 10);
			int gradientWidth = 50;
			leftGradientBound = Math.max(leftTextBound - gradientWidth, 0);
			rightGradientBound = Math.min(rightTextBound + gradientWidth, width);
		}

		private void paint(Graphics2D graphics) {
			GradientPaint paintLeft = new GradientPaint(leftGradientBound, 0, transparentColor, leftTextBound, 0, backdropColor);
			graphics.setPaint(paintLeft);
			graphics.fillRect(leftGradientBound, 0, leftTextBound - leftGradientBound, heightOfThisItem);
			graphics.setPaint(backdropColor);
			graphics.fillRect(leftTextBound, 0, rightTextBound - leftTextBound, heightOfThisItem);
			GradientPaint paintRight = new GradientPaint(rightTextBound, 0, backdropColor, rightGradientBound, 0, transparentColor);
			graphics.setPaint(paintRight);
			graphics.fillRect(rightTextBound, 0, rightGradientBound - rightTextBound, heightOfThisItem);
			graphics.setFont(text.getFont());
			this.text.paint(graphics);
		}

		public boolean isExpired() {
			return event.isExpired();
		}

		public int getHeight() {
			return text.getPreferredSize().height;
		}

		public void recheckText() {
			String newText = event.getVisualText();
			if (!Objects.equals(newText, prevText)) {
				text.setText(prevText = newText);
			}
		}
	}


	private void addCallout(CalloutEvent callout) {
		synchronized (lock) {
			currentCallouts.add(new VisualCalloutItem(callout));
		}
	}

	private void refreshCallouts() {
		synchronized (lock) {
			currentCallouts.removeIf(VisualCalloutItem::isExpired);
			currentCallouts.forEach(VisualCalloutItem::recheckText);
		}
	}

	@HandleEvents
	public void handleEvent(EventContext context, CalloutEvent event) {
		if (event.getVisualText() != null && !event.getVisualText().isBlank()) {
			addCallout(event);
		}
	}

	@HandleEvents
	public void pullEnded(EventContext context, PullStartedEvent pse) {
		synchronized (lock) {
			currentCallouts.clear();
		}
	}

	private class InnerPanel extends JPanel {
		@Serial
		private static final long serialVersionUID = 6727734196395717257L;

		@Override
		public boolean isOpaque() {
			return false;
		}

		@Override
		public void paint(Graphics g) {
			Graphics2D graphics = (Graphics2D) g;
			AffineTransform oldTransform = graphics.getTransform();
			AffineTransform newTransform = new AffineTransform(oldTransform);
//			g.clearRect(0, 0, getWidth(), getHeight());
			int curY = 0;
			for (VisualCalloutItem ce : currentCalloutsTmp) {
				if (curY + ce.getHeight() > getHeight()) {
					break;
				}
				ce.paint(graphics);
				int height = ce.getHeight();
				newTransform.translate(0, height + 5);
				curY += height;
				graphics.setTransform(newTransform);
			}
			graphics.setTransform(oldTransform);
		}
	}


	private void refresh() {
		refreshCallouts();
		synchronized (lock) {
			currentCalloutsTmp = new ArrayList<>(currentCallouts);
		}
		SwingUtilities.invokeLater(innerPanel::repaint);
	}
	// TODO: smooth transition

	public static void main(String[] args) {
		CommonGuiSetup.setup();
		{
			FlyingTextOverlay overlay = new FlyingTextOverlay(new InMemoryMapPersistenceProvider());
			overlay.finishInit();
			overlay.setVisible(true);
			overlay.setEditMode(true);
			overlay.getEnabled().set(true);
			double scaleFactor = 1.5;
			overlay.setScale(scaleFactor);
			overlay.addCallout(new BasicCalloutEvent(null, "One", 5000));
			overlay.addCallout(new BasicCalloutEvent(null, "This second callout is longer", 15000));
			overlay.addCallout(new BasicCalloutEvent(null, "Three", 255000));
			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
			overlay.addCallout(new BasicCalloutEvent(null, "This one is so long, it isn't going to fit on the screen", 255000));
			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
		}

	}

}
