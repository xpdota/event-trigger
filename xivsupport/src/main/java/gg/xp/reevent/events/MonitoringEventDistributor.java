package gg.xp.reevent.events;

import gg.xp.compmonitor.CompMonitor;
import gg.xp.reevent.context.StateStore;
import gg.xp.reevent.scan.AutoHandler;
import gg.xp.reevent.scan.AutoHandlerConfig;
import gg.xp.reevent.scan.AutoScan;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.topology.Topology;
import gg.xp.reevent.topology.TopologyInfo;
import gg.xp.reevent.topology.TopologyProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonitoringEventDistributor extends BasicEventDistributor implements TopologyProvider {
	private static final Logger log = LoggerFactory.getLogger(MonitoringEventDistributor.class);
	private final AutoScan scanner;
	private final TopologyInfo topoInfo;
	private final Object loadLock = new Object();
	private final Map<Class<? extends Event>, List<EventHandler<Event>>> eventClassMap = new HashMap<>();
	private final List<@NotNull EventHandler<Event>> autoHandlers = new ArrayList<>();
	private final List<@NotNull EventHandler<Event>> manualHandlers = new ArrayList<>();
	private volatile boolean dirty;
	private Topology topology;

	public MonitoringEventDistributor(StateStore state, AutoScan scanner, TopologyInfo topoInfo, CompMonitor mon, AutoHandlerConfig config) {
		super(state);
		mon.addAndRunListener(item -> {
			boolean dirty = false;
			Object inst = item.instance();
			if (inst instanceof EventHandler<?> eh) {
//				synchronized (loadLock) {
				autoHandlers.add((EventHandler<Event>) eh);
				dirty = true;
//				}
			}
			Class<?> clazz = inst.getClass();
			Method[] methods = clazz.getMethods();
			for (Method method : methods) {
				if (method.isAnnotationPresent(HandleEvents.class)) {
					AutoHandler rawEvh = new AutoHandler(clazz, method, inst, config);
//					synchronized (loadLock) {
					autoHandlers.add(rawEvh);
					dirty = true;
//					}
				}
			}
			if (dirty) {
				this.dirty = true;
			}
		});
		this.scanner = scanner;
		this.topoInfo = topoInfo;
		topology = Topology.fromHandlers(Collections.emptyList(), this.topoInfo);
	}

	@Override
	public synchronized void registerHandler(EventHandler<Event> handler) {
		if (handler == null) {
			throw new IllegalArgumentException("Handler was null!");
		}
		manualHandlers.add(handler);
		dirty = true;
	}

	public void reloadIfNeeded() {
		scanner.doScanIfNeeded();
		if (!dirty) {
			return;
		}
		log.info("Reloading", new RuntimeException());
//		synchronized (loadLock) {
		handlers.clear();
		handlers.addAll(manualHandlers);
		handlers.addAll(autoHandlers);
		sortHandlers();
//		}
		topology = Topology.fromHandlers(new ArrayList<>(this.handlers), topoInfo);
		dirty = false;
	}

	@Override
	protected void sortHandlers() {
		super.sortHandlers();
		eventClassMap.clear();
	}

	@Override
	protected List<EventHandler<Event>> getHandlersForEvent(Event event) {
		Class<? extends Event> eventClass = event.getClass();
		return eventClassMap.computeIfAbsent(eventClass, cls -> handlers.stream().filter(eh -> {
			if (eh instanceof TypedEventHandler<Event> teh) {
				return teh.getType().isAssignableFrom(eventClass);
			}
			else {
				return true;
			}
		}).sorted(Comparator.comparing(EventHandler::getOrder)).toList());
	}

	@Override
	public Topology getTopology() {
		return topology;
	}

	@Override
	public void acceptEvent(Event event) {
		reloadIfNeeded();
		super.acceptEvent(event);
	}


}
