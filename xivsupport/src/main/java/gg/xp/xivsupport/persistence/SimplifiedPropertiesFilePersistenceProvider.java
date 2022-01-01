package gg.xp.xivsupport.persistence;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SimplifiedPropertiesFilePersistenceProvider extends BaseStringPersistenceProvider {

	private static final Logger log = LoggerFactory.getLogger(SimplifiedPropertiesFilePersistenceProvider.class);
	private final Properties properties;
	private final File file;

	public SimplifiedPropertiesFilePersistenceProvider(File file) {
		this.file = file;
		Properties properties = new Properties();
		try {
			try (FileInputStream stream = new FileInputStream(file)) {
				properties.load(stream);
			}
		}
		catch (FileNotFoundException e) {
			log.info("Properties file does not yet exist");
		}
		catch (IOException e) {
			throw new RuntimeException("Could not load properties", e);
		}
		this.properties = properties;
	}

	private void writeChangesToDisk() {
		try {
			File parentFile = file.getParentFile();
			if (parentFile != null) {
				parentFile.mkdirs();
			}
			try (FileOutputStream stream = new FileOutputStream(file)) {
				properties.store(stream, "Saved programmatically - close program before editing");
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void setValue(@NotNull String key, @Nullable String value) {
		String truncated = StringUtils.abbreviate(value, 50);
		log.info("Setting changed: {} -> {}", key, truncated);
		properties.setProperty(key, value);
		writeChangesToDisk();
	}

	@Override
	protected void deleteValue(@NotNull String key) {
		log.info("Setting deleted: {}", key);
		properties.remove(key);
		writeChangesToDisk();
	}

	@Override
	protected @Nullable String getValue(@NotNull String key) {
		return properties.getProperty(key);
	}

	@Override
	protected void clearAllValues() {
		log.info("Settings wiped");
		properties.clear();
		writeChangesToDisk();
	}
}
