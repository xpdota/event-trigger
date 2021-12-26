package gg.xp.xivsupport.gui.overlay;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.speech.CalloutEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

public class FlyingTextOverlay {

	private final List<CalloutEvent> currentCallouts = new ArrayList<>();
	private final Object lock = new Object();
	// Will be rubber-stamped like a table cell renderer
	private final JLabel labelStamp;
	private final Color color = new Color(255, 255, 64, 255);

	public FlyingTextOverlay() {
		labelStamp = new JLabel();
		labelStamp.setFont(labelStamp.getFont().deriveFont(new AffineTransform(2, 0, 0, 0, 2, 0)));
		labelStamp.setForeground(color);
//		new RefreshLoop<>("FlyingTextOv")
	}

	private void addCallout(CalloutEvent callout) {
		synchronized (lock) {
			currentCallouts.add(callout);
		}
	}

	private void removeExpiredCallouts() {
		synchronized (lock) {
			currentCallouts.removeIf(CalloutEvent::isExpired);
		}
	}

	@HandleEvents
	public void handleEvent(EventContext context, CalloutEvent event) {
		if (event.getVisualText() != null) {
			addCallout(event);
		}
	}


}
