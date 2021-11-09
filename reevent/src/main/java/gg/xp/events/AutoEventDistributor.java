package gg.xp.events;

import gg.xp.scan.AutoHandler;
import gg.xp.scan.AutoHandlerScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoEventDistributor extends BasicEventDistributor {
	private static final Logger log = LoggerFactory.getLogger(AutoEventDistributor.class);

	public AutoEventDistributor() {
		handlers.addAll(AutoHandlerScan.listAll());
	}

	private void reload() {
		handlers.clear();
		AutoHandlerScan.listAll().forEach(this::registerHandler);
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
