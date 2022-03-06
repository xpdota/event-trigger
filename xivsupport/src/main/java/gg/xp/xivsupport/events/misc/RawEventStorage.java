package gg.xp.xivsupport.events.misc;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.LiveOnly;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.persistence.Compressible;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.replay.ReplayController;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.picocontainer.PicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

public class RawEventStorage {


	private static final Logger log = LoggerFactory.getLogger(RawEventStorage.class);
	private static final ExecutorService exs = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder()
			.daemon(false)
			.namingPattern("RawEventStorageThread-%d")
			.uncaughtExceptionHandler((t, e) -> log.error("Uncaught Error", e))
			.priority(Thread.MIN_PRIORITY)
			.build());

	private final IntSetting maxEventsStored;
	private final String dirName;
	private final String sessionName;
	// TODO: I have confirmed that this does result in a (very slow) memory leak, because the OOS just keeps handles on
	// everything because the output stream needs to keep track of references. But since realistically we'd just be
	// writing websocket stuff to it, we can probably just save one big string.
	private ObjectOutputStream eventSaveStream;
	// TODO: cap this or otherwise manage memory
	private final Object eventsPruneLock = new Object();
	private List<Event> events = new ArrayList<>();
	private final BooleanSetting saveToDisk;
	private boolean allowSave = true;

	public RawEventStorage(PicoContainer container, PersistenceProvider persist) {
		boolean isReplay = container.getComponent(ReplayController.class) != null;
		IntSetting maxEventsStoredLegacy = new IntSetting(persist, "raw-storage.events-to-retain", 1_000_000);
		int defaultMaxEvents;
		if (isReplay) {
			defaultMaxEvents = 1_000_000;
		}
		else if (maxEventsStoredLegacy.get() != 1_000_000) {
			defaultMaxEvents = maxEventsStoredLegacy.get();
		}
		else {
			defaultMaxEvents = 100_000;
		}
		maxEventsStored = new IntSetting(persist, "raw-storage.events-to-retain-" + (isReplay ? "replay" : "live"), defaultMaxEvents);
		saveToDisk = new BooleanSetting(persist, "raw-storage.save-to-disk", false);
		dirName = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		sessionName = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (eventSaveStream != null) {
				try {
					eventSaveStream.flush();
					eventSaveStream.close();
				}
				catch (IOException e) {
					log.error("Error", e);
				}
			}
		}));
	}

	@HandleEvents(order = Integer.MIN_VALUE)
	public void storeEvent(EventContext context, Event event) {
		events.add(event);
		int maxEvents = maxEventsStored.get();
		if (events.size() > maxEvents) {
			log.info("Pruning events");
			synchronized (eventsPruneLock) {
				int demarcation = events.size() / 3;
				events = new ArrayList<>(events.subList(demarcation, events.size()));
			}
			// TODO: shutdown hook, or just stream events directly
			exs.submit(System::gc);
		}
	}

	@HandleEvents
	public void compressEvents(EventContext context, Event event) {
		exs.submit(() -> {
			compressEvent(event);
		});
	}

	@LiveOnly
	@HandleEvents(order = Integer.MAX_VALUE)
	public void writeEventToDisk(EventContext context, Event event) {
		exs.submit(() -> {
			this.saveEventToDisk(event);
		});
	}

	@HandleEvents
	public void clear(EventContext context, DebugCommand event) {
		if ("clear".equals(event.getCommand())) {
			events = new ArrayList<>();
			exs.submit(System::gc);
		}
	}

	public List<Event> getEvents() {
		// Trying new thing - this implementation is safe because we only append to the list
		return new ProxyForAppendOnlyList<>(events);
	}

	public IntSetting getMaxEventsStoredSetting() {
		return maxEventsStored;
	}

	private static void compressEvent(Event event) {
		if (event instanceof Compressible) {
			((Compressible) event).compress();
		}
	}

	private void saveEventToDisk(Event event) {
		if (event.shouldSave() && allowSave && saveToDisk.get()) {
			try {
				if (eventSaveStream == null) {
					String userDataDir = System.getenv("APPDATA");
					Path sessionsDir = Paths.get(userDataDir, "triggevent", "sessions", dirName);
					File sessionsDirFile = sessionsDir.toFile();
					sessionsDirFile.mkdirs();
					if (!sessionsDirFile.exists() && sessionsDirFile.isDirectory()) {
						log.error("Error saving to disk! Could not make dirs: {}", sessionsDirFile.getAbsolutePath());
						return;
					}
					File file = Paths.get(sessionsDir.toString(), sessionName + ".session.oos.gz").toFile();
					FileOutputStream fileOutputStream = new FileOutputStream(file, true);
					GZIPOutputStream compressedOutputStream = new GZIPOutputStream(fileOutputStream);
					eventSaveStream = new ObjectOutputStream(compressedOutputStream);
				}
				eventSaveStream.writeObject(event);
			}
			catch (IOException e) {
				log.error("Error saving to disk!", e);
			}
		}
	}

	public void flushToDisk() {
		if (eventSaveStream != null) {
			try {
				eventSaveStream.flush();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public BooleanSetting getSaveToDisk() {
		return saveToDisk;
	}

	public void setAllowSave(boolean allowSave) {
		this.allowSave = allowSave;
	}
}
