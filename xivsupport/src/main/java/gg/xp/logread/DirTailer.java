package gg.xp.logread;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DirTailer {

	private static final Logger log = LoggerFactory.getLogger(DirTailer.class);
//	static {
//		try {
//			// Xiv doesn't support 32 bit anymore - don't care?
////			NativeUtils.loadLibraryFromJar("/jnotify.dll");
//			NativeUtils.loadLibraryFromJar("/jnotify_64bit.dll");
//			log.info("Loaded library");
//		}
//		catch (IOException e) {
//			throw new RuntimeException("Error loading libraries", e);
//		}
//	}


	private final File dirToTail;
	private int wid;
	private final Map<String, LogTailer> fileTailers = new ConcurrentHashMap<>();
	private final Consumer<String> lineConsumer;

	public DirTailer(File dirToTail, Consumer<String> lineConsumer) {
		this.lineConsumer = lineConsumer;
		if (!dirToTail.exists()) {
			throw new IllegalArgumentException("Dir does not exist: " + dirToTail);
		}
		if (!dirToTail.isDirectory()) {
			throw new IllegalArgumentException("Not a directory: " + dirToTail);
		}
		this.dirToTail = dirToTail;
//		ClassLoader oldCl = Thread.currentThread().getContextClassLoader();

	}

	public void start() {
		try {
			wid = JNotify.addWatch(dirToTail.getPath(), JNotify.FILE_ANY, true, new Listener());
		}
		catch (JNotifyException e) {
			throw new RuntimeException("Error setting up FS notifier", e);
		}
		//noinspection ConstantConditions - already confirmed to be a directory, unusual race conditions aside
		Arrays.stream(dirToTail.listFiles())
				.max(Comparator.comparing(File::lastModified))
				.map(File::getName)
				.ifPresent(this::addOrUpdateNormalTailer);
	}

	public void destroy() {
		try {
			JNotify.removeWatch(wid);
		}
		catch (JNotifyException e) {
			throw new RuntimeException(e);
		}
	}

	private void addOrUpdateNormalTailer(String name) {
		fileTailers.compute(name, (fname, tailer) -> {
			if (tailer == null) {
				// Haven't tailed this file yet - create tailer
				log.info("Starting tail on log file: {}", name);
				LogTailer logTailer = new LogTailer(new File(dirToTail, fname), lineConsumer);
				logTailer.startCurrentPos();
				return logTailer;
			}
			else {
				// Already tailing this file - wake up tailer
				tailer.notifyCheck();
				return tailer;
			}
		});
	}

	private class Listener implements JNotifyListener {

		@Override
		public void fileCreated(int wd, String rootPath, String name) {
			log.info("Created {} {} {}", wd, rootPath, name);
			// This is slightly different than 'modified' - as we want to see the whole file, not just tail
			if (name.toLowerCase(Locale.ROOT).endsWith(".log")) {
				fileTailers.compute(name, (fname, tailer) -> {
					LogTailer logTailer = new LogTailer(new File(dirToTail, fname), lineConsumer);
					logTailer.startAtBeginning();
					return logTailer;
				});
			}
		}

		@Override
		public void fileDeleted(int wd, String rootPath, String name) {
			// Corner case, ignore for now
			log.info("Deleted {} {} {}", wd, rootPath, name);
		}

		@Override
		public void fileModified(int wd, String rootPath, String name) {
			// TODO: reduce log level
			log.trace("Modified {} {} {}", wd, rootPath, name);
			if (name.toLowerCase(Locale.ROOT).endsWith(".log")) {
				addOrUpdateNormalTailer(name);
			}
		}

		@Override
		public void fileRenamed(int wd, String rootPath, String oldName, String newName) {
			log.info("Renamed {} {} {} {}", wd, rootPath, oldName, newName);
			// Corner case, ignore for now
		}
	}
}
