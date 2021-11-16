package gg.xp.xivsupport.sys;

import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.events.BasicEventDistributor;
import gg.xp.reevent.events.DummyEventToForceAutoScan;
import gg.xp.reevent.events.EventMaster;
import gg.xp.xivsupport.events.state.PicoStateStore;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.ws.ActWsLogSource;
import gg.xp.reevent.scan.AutoHandlerConfig;
import gg.xp.reevent.scan.AutoHandlerScan;
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

	// Just the required stuff, doesn't start anything
	private static MutablePicoContainer requiredComponents() {
		MutablePicoContainer pico = new PicoBuilder()
				.withCaching()
				.withLifecycle()
				.withAutomatic()
				.build();
		pico.addComponent(AutoEventDistributor.class);
		pico.addComponent(AutoHandlerConfig.class);
		pico.addComponent(AutoHandlerScan.class);
		pico.addComponent(EventMaster.class);
		pico.addComponent(PicoStateStore.class);
		pico.addComponent(XivState.class);
		pico.addComponent(PicoBasedInstanceProvider.class);
		pico.addComponent(pico);
		return pico;

	}

	/**
	 * Stripped-down configuration for unit testing. Still has auto-scanning, so you
	 * can use this to make a reasonable integration test without needing to manually
	 * add everything.
	 *
	 * @return The container
	 */
	public static MutablePicoContainer testingMasterInit() {
		MutablePicoContainer pico = requiredComponents();
		pico.getComponent(AutoHandlerConfig.class).setTest(true);
		pico.getComponent(EventMaster.class).start();
		return pico;
	}

	/**
	 * Even more stripped-down configuration for unit testing. Does NOT auto scan, giving you
	 * more control over what gets registered.
	 *
	 * @return The container
	 */
	public static MutablePicoContainer testingMinimalInit() {
		MutablePicoContainer pico = new PicoBuilder()
				.withCaching()
				.withLifecycle()
				.withAutomatic()
				.build();
		pico.addComponent(BasicEventDistributor.class);
		pico.addComponent(EventMaster.class);
		pico.addComponent(PicoStateStore.class);
		pico.addComponent(XivState.class);
		pico.addComponent(PicoBasedInstanceProvider.class);
		pico.addComponent(AutoHandlerConfig.class);
		pico.getComponent(AutoHandlerConfig.class).setTest(true);
		pico.addComponent(pico);

		pico.getComponent(EventMaster.class).start();
		return pico;
	}

	/**
	 * The typical configuration for actual use
	 *
	 * @return The container
	 */
	public static MutablePicoContainer masterInit() {
		log.info("Starting main program");
		log.info("PID: {}", ProcessHandle.current().pid());


		MutablePicoContainer pico = requiredComponents();
		pico.addComponent(ActWsLogSource.class);

		// TODO: use "Startable" interface?
		pico.getComponent(AutoEventDistributor.class).acceptEvent(new DummyEventToForceAutoScan());
		pico.getComponent(EventMaster.class).start();
		pico.getComponent(ActWsLogSource.class).start();
		log.info("Everything seems to have started successfully");
		return pico;
	}


}
