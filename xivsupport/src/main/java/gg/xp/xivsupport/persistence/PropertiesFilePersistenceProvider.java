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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PropertiesFilePersistenceProvider extends BaseStringPersistenceProvider {

	private static final Logger log = LoggerFactory.getLogger(PropertiesFilePersistenceProvider.class);
	private static final ExecutorService exs = Executors.newSingleThreadExecutor();
	private final Properties properties;
	private final File file;
	private final File backupFile;
	private final boolean readOnly;

	// TODO: is this threadsafe?
	public static PropertiesFilePersistenceProvider inUserDataFolder(String baseName) {
		return inUserDataFolder(baseName, false);
	}
	public static PropertiesFilePersistenceProvider inUserDataFolder(String baseName, boolean readOnly) {
		String userDataDir = System.getenv("APPDATA");
		log.info("Appdata: {}", userDataDir);
		File file = Paths.get(userDataDir, "triggevent", baseName + ".properties").toFile();
		log.info("Using file: {}", file);
		return new PropertiesFilePersistenceProvider(file, readOnly);
	}

	public PropertiesFilePersistenceProvider(File file) {
		this(file, false);
	}

	public PropertiesFilePersistenceProvider(File file, boolean readOnly) {
		this.file = file;
		this.backupFile = Paths.get(file.getParentFile().toPath().toString(), file.getName() + ".backup").toFile();
		this.readOnly = readOnly;
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
		writeChangesToDisk();
	}

	private Future<?> writeChangesToDisk() {
		if (readOnly) {
			return CompletableFuture.completedFuture(null);
		}
		return exs.submit(() -> {
			try {
				File parentFile = file.getParentFile();
				if (parentFile != null) {
					parentFile.mkdirs();
				}
				try (FileOutputStream stream = new FileOutputStream(file)) {
					properties.store(stream, "Saved programmatically - close program before editing");
				}
				if (!backupFile.exists()) {
					if (!backupFile.createNewFile()) {
						throw new RuntimeException("Could not create backup file");
					}
				}
				try (FileOutputStream stream = new FileOutputStream(backupFile)) {
					properties.store(stream, "Saved programmatically - backup file");
				}
			}
			catch (IOException e) {
				log.error("Error saving properties! Changes may not be saved!", e);
			}
		});
	}

	public void flush() {
		Future<?> future = writeChangesToDisk();
		try {
			future.get(5, TimeUnit.SECONDS);
		}
		catch (InterruptedException | ExecutionException | TimeoutException e) {
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
