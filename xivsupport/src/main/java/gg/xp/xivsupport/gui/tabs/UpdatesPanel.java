package gg.xp.xivsupport.gui.tabs;

import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.SimplifiedPropertiesFilePersistenceProvider;
import gg.xp.xivsupport.persistence.gui.StringSettingGui;
import gg.xp.xivsupport.persistence.settings.StringSetting;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.prefs.PreferenceChangeEvent;

public class UpdatesPanel extends TitleBorderFullsizePanel {
	private static final Logger log = LoggerFactory.getLogger(UpdatesPanel.class);
	private static final String propsOverrideFileName = "update.properties";
	private File installDir;
	private File propsOverride;
	private PersistenceProvider updatePropsFilePers;

	public UpdatesPanel() {
		super("Updates");
		try {
			this.installDir = Platform.getInstallDir();
			propsOverride = Paths.get(installDir.toString(), propsOverrideFileName).toFile();
			updatePropsFilePers = new SimplifiedPropertiesFilePersistenceProvider(propsOverride);
		}
		catch (Throwable e) {
			log.error("Error setting up updates tab", e);
			add(new JLabel("There was an error. You can try running the updater manually by running triggevent-upd.exe."));
			return;
		}
//		setLayout(new BorderLayout());
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 0;
		c.weighty = 0;
		JButton button = new JButton("Check for Updates and Restart");
		button.addActionListener(l -> {
			try {
				// Desktop.open seems to open it in such a way that when we exit, we release the mutex, so the updater
				// can relaunch the application correctly.
				Desktop.getDesktop().open(Paths.get(installDir.toString(), "triggevent-upd.exe").toFile());
			}
			catch (IOException e) {
				log.error("Error launching updater", e);
				JOptionPane.showMessageDialog(SwingUtilities.getRoot(button), "There was an error launching the updater. You can try running the updater manually by running triggevent-upd.exe.");
				return;
			}
			System.exit(0);
		});
		add(new JLabel("Install Dir: " + installDir), c);
		c.gridy++;
		JPanel content = new JPanel();
		content.add(new StringSettingGui(new StringSetting(updatePropsFilePers, "branch", "stable"), "Branch").getComponent());
		content.add(button);
		add(content, c);
		c.gridy++;
		JButton openInstallDirButton = new JButton("Open Install Dir");
		openInstallDirButton.addActionListener(l -> {
			try {
				Desktop.getDesktop().open(installDir);
			}
			catch (IOException e) {
				log.error("Error opening install dir", e);
				throw new RuntimeException(e);
			}
		});
		add(openInstallDirButton, c);
		c.gridy++;
		c.weighty = 1;
		add(new JPanel());
	}
}
