package gg.xp.xivsupport.persistence;

import gg.xp.xivsupport.gui.tabs.UpdatesPanel;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public final class Platform {
	private Platform() {
	}

	public static Path getTriggeventDir() {
		String appData = System.getenv("APPDATA");
		Path userDataDir;
		if (appData == null) {
			String homeDir = System.getProperty("user.home");
			// *nix
			userDataDir = Paths.get(homeDir, ".triggevent");
		}
		else {
			// win
			userDataDir = Paths.get(appData, "triggevent");
		}
		return userDataDir;
	}

	public static Path getSessionsDir() {
		return Paths.get(getTriggeventDir().toString(), "sessions");
	}

	public static Path getGroovyDir() {
		return Paths.get(getTriggeventDir().toString(), "userscripts");
	}

	public static Path getActDir() {
		String appData = System.getenv("APPDATA");
		Path userDataDir;
		if (appData == null) {
			String homeDir = System.getProperty("user.home");
			// *nix
			userDataDir = Paths.get(homeDir);
		}
		else {
			// win
			userDataDir = Paths.get(appData, "Advanced Combat Tracker", "FFXIVLogs");
		}
		return userDataDir;
	}

	public static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");
	}

	public static boolean isInIDE() {
		File jarLocation;
		try {
			jarLocation = new File(UpdatesPanel.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			return jarLocation.isDirectory();
		}
		catch (Throwable e) {
			return false;
		}
	}

	public static File getInstallDir() {
		File jarLocation;
		try {
			jarLocation = new File(UpdatesPanel.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			if (jarLocation.isFile()) {
				jarLocation = jarLocation.getParentFile();
			}
			return jarLocation.getParentFile();
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}
