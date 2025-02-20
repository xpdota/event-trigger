package gg.xp.xivsupport.gui;

import com.formdev.flatlaf.FlatLaf;
import gg.xp.xivsupport.gui.overlay.Scaled;
import gg.xp.xivsupport.gui.themes.BuiltinTheme;
import gg.xp.xivsupport.persistence.settings.EnumSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class CommonGuiSetup {
	private static final Logger log = LoggerFactory.getLogger(CommonGuiSetup.class);

	private static final Object lock = new Object();
	private static volatile boolean done;

	private enum RenderMode {
		// D3D9 + software image scaling
		D3D,
		// D3D9 + hardware image scaling
		D3D_HWSCALE,
		// OpenGL
		OPENGL,
		// Software
		SOFTWARE
	}

	private static final RenderMode mode = RenderMode.SOFTWARE;
	private static final int GUI_WARN_MS = 200;

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
		switch (mode) {
			case D3D -> System.setProperty("sun.java2d.d3d", "true");
			case D3D_HWSCALE -> {
				System.setProperty("sun.java2d.d3d", "true");
				System.setProperty("sun.java2d.ddscale", "true");
			}
			case OPENGL -> {
				System.setProperty("sun.java2d.opengl", "true");
				System.setProperty("sun.java2d.d3d", "false");
			}
			case SOFTWARE -> {
				System.setProperty("sun.java2d.opengl", "false");
				System.setProperty("sun.java2d.d3d", "false");
			}
		}
		EnumSetting<BuiltinTheme> themeSetting = WindowConfig.getThemeSettingStatic();
		log.info("Theme: {}", themeSetting.get());
		themeSetting.addAndRunListener(() -> setTheme(themeSetting.get()));
		SwingUtilities.invokeLater(() -> {
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		});
		// Increase tooltip hang time from 4s to 60s
		ToolTipManager.sharedInstance().setDismissDelay(60_000);
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		if (toolkit.isDynamicLayoutActive()) {
			toolkit.setDynamicLayout(true);
		}
		EventQueue queue = toolkit.getSystemEventQueue();
		CompletableFuture<Monitor> monFuture = new CompletableFuture<>();
		SwingUtilities.invokeLater(() -> {
			Monitor monitor = new Monitor(Thread.currentThread());
//			monitor.start();
			monFuture.complete(monitor);
		});
		final Monitor monitor;
		log.info("Waiting for monitor setup");
		Monitor monitorTmp;
		try {
			monitorTmp = monFuture.get(5, TimeUnit.SECONDS);
		}
		catch (Throwable e) {
			log.info("Error setting up gui performance monitor", e);
			monitorTmp = null;
		}
		log.info("Monitor setup done");
		monitor = monitorTmp;
//		monitor = null;
		queue.push(new EventQueue() {
			@Override
			protected void dispatchEvent(AWTEvent event) {
				long timeBefore = System.currentTimeMillis();
				try {
					if (event.getClass().equals(MouseEvent.class)) {
						MouseEvent mouseEvent = (MouseEvent) event;
						Object source = mouseEvent.getSource();
						if (source instanceof Scaled scaled && source instanceof Component) {
							MouseEvent newEvent = new MouseEvent(
									(Component) scaled,
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
//					if (event instanceof KeyEvent key) {
//						String d = InputEvent.getModifiersExText(key.getModifiersEx()) + " + " + KeyEvent.getKeyText(key.getKeyCode());
//
//						log.info("Key event: {}", d);
//					}
					super.dispatchEvent(event);
				}
				finally {
					long timeAfter = System.currentTimeMillis();
					long delta = timeAfter - timeBefore;
					// TODO find good value for this - 100 might be a little low
					if (delta > GUI_WARN_MS) {
						log.warn("Slow GUI performance: took {}ms to dispatch event {}", delta, event);
						if (monitor != null) {
							StackTraceElement[] lastStackTrace = monitor.getLastStackTrace();
							if (lastStackTrace != null && lastStackTrace.length > 0) {
								RuntimeException dummyException = new RuntimeException("Dummy Exception");
								dummyException.setStackTrace(lastStackTrace);
								log.warn("Possible stuck point", dummyException);
							}
						}
					}
				}
			}
		});
	}

	private static void setTheme(BuiltinTheme builtinTheme) {
		try {
			UIManager.setLookAndFeel(builtinTheme.getLaf());
			FlatLaf.updateUI();
		}
		catch (Throwable t) {
			log.error("Error setting up look and feel", t);
		}
	}

	private static class Monitor extends Thread {

		private final Thread threadToWatch;
		private volatile StackTraceElement[] lastStackTrace;

		public Monitor(Thread threadToWatch) {
			setDaemon(true);
			setName("MonitorFor-" + threadToWatch.getName());
			this.threadToWatch = threadToWatch;
		}

		public StackTraceElement[] getLastStackTrace() {
			return lastStackTrace;
		}

		@SuppressWarnings("BusyWait")
		@Override
		public void run() {
			log.info("Starting monitor for thread {}", threadToWatch);
			while (true) {
				lastStackTrace = threadToWatch.getStackTrace();
				try {
					Thread.sleep(50);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
