package gg.xp.sys;

import gg.xp.context.StateStore;
import gg.xp.events.AutoEventDistributor;
import gg.xp.events.Event;
import gg.xp.events.EventDistributor;
import gg.xp.events.EventMaster;
import gg.xp.events.state.XivState;
import gg.xp.events.ws.ActWsLogSource;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
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

	public static MutablePicoContainer masterInit() {
		log.info("Starting main program");
		log.info("PID: {}", ProcessHandle.current().pid());

		MutablePicoContainer pico = new DefaultPicoContainer();
		pico.addComponent(AutoEventDistributor.class);
		pico.addComponent(EventMaster.class);
		pico.addComponent(StateStore.class);
		pico.addComponent(XivState.class);
		pico.addComponent(ActWsLogSource.class);
		pico.addComponent(pico);

		// TODO: picocontainer or something - cross-class dependencies are getting out of hand
		pico.getComponent(EventMaster.class).start();
		pico.getComponent(ActWsLogSource.class).start();

		log.info("Everything seems to have started successfully");
		return pico;
	}


}
