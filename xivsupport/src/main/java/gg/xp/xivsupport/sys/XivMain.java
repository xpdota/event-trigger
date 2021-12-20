package gg.xp.xivsupport.sys;

import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.events.BasicEventDistributor;
import gg.xp.reevent.events.BasicEventQueue;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.topology.TopoInfoImpl;
import gg.xp.xivsupport.events.state.PicoStateStore;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.ws.ActWsLogSource;
import gg.xp.reevent.scan.AutoHandlerConfig;
import gg.xp.reevent.scan.AutoHandlerScan;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.PropertiesFilePersistenceProvider;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class XivMain {

	private static final Logger log = LoggerFactory.getLogger(XivMain.class);

	static {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> log.error("Uncaught exception in thread {}", t, e));
	}

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
		pico.addComponent(BasicEventQueue.class);
		pico.addComponent(PicoStateStore.class);
		pico.addComponent(XivState.class);
		pico.addComponent(PicoBasedInstanceProvider.class);
		pico.addComponent(TopoInfoImpl.class);
		pico.addComponent(pico);
		return pico;

	}

	private static boolean isRealLauncher() {
		return !"true".equals(System.getenv("TRIGGEVENT_TESTING"));
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
		pico.addComponent(InMemoryMapPersistenceProvider.class);
		pico.getComponent(AutoHandlerConfig.class).setNotLive(true);
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
		pico.addComponent(InMemoryMapPersistenceProvider.class);
		pico.getComponent(AutoHandlerConfig.class).setNotLive(true);
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
		// TODO: replace with on-disk storage when done
		if (isRealLauncher()) {
			pico.addComponent(PropertiesFilePersistenceProvider.inUserDataFolder("triggevent"));
		}
		else {
			pico.addComponent(PropertiesFilePersistenceProvider.inUserDataFolder("triggevent-testing"));
		}

		// TODO: use "Startable" interface?
		pico.getComponent(AutoEventDistributor.class).acceptEvent(new InitEvent());
		pico.getComponent(EventMaster.class).start();
		pico.getComponent(ActWsLogSource.class).start();
		log.info("Everything seems to have started successfully");
		return pico;
	}

	public static MutablePicoContainer importInit() {
		MutablePicoContainer pico = requiredComponents();
		if (isRealLauncher()) {
			pico.addComponent(PropertiesFilePersistenceProvider.inUserDataFolder("triggevent", true));
		}
		else {
			pico.addComponent(PropertiesFilePersistenceProvider.inUserDataFolder("triggevent-testing", true));
		}
		pico.getComponent(AutoHandlerConfig.class).setNotLive(true);
		pico.getComponent(EventMaster.class).start();
		return pico;
	}

}
