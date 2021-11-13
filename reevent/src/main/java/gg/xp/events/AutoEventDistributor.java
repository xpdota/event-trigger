package gg.xp.events;

import gg.xp.context.StateStore;
import gg.xp.scan.AutoHandlerScan;
import gg.xp.topology.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class AutoEventDistributor extends BasicEventDistributor {
	private static final Logger log = LoggerFactory.getLogger(AutoEventDistributor.class);
	private final AutoHandlerScan scanner;

	public AutoEventDistributor(StateStore state, AutoHandlerScan scanner) {
		super(state);
		this.scanner = scanner;
		handlers.addAll(scanner.build());
	}

	private void reload() {
		handlers.clear();
		handlers.addAll(scanner.build());
	}

	public Topology getTopology() {
		return Topology.fromHandlers(new ArrayList<>(handlers));
	}

	// TODO: is there a better place to put this?
	@Override
	public void acceptEvent(Event event) {
		if (event instanceof TopologyReloadEvent) {
			log.warn("RELOAD REQUESTED - ERRORS LIKELY");
			reload();
		}
		super.acceptEvent(event);
	}


}
