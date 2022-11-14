package gg.xp.xivsupport.persistence;

import gg.xp.xivsupport.gui.tabs.UpdatesPanel;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

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

	public static Path getAddonsDir() {
		return Paths.get(getInstallDir().toString(), "addon");
	}

	public static List<URL> getAddonJars() {
		File[] addonDirs = getAddonsDir().toFile().listFiles(File::isDirectory);
		if (addonDirs == null) {
			return Collections.emptyList();
		}
		return Arrays.stream(addonDirs)
				.flatMap(addonDir -> {
					File[] subFiles = addonDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
					return subFiles == null ? Stream.empty() : Arrays.stream(subFiles);
				})
				.map(file -> {
					try {
						return file.toURI().toURL();
					}
					catch (MalformedURLException e) {
						throw new RuntimeException(e);
					}
				})
				.toList();
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
