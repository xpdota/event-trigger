package gg.xp.sys;

import gg.xp.events.AutoEventDistributor;
import gg.xp.events.Event;
import gg.xp.events.EventDistributor;
import gg.xp.events.EventMaster;
import gg.xp.events.ws.ActWsLogSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class XivMain {

	private static final Logger log = LoggerFactory.getLogger(XivMain.class);

	private XivMain() {
	}

	public static void main(String[] args) {
		log.info("Starting main program");
		log.info("PID: {}", ProcessHandle.current().pid());

		EventDistributor<Event> eventDistributor = new AutoEventDistributor();

		EventMaster master = new EventMaster(eventDistributor);
		master.start();

		ActWsLogSource wsLogSource = new ActWsLogSource(master);
		wsLogSource.start();

		log.info("Everything seems to have started successfully");
	}


}
