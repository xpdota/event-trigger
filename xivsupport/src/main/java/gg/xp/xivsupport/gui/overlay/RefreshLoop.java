package gg.xp.xivsupport.gui.overlay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public class RefreshLoop<X> {

	private static final Logger log = LoggerFactory.getLogger(RefreshLoop.class);
	private static final AtomicInteger threadIdCounter = new AtomicInteger();

	private final WeakReference<X> item;
	private final Object sleepLock = new Object();
	private final Thread thread;
	private volatile boolean stop;

	public RefreshLoop(String threadNameStub, X item, Consumer<X> periodicTask, Function<X, Long> interval) {
		this.item = new WeakReference<>(item);
		thread = new Thread(() -> {
			while (!stop) {
				try {
					X actualItem = this.item.get();
					if (actualItem == null) {
						log.info("Stopping refresh loop because refreshable item no longer exists");
						return;
					}
					periodicTask.accept(actualItem);
					long sleepTime = interval.apply(actualItem);
					synchronized (sleepLock) {
						sleepLock.wait(sleepTime);
					}
				}
				catch (Throwable t) {
					log.error("Error in periodic refresh", t);
				}
			}
		});
		thread.setDaemon(true);
		thread.setName(threadNameStub + '-' + threadIdCounter.getAndIncrement());

	}

	public void start() {
		thread.start();
	}

	public void stop() {
		stop = true;
	}

	public void refreshNow() {
		synchronized (sleepLock) {
			//noinspection NakedNotify
			sleepLock.notifyAll();
		}
	}

}
