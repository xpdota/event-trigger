package gg.xp.xivsupport.events.misc;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.LiveOnly;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.FadeOutEvent;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.persistence.Compressible;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.replay.ReplayController;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import gg.xp.xivsupport.sys.Threading;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
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
	private final BucketList<Event> events = new BucketList<>(1 << 13);
	private final BooleanSetting saveToDisk;
	private final BlockingQueue<Event> eventSaveQueue = new LinkedBlockingQueue<>();
	private boolean allowSave = true;

	public RawEventStorage(PicoContainer container, PersistenceProvider persist, PrimaryLogSource pls) {
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
		allowSave = !pls.getLogSource().isImport();
		Threading.namedDaemonThreadFactory("EventCompressSave").newThread(this::eventProcessingLoop).start();
	}

	@HandleEvents(order = Integer.MIN_VALUE)
	public void storeEvent(EventContext context, Event event) {
		events.add(event);
		eventSubTypeCache.forEach((type, list) -> {
			if (type.isInstance(event)) {
				list.add(event);
			}
		});
		// TODO: make it so this can't be zero
		int maxEvents = getMaxEvents();
		if (events.size() > maxEvents) {
			doPrune(-1);
		}
	}

	private int getMaxEvents() {
		int maxEvents = maxEventsStored.get();
		if (maxEvents == 0) {
			return 50_000;
		}
		return maxEvents;
	}

	private void doPrune(int eventsToKeep) {
		if (events.bucketCount() <= 2) {
			return;
		}
		if (eventsToKeep > 0 && events.size() < eventsToKeep) {
			return;
		}
		log.info("Pruning events");
		synchronized (eventsPruneLock) {
			if (eventsToKeep == -1) {
				events.prune();
			}
			else {
				boolean pruneOk = true;
				while (events.size() > eventsToKeep && pruneOk) {
					pruneOk = events.prune();
				}
			}
		}
		eventSubTypeCache.clear();
		if (eventsToKeep >= 0) {
			exs.submit(System::gc);
		}
	}

	@HandleEvents
	public void pruneOnWipe(EventContext context, FadeOutEvent fadeOut) {
		doPrune((int) (getMaxEvents() * 0.65));

	}

	@HandleEvents(order = Integer.MAX_VALUE)
	public void queueEventForProcessing(EventContext context, Event event) {
		// This is slow. Potential fix is to save to a temp list, then dump them all once we hit a certain threshold.
		eventSaveQueue.add(event);
	}

//	@HandleEvents(order = 1_000_000)
//	public void compressEvents(EventContext context, Event event) {
//		exs.submit(() -> {
//			compressEvent(event);
//		});
//	}
//
//	@LiveOnly
//	@HandleEvents(order = Integer.MAX_VALUE)
//	public void writeEventToDisk(EventContext context, Event event) {
//		exs.submit(() -> {
//			this.saveEventToDisk(event);
//		});
//	}

	@HandleEvents
	public void clear(EventContext context, DebugCommand event) {
		if ("clear".equals(event.getCommand())) {
			clearAndGc();
		}
	}

	public void clear() {
		events.clear();
		eventSubTypeCache.clear();
	}

	public void clearAndGc() {
		clear();
		exs.submit(System::gc);
	}

	public List<Event> getEvents() {
		// Trying new thing - this implementation is safe because we only append to the list
		return new ProxyForAppendOnlyList<>(events.appendOnlyCopy());
	}

	private final Map<Class<?>, List<Event>> eventSubTypeCache = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	public <X> List<X> getEventsOfType(Class<X> eventClass) {
		// TODO: concurrency issues may cause a few events to be missed when creating one of these
		return (List<X>) new ProxyForAppendOnlyList<>(eventSubTypeCache.computeIfAbsent(eventClass,
				(cls) -> (List<Event>) getEvents().stream().filter(eventClass::isInstance)
				.map(eventClass::cast)
				.collect(Collectors.toList())));

	}

	public IntSetting getMaxEventsStoredSetting() {
		return maxEventsStored;
	}

	private void eventProcessingLoop() {
		while (true) {
			try {
				processNextEvent();
			}
			catch (Throwable t) {
				log.error("Error processing event", t);
			}
		}
	}

	private void processNextEvent() {
		Event e = null;
		try {
			e = eventSaveQueue.take();
		}
		catch (InterruptedException ex) {
			log.error("Interrupted", ex);
		}
		processEvent(e);
	}

	private void processEvent(Event e) {
		compressEvent(e);
		saveEventToDisk(e);
	}

	private static void compressEvent(Event event) {
		if (event instanceof Compressible compressible) {
			compressible.compress();
		}
	}

	private void saveEventToDisk(Event event) {
		if (event.shouldSave() && allowSave && saveToDisk.get()) {
			try {
				if (eventSaveStream == null) {
					Path sessionsDir = Platform.getSessionsDir().resolve(dirName);
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
