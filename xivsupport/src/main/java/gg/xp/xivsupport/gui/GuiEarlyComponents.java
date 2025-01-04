package gg.xp.xivsupport.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.tabs.SmartTabbedPane;
import gg.xp.xivsupport.replay.ReplayController;
import gg.xp.xivsupport.replay.gui.ReplayControllerGui;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.MutablePicoContainer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@ScanMe
public class GuiEarlyComponents {

	private final MutablePicoContainer pico;
	private Container gp;
	private JLabel loadingLabel;

	public GuiEarlyComponents(MutablePicoContainer pico) {
		this.pico = pico;
	}

	boolean isInitialized;
	private JFrame mainFrame;
	private SmartTabbedPane tabPane;
	private @Nullable TrayIcon icon;

	// to be called from gui thread only
	void init() {
		if (isInitialized) {
			return;
		}
		var wc = pico.getComponent(WindowConfig.class);
		var replay = pico.getComponent(ReplayController.class);
		mainFrame = new JFrame("Triggevent");
		tabPane = new SmartTabbedPane();
		mainFrame.setLayout(new BorderLayout());
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//			frame.setLocationByPlatform(true);
		mainFrame.setSize(1280, 960);
		mainFrame.setLocationRelativeTo(null);
		mainFrame.addWindowStateListener(new WindowAdapter() {
			@Override
			public void windowStateChanged(WindowEvent e) {
				if (wc.getMinimizeToTray().get()) {
					if ((e.getNewState() & JFrame.ICONIFIED) != 0) {
						setUpTrayIcon();
						mainFrame.setVisible(false);
					}
					else {
						mainFrame.setVisible(true);
						removeTrayIcon();
					}
				}
			}
		});
		if (wc.getStartMinimized().get() && replay == null) {
			mainFrame.setState(JFrame.ICONIFIED);
			if (wc.getMinimizeToTray().get()) {
				setUpTrayIcon();
				mainFrame.setVisible(false);
			}
			else {
				mainFrame.setVisible(true);
			}
		}
		else {
			mainFrame.setVisible(true);
		}
		mainFrame.add(tabPane);
		if (replay != null) {
			mainFrame.add(new ReplayControllerGui(pico, replay).getPanel(), BorderLayout.PAGE_START);
		}
		gp = (Container) mainFrame.getGlassPane();
		loadingLabel = new JLabel("Loading...");
		loadingLabel.setFont(loadingLabel.getFont().deriveFont(Font.PLAIN, 48));
		gp.setLayout(new GridBagLayout());
		gp.add(loadingLabel);
		gp.setVisible(true);
		isInitialized = true;
	}


	public JFrame getMainFrame() {
		return mainFrame;
	}

	public SmartTabbedPane getTabPane() {
		return tabPane;
	}

	private void removeTrayIcon() {
		if (icon != null) {
			SystemTray.getSystemTray().remove(icon);
		}
	}

	private void setUpTrayIcon() {
		if (icon == null) {
			Dimension size = SystemTray.getSystemTray().getTrayIconSize();
			icon = new TrayIcon(new ImageIcon(GeneralIcons.DAMAGE_MAGIC.getIconUrl()).getImage().getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH));
			icon.addActionListener(l -> {
				mainFrame.setVisible(true);
				mainFrame.setState(mainFrame.getState() & ~JFrame.ICONIFIED);
				mainFrame.requestFocus();
				removeTrayIcon();
			});
		}
		try {
			SystemTray.getSystemTray().add(icon);
		}
		catch (AWTException e) {
			throw new RuntimeException(e);
		}
	}

	void hideLoading() {
		gp.remove(loadingLabel);
		gp.setVisible(false);
	}

}
