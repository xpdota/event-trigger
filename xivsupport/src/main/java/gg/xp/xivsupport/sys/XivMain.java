package gg.xp.xivsupport.sys;

import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.events.BasicEventDistributor;
import gg.xp.reevent.events.BasicEventQueue;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.scan.AutoHandlerConfig;
import gg.xp.reevent.scan.AutoHandlerScan;
import gg.xp.reevent.topology.TopoInfoImpl;
import gg.xp.xivdata.data.ActionLibrary;
import gg.xp.xivdata.data.StatusEffectLibrary;
import gg.xp.xivsupport.events.state.PicoStateStore;
import gg.xp.xivsupport.events.state.XivStateImpl;
import gg.xp.xivsupport.events.ws.ActWsLogSource;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.PropertiesFilePersistenceProvider;
import gg.xp.xivsupport.persistence.UserDirPropsPersistenceProvider;
import groovy.lang.GroovyShell;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;

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
		log.info("Assembling required components");
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
		pico.addComponent(XivStateImpl.class);
		pico.addComponent(PicoBasedInstanceProvider.class);
		// Removing ability to disable topo items persistently because it was nothing but a support headache when
		// someone disabled something and forgot, or disabled something accidentally.
		pico.addComponent(new TopoInfoImpl(new InMemoryMapPersistenceProvider()));
		pico.addComponent(PrimaryLogSource.class);
		pico.addComponent(pico);
		pico.getComponent(AutoHandlerConfig.class).setAddonJars(findAddonJars());
		log.info("Required components done");
		return pico;

	}

	private static List<URL> findAddonJars() {
		return Platform.getAddonJars();
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
		pico.addComponent(BasicEventQueue.class);
		pico.addComponent(PicoStateStore.class);
		pico.addComponent(XivStateImpl.class);
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
		if (isRealLauncher()) {
			pico.addComponent(UserDirPropsPersistenceProvider.inUserDataFolder("triggevent"));
		}
		else {
			pico.addComponent(UserDirPropsPersistenceProvider.inUserDataFolder("triggevent-testing"));
		}

		new Thread(() -> {
			log.info("StartupHelper begin");
			//noinspection ResultOfObjectAllocationIgnored
			new GroovyShell();
			ActionLibrary.getAll();
			StatusEffectLibrary.getAll();
			log.info("StartupHelper end");
		}, "StartupHelper").start();

		// TODO: use "Startable" interface?
		AutoEventDistributor dist = pico.getComponent(AutoEventDistributor.class);
		log.info("Init start");
		dist.acceptEvent(new InitEvent());
		pico.getComponent(EventMaster.class).start();
		pico.getComponent(ActWsLogSource.class).start();
		log.info("Everything seems to have started successfully");
		return pico;
	}

	public static MutablePicoContainer importInit() {
		MutablePicoContainer pico = requiredComponents();
		pico.addComponent(persistenceProvider());
		pico.getComponent(AutoHandlerConfig.class).setNotLive(true);
		pico.getComponent(EventMaster.class).start();
		return pico;
	}

	public static PersistenceProvider persistenceProvider() {
		if (isRealLauncher()) {
			return UserDirPropsPersistenceProvider.inUserDataFolder("triggevent", true);
		}
		else {
			return UserDirPropsPersistenceProvider.inUserDataFolder("triggevent-testing", true);
		}
	}

	public static PersistenceProvider importPersProvider() {
		return UserDirPropsPersistenceProvider.inUserDataFolder("imports", false);
	}

}
