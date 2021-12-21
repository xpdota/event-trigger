package gg.xp.xivsupport.persistence;

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
}
