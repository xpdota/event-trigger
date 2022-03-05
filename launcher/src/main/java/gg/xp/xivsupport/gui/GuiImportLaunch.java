package gg.xp.xivsupport.gui;

import gg.xp.xivsupport.eventstorage.EventReader;
import gg.xp.xivsupport.gui.components.ReadOnlyText;
import gg.xp.xivsupport.gui.util.CatchFatalError;
import gg.xp.xivsupport.persistence.Platform;
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
		CatchFatalError.run(CommonGuiSetup::setup);
		SwingUtilities.invokeLater(() -> CatchFatalError.run(() -> {
			JFrame frame = new JFrame("Triggevent Import");
			frame.setLocationByPlatform(true);
			JPanel panel = new TitleBorderFullsizePanel("Import");

			JCheckBox decompressCheckbox = new JCheckBox("Decompress events (uses more memory)");
			Path sessionsDir = Paths.get(Platform.getTriggeventDir().toString(), "sessions");
			JFileChooser sessionChooser = new JFileChooser(sessionsDir.toString());
			sessionChooser.setPreferredSize(new Dimension(800, 600));
			JButton importSessionButton = new JButton("Import Session");
			importSessionButton.addActionListener(e -> {
				int result = sessionChooser.showOpenDialog(panel);
				if (result == JFileChooser.APPROVE_OPTION) {
					File file = sessionChooser.getSelectedFile();
					// TODO: this should be async
					CatchFatalError.run(() -> {
						LaunchImportedSession.fromEvents(EventReader.readEventsFromFile(file), decompressCheckbox.isSelected());
					});
					frame.setVisible(false);
				}
			});


			Path actLogDir = Platform.getActDir();
			JFileChooser actLogChooser = new JFileChooser(actLogDir.toString());
			actLogChooser.setPreferredSize(new Dimension(800, 600));
			JButton importActLogButton = new JButton("Import ACT Log");
			importActLogButton.addActionListener(e -> {
				int result = actLogChooser.showOpenDialog(panel);
				if (result == JFileChooser.APPROVE_OPTION) {
					File file = actLogChooser.getSelectedFile();
					// TODO: this should be async
					CatchFatalError.run(() -> {
						LaunchImportedActLog.fromEvents(EventReader.readActLogFile(file), decompressCheckbox.isSelected());
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
				c.weighty = 0;
				c.gridx = 0;
				c.gridwidth = GridBagConstraints.REMAINDER;

				panel.add(decompressCheckbox, c);
			}
			{
				c.gridy++;
				c.weighty = 0;
				c.gridx = 0;
				c.gridwidth = GridBagConstraints.REMAINDER;
				panel.add(new ReadOnlyText("In replay mode, the program will use your existing settings, but any changes you make will not be saved."), c);
			}
			{
				c.gridy++;
				c.weighty = 1;
				// Filler
				panel.add(new JPanel(), c);
			}


			frame.add(panel);
			frame.setSize(new Dimension(400, 400));
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.setVisible(true);
		}));

	}
}
