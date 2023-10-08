package gg.xp.compmonitor;

import org.picocontainer.ComponentAdapter;
import org.picocontainer.PicoContainer;
import org.picocontainer.monitors.NullComponentMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class CompMonitor extends NullComponentMonitor {
	@Serial
	private static final long serialVersionUID = -4049648364319492087L;

	private static final Logger log = LoggerFactory.getLogger(CompMonitor.class);
	private final List<InstantiatedItem<?>> all = new ArrayList<>();
	private final List<CompListener> listeners = new ArrayList<>();
	private final Object lock = new Object();
	private PicoContainer container;

	@Override
	public <T> void instantiated(PicoContainer container, ComponentAdapter<T> componentAdapter, Constructor<T> constructor, Object instantiated, Object[] injected, long duration) {
		// ONLY apply to a single container
		if (!checkContainer(container)) {
			return;
		}
		if (instantiated == null) {
			log.error("Failed to instantiate {} {} {} {}", componentAdapter, constructor, constructor.getDeclaringClass(), instantiated);
			return;
		}
		InstantiatedItem<T> inst = new InstantiatedItem<>(constructor.getDeclaringClass(), (T) instantiated);
		synchronized (lock) {
			all.add(inst);
			listeners.forEach(listener -> listener.added(inst));
		}
		if (duration >= 100) {
			log.warn("CompMonitor: {}ms to instantiate {}", duration, instantiated);
		}
	}

	private boolean checkContainer(PicoContainer container) {
		if (this.container == null) {
			this.container = container;
			return true;
		}
		else {
			return this.container == container;
		}
	}

	public void addListener(CompListener listener) {
//		synchronized (lock) {
			listeners.add(listener);
//		}
	}

	public void addAndRunListener(CompListener listener) {
//		synchronized (lock) {
			addListener(listener);
			all.forEach(listener::added);
//		}
	}
}
