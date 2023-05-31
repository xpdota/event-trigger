package gg.xp.xivsupport.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;

public final class UserDirPropsPersistenceProvider {
	private UserDirPropsPersistenceProvider() {
	}

	private static final Logger log = LoggerFactory.getLogger(UserDirPropsPersistenceProvider.class);

	// TODO: is this threadsafe?
	public static PropertiesFilePersistenceProvider inUserDataFolder(String baseName) {
		return inUserDataFolder(baseName, false);
	}
	public static PropertiesFilePersistenceProvider inUserDataFolder(String baseName, boolean canBeReadOnly) {
		String userDataDir = Platform.getTriggeventDir().toString();
		log.info("Data dir: {}", userDataDir);
		File file = Paths.get(userDataDir, baseName + ".properties").toFile();
		log.info("Using file: {}", file);
		return new PropertiesFilePersistenceProvider(file, canBeReadOnly);
	}
}
