package gg.xp.xivsupport.gui;

import gg.xp.xivsupport.eventstorage.EventReader;
import gg.xp.xivsupport.gui.util.CatchFatalError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class GuiImportLaunch {
	private static final Logger log = LoggerFactory.getLogger(GuiImportLaunch.class);

	private GuiImportLaunch() {
	}

	public static void main(String[] args) {
		log.info("GUI Import Init");
		CatchFatalError.run(() -> {
			CommonGuiSetup.setup();
			JFrame frame = new JFrame("Triggevent Import");
			frame.setLocationByPlatform(true);
			JPanel panel = new TitleBorderFullsizePanel("Import");
			String userDataDir = System.getenv("APPDATA");
//			Path sessionsDir = Paths.get(userDataDir, "Advanced Combat Tracker", "FFXIVLogs");


			Path sessionsDir = Paths.get(userDataDir, "triggevent", "sessions");
			JFileChooser sessionChooser = new JFileChooser(sessionsDir.toString());
			JButton importSessionButton = new JButton("Import Session");
			importSessionButton.addActionListener(e -> {
				int result = sessionChooser.showOpenDialog(panel);
				if (result == JFileChooser.APPROVE_OPTION) {
					File file = sessionChooser.getSelectedFile();
					// TODO: this should be async
					CatchFatalError.run(() -> {
						LaunchImportedSession.fromEvents(EventReader.readEventsFromFile(file));
					});
					frame.setVisible(false);
				}
			});


			Path actLogDir = Paths.get(userDataDir, "Advanced Combat Tracker", "FFXIVLogs");
			JFileChooser actLogChooser = new JFileChooser(actLogDir.toString());
			JButton importActLogButton = new JButton("Import ACT Log");
			importActLogButton.addActionListener(e -> {
				int result = actLogChooser.showOpenDialog(panel);
				if (result == JFileChooser.APPROVE_OPTION) {
					File file = actLogChooser.getSelectedFile();
					// TODO: this should be async
					CatchFatalError.run(() -> {
						LaunchImportedActLog.fromEvents(EventReader.readActLogFile(file));
					});
					frame.setVisible(false);
				}
			});

			panel.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();

			c.gridx = 0;
			c.gridy = 0;
			c.insets = new Insets(10, 10, 10, 10);
			c.fill = GridBagConstraints.HORIZONTAL;
			{
				c.weightx = 1;
				c.gridwidth = GridBagConstraints.REMAINDER;
				JLabel importLabel = new JLabel("Please select a file to import");
				panel.add(importLabel, c);
			}
			{
				c.anchor = GridBagConstraints.LINE_START;
				c.gridy++;
				c.gridwidth = 1;
				c.weightx = 0;
				panel.add(importSessionButton, c);
				c.gridx++;
				c.weightx = 1;
				panel.add(new JLabel("Import a Triggevent Session"), c);
			}
			{
				c.gridy++;
				c.gridx = 0;
				c.gridwidth = 1;
				c.weightx = 0;
				panel.add(importActLogButton, c);
				c.gridx++;
				c.weightx = 1;
				panel.add(new JLabel("Import an ACT Log file"), c);
			}
			{
				c.gridy++;
				c.weighty = 1;
				c.gridx = 0;
				c.gridwidth = GridBagConstraints.REMAINDER;
				// Filler
				panel.add(new JPanel(), c);
			}


			frame.add(panel);
			frame.setSize(new Dimension(400, 400));
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.setVisible(true);
		});

	}
}
