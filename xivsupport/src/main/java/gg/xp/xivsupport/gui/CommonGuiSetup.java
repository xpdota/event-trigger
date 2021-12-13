package gg.xp.xivsupport.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import gg.xp.xivsupport.gui.overlay.Scaled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public final class CommonGuiSetup {
	private static final Logger log = LoggerFactory.getLogger(CommonGuiSetup.class);

	private static final Object lock = new Object();
	private static volatile boolean done;

	private CommonGuiSetup() {
	}

	public static void setup() {
		if (!done) {
			synchronized (lock) {
				if (!done) {
					doSetup();
					done = true;
				}
			}
		}
	}

	private static void doSetup() {
		try {
//			UIManager.setLookAndFeel(new DarculaLaf());
			UIManager.setLookAndFeel(new FlatDarculaLaf());
		}
		catch (Throwable t) {
			log.error("Error setting up look and feel", t);
		}
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		if (toolkit.isDynamicLayoutActive()) {
			toolkit.setDynamicLayout(true);
		}
		EventQueue queue = toolkit.getSystemEventQueue();
		queue.push(new EventQueue() {
			@Override
			protected void dispatchEvent(AWTEvent event) {
				long timeBefore = System.currentTimeMillis();
				try {
					if (event.getClass().equals(MouseEvent.class)) {
						MouseEvent mouseEvent = (MouseEvent) event;
						Object source = mouseEvent.getSource();
						if (source instanceof Scaled) {
							Scaled scaled = (Scaled) source;
							MouseEvent newEvent = new MouseEvent(
									(Component) source,
									mouseEvent.getID(),
									mouseEvent.getWhen(),
									mouseEvent.getModifiersEx(),
									(int) (mouseEvent.getX() / scaled.getScaleFactor()),
									(int) (mouseEvent.getY() / scaled.getScaleFactor()),
									mouseEvent.getXOnScreen(),
									mouseEvent.getYOnScreen(),
									mouseEvent.getClickCount(),
									mouseEvent.isPopupTrigger(),
									mouseEvent.getButton()
							);
							super.dispatchEvent(newEvent);
							return;
						}

					}
					super.dispatchEvent(event);
				}
				finally {
					long timeAfter = System.currentTimeMillis();
					long delta = timeAfter - timeBefore;
					// TODO find good value for this - 100 might be a little low
					if (delta > 100) {
						log.warn("Slow GUI performance: took {}ms to dispatch event {}", delta, event);
					}
				}
			}
		});

	}
}
