package gg.xp.xivsupport.persistence;

import gg.xp.xivsupport.gui.tabs.UpdatesPanel;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
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

	private static final Logger log = LoggerFactory.getLogger(Platform.class);

	private Platform() {
	}

	private static @Nullable String getProp(String prop) {
		String fromSysProps = System.getProperty(prop);
		if (fromSysProps != null && !fromSysProps.isBlank()) {
			return fromSysProps;
		}
		String fromEnv = System.getenv(prop);
		if (fromEnv != null && !fromEnv.isBlank()) {
			return fromEnv;
		}
		return null;
	}

	/**
	 * @return The settings dir
	 */
	public static Path getTriggeventDir() {
		String override = getProp("triggevent_settings_dir");
		if (override != null) {
			File fileMaybe = new File(override);
			if (fileMaybe.isDirectory()) {
				return fileMaybe.toPath();
			}
			else {
				log.info("Invalid override path: '{}'", override);
			}
		}
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

	public static void executeUpdater() throws IOException {
		// Desktop.open seems to open it in such a way that when we exit, we release the mutex, so the updater
		// can relaunch the application correctly.
		if (isWindows()) {
			Desktop.getDesktop().open(Paths.get(getInstallDir().toString(), "triggevent-upd.exe").toFile());
		}
		else {
			Runtime.getRuntime().exec(new String[]{"sh", "triggevent-upd.sh"});
		}
	}

	public static void showFileInExplorer(File file) {
		try {
			if (isWindows() && !file.toString().contains("\"")) {
				// Come on Oracle, why is Desktop.browseFileDirectory() "unsupported" on Windows when
				// it's literally just this?
				Runtime.getRuntime().exec(new String[]{"explorer.exe", "/select,\"" + file + '"'});
			}
			else {
				Desktop.getDesktop().open(file.getParentFile());
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
