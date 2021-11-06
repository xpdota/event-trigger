package gg.xp.logread;


import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

public class LogTailer {

	private static final Logger log = LoggerFactory.getLogger(LogTailer.class);
	private static final ThreadFactory threadFactory = new BasicThreadFactory.Builder()
			.daemon(true)
			.namingPattern("LogTailer-%d")
			.build();

	private final File fileToTail;
	private final Consumer<String> lineConsumer;
	private final Object stateNotify = new Object();
	private volatile boolean keepRunning;
	private volatile boolean startAtBeginning;
	private Thread readerThread;

	public LogTailer(File fileToTail, Consumer<String> lineConsumer) {
		if (!fileToTail.exists()) {
			throw new IllegalArgumentException("File does not exist: " + fileToTail);
		}
		if (!fileToTail.isFile()) {
			throw new IllegalArgumentException("Not a file: " + fileToTail);
		}
		if (!fileToTail.canRead()) {

			throw new IllegalArgumentException("Cannot read file: " + fileToTail);
		}
		this.fileToTail = fileToTail;
		this.lineConsumer = lineConsumer;
	}

	public void startAtBeginning() {
		synchronized (stateNotify) {
			if (readerThread != null) {
				throw new IllegalStateException("Already running");
			}
			startAtBeginning = true;
			initThread();
		}
	}

	public void startCurrentPos() {
		synchronized (stateNotify) {
			if (readerThread != null) {
				throw new IllegalStateException("Already running");
			}
			initThread();
		}
	}

	@SuppressWarnings("WaitNotifyWhileNotSynced")
	private void initThread() {
		keepRunning = true;
		readerThread = threadFactory.newThread(this::readerThreadLoop);
		readerThread.start();
		stateNotify.notifyAll();
	}

	public void stop() {
		synchronized (stateNotify) {
			keepRunning = false;
			stateNotify.notifyAll();
			readerThread = null;
		}
	}

	// Roughly based on https://crunchify.com/log-file-tailer-tail-f-implementation-in-java-best-way-to-tail-any-file-programmatically/
	private void readerThreadLoop() {
		long lastPosition = startAtBeginning ? 0 : fileToTail.length();
		boolean logWhenCaughtUp = startAtBeginning;
		while (keepRunning) {
			try {
				long newSize = fileToTail.length();
				log.trace("Sizes: {}->{}", lastPosition, newSize);
				if (newSize > lastPosition) {
					try (RandomAccessFile raf = new RandomAccessFile(fileToTail, "r")) {
						raf.seek(lastPosition);
						String line;
						while ((line = raf.readLine()) != null) {
							lineConsumer.accept(line);
						}
						lastPosition = newSize;
						if (logWhenCaughtUp) {
							logWhenCaughtUp = false;
							log.info("Caught up: file {} now at pos {}", fileToTail.getName(), newSize);
						}
					}
					catch (FileNotFoundException ignored) {
						// Shouldn't get FNF because we verified the file exists. This means it was deleted.
						log.error("File is gone: {}", fileToTail);
					}
					catch (IOException e) {
						log.error("Error reading file: {}", fileToTail, e);
					}
				}
			}
			catch (Throwable t) {
				log.error("Error running log read loop", t);
			}
			// TODO
			try {
				synchronized (stateNotify) {
					stateNotify.wait(1000);
				}
			}
			catch (InterruptedException e) {
				log.error("Interrupted", e);
			}
		}
	}

	@SuppressWarnings("NakedNotify")
	public void notifyCheck() {
		synchronized (stateNotify) {
			stateNotify.notifyAll();
		}
	}

}
