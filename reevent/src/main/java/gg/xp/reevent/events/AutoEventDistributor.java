package gg.xp.reevent.events;

import gg.xp.reevent.context.StateStore;
import gg.xp.reevent.scan.AutoHandler;
import gg.xp.reevent.scan.AutoHandlerScan;
import gg.xp.reevent.topology.Topology;
import gg.xp.reevent.topology.TopologyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AutoEventDistributor extends BasicEventDistributor {
	private static final Logger log = LoggerFactory.getLogger(AutoEventDistributor.class);
	private final AutoHandlerScan scanner;
	private final TopologyInfo topoInfo;
	private final Object loadLock = new Object();
	private final Map<Class<? extends Event>, List<EventHandler<Event>>> eventClassMap = new HashMap<>();
	boolean isLoaded;
	private Topology topology;

	public AutoEventDistributor(StateStore state, AutoHandlerScan scanner, TopologyInfo topoInfo) {
		super(state);
		this.scanner = scanner;
		this.topoInfo = topoInfo;
		topology = Topology.fromHandlers(Collections.emptyList(), this.topoInfo);
	}

	// TODO: this just doesn't work well until event sources are also auto-ified
	// We get double events after reloading
	public void reload() {
		// TODO: kind of jank - maybe just have another list of manually-added handlers?
		List<EventHandler<Event>> handlersToKeep = handlers.stream().filter(e -> !(e instanceof AutoHandler)).toList();
		handlers.clear();
		handlers.addAll(handlersToKeep);
		List<AutoHandler> handlers = scanner.build();
		this.handlers.addAll(handlers);
		sortHandlers();
		topology = Topology.fromHandlers(new ArrayList<>(this.handlers), topoInfo);
		isLoaded = true;
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

	public Topology getTopology() {
		return topology;
	}

	// TODO: is there a better place to put this?
	@Override
	public void acceptEvent(Event event) {
		if (!isLoaded) {
			reload();
		}
		if (event instanceof TopologyReloadEvent) {
			// Disabling hot reloading for now
			// Now that log *providers* such as the WS log source expose handlers via
			// @HandleEvents, reloading also causes them to spin up their log sources
			// twice, with disastrous results.
			log.error("Reloading currently disabled");
//			log.warn("RELOAD REQUESTED - ERRORS LIKELY");
//			reload();
		}
		super.acceptEvent(event);
	}


}
