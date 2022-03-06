package gg.xp.xivsupport.gui.tabs;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.SimplifiedPropertiesFilePersistenceProvider;
import gg.xp.xivsupport.persistence.gui.StringSettingGui;
import gg.xp.xivsupport.persistence.settings.StringSetting;
import gg.xp.xivsupport.sys.Threading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class UpdatesPanel extends TitleBorderFullsizePanel implements TabAware {
	private static final Logger log = LoggerFactory.getLogger(UpdatesPanel.class);
	private static final String propsOverrideFileName = "update.properties";
	private static final ExecutorService exs = Executors.newCachedThreadPool(Threading.namedDaemonThreadFactory("UpdateCheck"));
	private volatile UpdateCheckStatus updateCheckStatus = UpdateCheckStatus.NOT_STARTED;
	private JLabel checkingLabel;
	private File installDir;
	private File propsOverride;
	private PersistenceProvider updatePropsFilePers;
	private static final boolean isIde = Platform.isInIDE();

	private enum UpdateCheckStatus {
		NOT_STARTED,
		IN_PROGRESS,
		UPDATE_AVAILABLE,
		NO_UPDATE,
		ERROR
	}

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
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 0;
		c.weighty = 0;
		{
			checkingLabel = new JLabel("Update Status");
			doUpdateCheckInBackground();
			add(checkingLabel, c);
		}
		JButton button = new JButton("Check for Updates and Restart");
		button.addActionListener(l -> {
			// First, try to update the updater itself
			try {
				try {
					Class<?> clazz = Class.forName("gg.xp.xivsupport.gui.Update");
					clazz.getMethod("updateTheUpdater").invoke(null);
				} catch (Throwable e) {
					Class<?> clazz = Class.forName("gg.xp.xivsupport.gui.UpdateCopyForLegacyMigration");
					clazz.getMethod("updateTheUpdater").invoke(null);
				}
			}
			catch (Throwable e) {
				log.error("Error updating the updater - you may not have a recent enough version.", e);
				JOptionPane.showMessageDialog(SwingUtilities.getRoot(button), "There was an error updating the updater. This may fix itself after updates. ");
			}
			try {
				// Desktop.open seems to open it in such a way that when we exit, we release the mutex, so the updater
				// can relaunch the application correctly.
				Desktop.getDesktop().open(Paths.get(installDir.toString(), "triggevent-upd.exe").toFile());
			}
			catch (Throwable e) {
				log.error("Error launching updater", e);
				JOptionPane.showMessageDialog(SwingUtilities.getRoot(button), "There was an error launching the updater. You can try running the updater manually by running triggevent-upd.exe, or reinstall if that doesn't work.");
				return;
			}
			System.exit(0);
		});
		c.gridy++;
		add(new JLabel("Install Dir: " + installDir), c);
		c.gridy++;
		JPanel content = new JPanel();
		StringSetting setting = new StringSetting(updatePropsFilePers, "branch", "stable");
		content.add(new StringSettingGui(setting, "Branch").getComponent());
		setting.addListener(this::doUpdateCheckInBackground);
		content.add(button);
		add(content, c);
		c.gridy++;
		JButton openInstallDirButton = new JButton("Open Install Dir");
		openInstallDirButton.addActionListener(l -> GuiUtil.openFile(installDir));
		add(openInstallDirButton, c);
		c.gridy++;
		c.weighty = 1;
		add(new JPanel(), c);
	}

	private void setUpdateCheckStatus(UpdateCheckStatus updateCheckStatus) {
		this.updateCheckStatus = updateCheckStatus;
		checkingLabel.setText(
				switch (updateCheckStatus) {
					case NOT_STARTED -> "Update Status";
					case IN_PROGRESS -> "Checking for updates...";
					case NO_UPDATE -> "It looks like you are up to date.";
					case UPDATE_AVAILABLE -> "There are updates available!";
					case ERROR -> "Automatic Check Failed, but you can try updating anyway. Perhaps the branch does not exist?";
				}
		);
		if (updateCheckStatus != UpdateCheckStatus.IN_PROGRESS) {
			notifyParents();
		}
	}

	@Override
	public boolean hasWarning() {
		if (isIde) {
			return false;
		}
		return updateCheckStatus == UpdateCheckStatus.UPDATE_AVAILABLE || updateCheckStatus == UpdateCheckStatus.ERROR;
	}

	private void doUpdateCheckInBackground() {
		exs.submit(() -> {
			setUpdateCheckStatus(UpdateCheckStatus.IN_PROGRESS);
			try {
				Class<?> clazz = Class.forName("gg.xp.xivsupport.gui.Update");
				boolean result = (boolean) clazz.getMethod("justCheck", Consumer.class).invoke(null, (Consumer<String>) s -> log.info("From Updater: {}", s));
				if (result) {
					setUpdateCheckStatus(UpdateCheckStatus.UPDATE_AVAILABLE);
				}
				else {
					setUpdateCheckStatus(UpdateCheckStatus.NO_UPDATE);
				}
			}
			catch (Throwable firstError) {
				log.error("Error updating, will try backup updater", firstError);
				try {
					Class<?> clazz = Class.forName("gg.xp.xivsupport.gui.UpdateCopyForLegacyMigration");
					boolean result = (boolean) clazz.getMethod("justCheck", Consumer.class).invoke(null, (Consumer<String>) s -> log.info("From Updater: {}", s));
					if (result) {
						setUpdateCheckStatus(UpdateCheckStatus.UPDATE_AVAILABLE);
					}
					else {
						setUpdateCheckStatus(UpdateCheckStatus.NO_UPDATE);
					}
				} catch (Throwable e) {
					log.error("Error checking for updates - you may not have a recent enough version.", e);
					setUpdateCheckStatus(UpdateCheckStatus.ERROR);
				}
			}
		});
	}
}
