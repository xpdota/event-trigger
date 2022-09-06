package gg.xp.xivsupport.gui.overlay;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.gui.CommonGuiSetup;
import gg.xp.xivsupport.gui.Refreshable;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.ColorSetting;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
import gg.xp.xivsupport.persistence.settings.FontSetting;
import gg.xp.xivsupport.speech.BasicCalloutEvent;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger log = LoggerFactory.getLogger(FlyingTextOverlay.class);

	private final List<VisualCalloutItem> currentCallouts = new ArrayList<>();
	private final SimpleAttributeSet attribs;
	private volatile List<VisualCalloutItem> currentCalloutsTmp = Collections.emptyList();
	private final Object lock = new Object();
	// Will be rubber-stamped like a table cell renderer
	private static final Color defaultTextColor = new Color(255, 255, 64, 255);
	private static final Color backdropColor = new Color(20, 21, 22, 128);
	private static final Color transparentColor = new Color(0, 0, 0, 0);
	private static final int textPadding = 10;
	private final InnerPanel innerPanel;
	private final int templateHeight;
	private final EnumSetting<TextAlignment> alignmentSetting;
	private final ColorSetting textColorSetting;
	private final FontSetting textFontSetting;

	public FlyingTextOverlay(PersistenceProvider pers, OverlayConfig oc) {
		super("Callout Text", "callout-text-overlay", oc, pers);
		alignmentSetting = new EnumSetting<>(pers, "callout-text-overlay.text-alignment", TextAlignment.class, TextAlignment.CENTER);
		textColorSetting = new ColorSetting(pers, "callout-text-overlay.text-color", defaultTextColor);
		textFontSetting = new FontSetting(pers, "callout-text-overlay.text-font", new JLabel().getFont().getFontName(), 24);
		JLabel templateJLabel = new JLabel();
		templateJLabel.setFont(textFontSetting.get());
		templateJLabel.setText("A");
		templateHeight = templateJLabel.getPreferredSize().height;
		RefreshLoop<FlyingTextOverlay> refresher = new RefreshLoop<>("CalloutOverlay", this, FlyingTextOverlay::refresh, i -> i.calculateUnscaledFrameTime(50));
		innerPanel = new InnerPanel();
		innerPanel.setPreferredSize(new Dimension(400, 220));
		getPanel().add(innerPanel);
		attribs = new SimpleAttributeSet();
		recheckAlignment();
		alignmentSetting.addListener(this::recheckAlignment);
		refresher.start();
	}

	private void recheckAlignment() {
		StyleConstants.setAlignment(attribs, switch (alignmentSetting.get()) {
			case LEFT -> StyleConstants.ALIGN_LEFT;
			case CENTER -> StyleConstants.ALIGN_CENTER;
			case RIGHT -> StyleConstants.ALIGN_RIGHT;
		});

	}

	private final class VisualCalloutItem {
		private final CalloutEvent event;
		private final SimpleMultiLineText text;
		private final int centerX;
		private final int width;
		private String prevText = "";
		// TODO: do these all need to be volatile?
		private volatile int leftOuterGradientBound;
		private volatile int rightOuterGradientBound;
		private volatile int heightOfThisItem;
		private volatile int leftInnerGradientBound;
		private volatile int rightInnerGradientBound;
		private volatile int ecXoffset;
		private volatile int textXoffset;
		private final @Nullable Component extraComponent;
		private int textLeftBound;
		private static final int maxGradientWidth = 50;

		private VisualCalloutItem(CalloutEvent event) {
			this.event = event;
			text = new SimpleMultiLineText();
			text.setAlignment(alignmentSetting.get());
			text.setFont(textFontSetting.get());
			width = innerPanel.getWidth();
			centerX = width >> 1;
			{
				extraComponent = event.graphicalComponent();
				if (extraComponent == null) {
					textLeftBound = textPadding;
				}
				else {
					Dimension oldPref = extraComponent.getPreferredSize();
					int newPrefHeight = templateHeight;
					// New width is template height times preferred aspect ratio, but not to be more than 4:1
					int extraComponentDesiredWidth = (int) Math.min(newPrefHeight * 4, oldPref.getWidth() / oldPref.getHeight() * newPrefHeight);
					if (extraComponentDesiredWidth == 0) {
						// If no preferred size set, just assume 1:1 is fine
						//noinspection SuspiciousNameCombination
						extraComponentDesiredWidth = newPrefHeight;
					}
					extraComponent.setBounds(0, 0, extraComponentDesiredWidth, newPrefHeight);
					extraComponent.validate();
					textLeftBound = Math.max(textPadding, extraComponentDesiredWidth);
				}
			}
			text.setBounds(textLeftBound, 0, width - textLeftBound - 2 * textPadding, 1);
			int preferredHeight = text.getTextHeight();
//			text.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
			text.setBounds(textLeftBound, 0, width - textLeftBound - 2 * textPadding, preferredHeight);
			Color colorOverride = event.getColorOverride();
			if (colorOverride == null) {
				text.setForeground(textColorSetting.get());
			}
			else {
				text.setForeground(colorOverride);
			}
			recheckText();
			recheckPositions();
		}

		private void recheckPositions() {
			int preferredTextWidth = this.text.getTextWidth();
			Component ec = extraComponent;
			int extraComponentWidth = ec == null ? 0 : ec.getWidth() + textPadding;
			int contentWidth = Math.min(preferredTextWidth + extraComponentWidth, width - 2 * textPadding);
			int textHeight = this.text.getTextHeight();
			int extraSpacePerSide = Math.max(10, (width - contentWidth) / 2);
			heightOfThisItem = ec == null ? textHeight : Math.max(textHeight, ec.getHeight());
			int gradientWidth = Math.min(maxGradientWidth, extraSpacePerSide);
			// TODO: these aren't the actual bounds
			switch (alignmentSetting.get()) {
				case LEFT -> {
					leftOuterGradientBound = 0;
					leftInnerGradientBound = gradientWidth;
					rightInnerGradientBound = leftInnerGradientBound + contentWidth;
					rightOuterGradientBound = rightInnerGradientBound + gradientWidth;
				}
				case CENTER -> {
					leftInnerGradientBound = centerX - contentWidth / 2;
					rightInnerGradientBound = centerX + contentWidth / 2;
					leftOuterGradientBound = leftInnerGradientBound - gradientWidth;
					rightOuterGradientBound = rightInnerGradientBound + gradientWidth;
				}
				case RIGHT -> {
					rightOuterGradientBound = width;
					rightInnerGradientBound = rightOuterGradientBound - gradientWidth;
					leftInnerGradientBound = rightInnerGradientBound - contentWidth;
					leftOuterGradientBound = leftInnerGradientBound - gradientWidth;
				}
			}
			ecXoffset = leftInnerGradientBound;
			textXoffset = ecXoffset + extraComponentWidth;
		}

		private void paint(Graphics2D graphics) {
			GradientPaint paintLeft = new GradientPaint(leftOuterGradientBound, 0, transparentColor, leftInnerGradientBound, 0, backdropColor);
			graphics.setPaint(paintLeft);
			graphics.fillRect(leftOuterGradientBound, 0, leftInnerGradientBound - leftOuterGradientBound, heightOfThisItem);
			graphics.setPaint(backdropColor);
			graphics.fillRect(leftInnerGradientBound, 0, rightInnerGradientBound - leftInnerGradientBound, heightOfThisItem);
			GradientPaint paintRight = new GradientPaint(rightInnerGradientBound, 0, backdropColor, rightOuterGradientBound, 0, transparentColor);
			graphics.setPaint(paintRight);
			graphics.fillRect(rightInnerGradientBound, 0, rightOuterGradientBound - rightInnerGradientBound, heightOfThisItem);
			Font oldFont = graphics.getFont();
			graphics.translate(textXoffset, 0);
			try {
				this.text.paintMinimumSquare(graphics);
			}
			catch (Throwable t) {
				log.error("Error rendering text for '{}' (from {})", prevText, event.getCallText(), t);
			}
			graphics.setFont(oldFont);
			Component extra = this.extraComponent;
			if (extra != null) {
				graphics.translate(ecXoffset - textXoffset, 0);
				try {
					extra.paint(graphics);
				}
				catch (Throwable t) {
					log.error("Error rendering extra component for '{}' (from {})", prevText, event.getCallText(), t);
				}
			}
		}

		public boolean isExpired() {
			return event.isExpired();
		}

		public int getHeight() {
			return heightOfThisItem;
		}

		public void recheckText() {
			try {
				String newText = event.getVisualText();
				if (!Objects.equals(newText, prevText)) {
					text.setText(newText);
					prevText = newText;
					int textHeight = this.text.getTextHeight();
					if (extraComponent != null) {
						recheckPositions();
					}
					else {
						heightOfThisItem = textHeight;
						recheckPositions();
					}
				}
			}
			catch (Throwable t) {
				log.error("Error updating text", t);
			}
			if (extraComponent != null && extraComponent instanceof Refreshable ref) {
				try {
					ref.refresh();
				}
				catch (Throwable t) {
					log.error("Error updating visual component", t);
				}
			}
		}
	}


	private void addCallout(CalloutEvent callout) {
		synchronized (lock) {
			for (int i = 0; i < currentCallouts.size(); i++) {
				if (callout.shouldReplace(currentCallouts.get(i).event)) {
					currentCallouts.set(i, new VisualCalloutItem(callout));
					return;
				}
			}
			currentCallouts.add(new VisualCalloutItem(callout));
		}
	}

	private void refreshCallouts() {
		synchronized (lock) {
			currentCallouts.removeIf(visualCalloutItem -> {
				boolean expired = visualCalloutItem.isExpired();
				if (expired) {
					log.info("Removed call: {}", visualCalloutItem.event.getVisualText());
				}
				return expired;
			});
			currentCallouts.forEach(VisualCalloutItem::recheckText);
		}
	}

	@HandleEvents
	public void handleEvent(EventContext context, CalloutEvent event) {
		if (event.getVisualText() != null && !event.getVisualText().isBlank()) {
			log.info("Added call: {}", event.getVisualText());
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
			if (currentCalloutsTmp.isEmpty()) {
				return;
			}
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

	public EnumSetting<TextAlignment> getAlignmentSetting() {
		return alignmentSetting;
	}

	public ColorSetting getTextColorSetting() {
		return textColorSetting;
	}

	public FontSetting getTextFontSetting() {
		return textFontSetting;
	}

	public static void main(String[] args) {
		CommonGuiSetup.setup();
		{
			InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
			pers.save("callout-text-overlay.text-alignment", "RIGHT");
			OverlayConfig oc = new OverlayConfig(pers);
			FlyingTextOverlay overlay = new FlyingTextOverlay(pers, oc);
			overlay.finishInit();
			overlay.setVisible(true);
			overlay.setEditMode(true);
			overlay.getEnabled().set(true);
			double scaleFactor = 2.5;
			overlay.setScale(scaleFactor);
			overlay.addCallout(new BasicCalloutEvent(null, "One", 5000) {
				@Override
				public @NotNull Component graphicalComponent() {
					return new JButton("Short");
				}

			});
			overlay.addCallout(new BasicCalloutEvent(null, "This second callout is longer", 15000) {
				@Override
				public @NotNull Component graphicalComponent() {
					return new JButton("Foo");
				}
			});
			overlay.addCallout(new BasicCalloutEvent(null, "Three", 255000));
			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
			overlay.addCallout(new BasicCalloutEvent(null, "This one is so long, it probably isn't going to fit on one row!", 255000) {
				@Override
				public @NotNull Component graphicalComponent() {
					return new JButton("Bar");
				}
			});
			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000) {
				@Override
				public @NotNull Component graphicalComponent() {
					return new JButton("Baz");
				}
			});
			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
		}

	}

}
