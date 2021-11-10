package gg.xp.sys;

import gg.xp.events.AutoEventDistributor;
import gg.xp.events.EventMaster;
import gg.xp.events.state.PicoStateStore;
import gg.xp.events.state.XivState;
import gg.xp.events.ws.ActWsLogSource;
import gg.xp.scan.AutoHandlerScan;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class XivMain {

	private static final Logger log = LoggerFactory.getLogger(XivMain.class);

	private XivMain() {
	}

	public static void main(String[] args) {
		masterInit();
	}

	private static MutablePicoContainer requiredComponents() {
		MutablePicoContainer pico = new PicoBuilder()
				.withCaching()
				.withLifecycle()
				.withAutomatic()
				.build();
		pico.addComponent(AutoEventDistributor.class);
		pico.addComponent(AutoHandlerScan.class);
		pico.addComponent(EventMaster.class);
		pico.addComponent(PicoStateStore.class);
		pico.addComponent(XivState.class);
		pico.addComponent(PicoBasedInstanceProvider.class);
		pico.addComponent(pico);
		return pico;

	}

	public static MutablePicoContainer masterNoSource() {

		MutablePicoContainer pico = requiredComponents();
		// TODO: picocontainer or something - cross-class dependencies are getting out of hand
		pico.getComponent(EventMaster.class).start();
		return pico;
	}

	public static MutablePicoContainer masterInit() {
		log.info("Starting main program");
		log.info("PID: {}", ProcessHandle.current().pid());


		MutablePicoContainer pico = requiredComponents();
		pico.addComponent(ActWsLogSource.class);

		// TODO: use "Startable" interface?
		pico.getComponent(EventMaster.class).start();
		pico.getComponent(ActWsLogSource.class).start();

		log.info("Everything seems to have started successfully");
		return pico;
	}


}
