package gg.xp.events;

import gg.xp.context.StateStore;
import gg.xp.scan.AutoHandler;
import gg.xp.scan.AutoHandlerScan;
import gg.xp.topology.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AutoEventDistributor extends BasicEventDistributor {
	private static final Logger log = LoggerFactory.getLogger(AutoEventDistributor.class);
	private final AutoHandlerScan scanner;
	boolean isLoaded;

	public AutoEventDistributor(StateStore state, AutoHandlerScan scanner) {
		super(state);
		this.scanner = scanner;
	}

	private void reload() {
		// TODO: kind of jank - maybe just have another list of manually-added handlers?
		List<EventHandler<Event>> handlersToKeep = handlers.stream().filter(e -> !(e instanceof AutoHandler)).collect(Collectors.toList());
		handlers.clear();
		handlers.addAll(handlersToKeep);
		handlers.addAll(scanner.build());
		isLoaded = true;
	}

	public Topology getTopology() {
		return Topology.fromHandlers(new ArrayList<>(handlers));
	}

	// TODO: is there a better place to put this?
	@Override
	public void acceptEvent(Event event) {
		if (!isLoaded) {
			reload();
		}
		if (event instanceof TopologyReloadEvent) {
			log.warn("RELOAD REQUESTED - ERRORS LIKELY");
			reload();
		}
		super.acceptEvent(event);
	}


}
