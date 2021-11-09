package gg.xp.sys;

import gg.xp.events.AutoEventDistributor;
import gg.xp.events.Event;
import gg.xp.events.EventDistributor;
import gg.xp.events.EventMaster;
import gg.xp.events.state.XivState;
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
		masterInit();
	}

	public static EventMaster masterInit() {
		log.info("Starting main program");
		log.info("PID: {}", ProcessHandle.current().pid());

		EventDistributor<Event> eventDistributor = new AutoEventDistributor();

		EventMaster master = new EventMaster(eventDistributor);
		// TODO: picocontainer or something - cross-class dependencies are getting out of hand
		EventDistributor<Event> distributor = master.getDistributor();
		distributor.getStateStore().putCustom(XivState.class, new XivState(master));
		master.start();


		ActWsLogSource wsLogSource = new ActWsLogSource(master);
		wsLogSource.start();

		log.info("Everything seems to have started successfully");
		return master;
	}


}
