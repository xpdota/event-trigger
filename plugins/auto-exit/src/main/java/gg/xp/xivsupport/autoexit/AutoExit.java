package gg.xp.xivsupport.autoexit;

import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.scan.AutoHandlerConfig;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.util.HasFriendlyName;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.IntSetting;
import gg.xp.xivsupport.persistence.settings.StringSetting;
import gg.xp.xivsupport.sys.Threading;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * Plugin to let triggevent automatically exit when ffxiv, ACT, or any other arbitrary process exits
 */
@ScanMe
public class AutoExit {

	private static final Logger log = LoggerFactory.getLogger(AutoExit.class);

	private static final long CHECK_INTERVAL_MS = 5_000;
	private Thread thread;

	private final BooleanSetting autoExitEnabled;
	private final StringSetting processName;
	private final IntSetting exitDelay;
	private final AutoHandlerConfig config;
	private final ThreadFactory tf;
	private final Object lock = new Object();

	private @Nullable ProcessHandle detectedProcess;
	private final List<Runnable> stateListeners = new CopyOnWriteArrayList<>();
	private volatile State state = State.WATCHER_INACTIVE;

	public AutoExit(PersistenceProvider pers, AutoHandlerConfig config) {
		this.config = config;
		autoExitEnabled = new BooleanSetting(pers, "autoexit.exit-on-process-exit", false);
		processName = new StringSetting(pers, "autoexit.exit-on-process-exit-name", "ffxiv_dx11.exe");
		exitDelay = new IntSetting(pers, "autoexit.exit-on-process-exit-delay", 30);
		tf = Threading.namedDaemonThreadFactory("AutoExit");
		autoExitEnabled.addListener(this::checkNow);
		processName.addListener(this::checkNow);
		exitDelay.addListener(this::checkNow);
		processName.addListener(this::interrupt);
	}

	/**
	 * Allows for GUI to be updated
	 *
	 * @param listener Listener to attach
	 */
	public void addStateListener(Runnable listener) {
		stateListeners.add(listener);
	}

	private void setState(State state) {
		this.state = state;
		stateListeners.forEach(Runnable::run);
	}

	@HandleEvents
	public void init(InitEvent event) {
		thread = tf.newThread(this::loop);
		thread.start();
	}

	// When the user changes the process name to watch, we want to be able to reset the watcher
	private void interrupt() {
		Thread t = thread;
		if (t != null) {
			t.interrupt();
		}
	}

	private void loop() {
		setState(State.NEVER_DETECTED);
		outer:
		while (true) {
			// Loop until we find a matching process
			ProcessHandle detected;
			while (true) {
				detected = findMatching();
				if (detected != null) {
					break;
				}
				synchronized (lock) {
					try {
						lock.wait(CHECK_INTERVAL_MS);
					}
					catch (InterruptedException ignored) {
						// Even if the process name changes, that's fine, since we re-check at the top of this inner loop
					}
				}
			}
			detectedProcess = detected;
			setState(State.ALIVE);
			log.info("Detected matching process: {}", detected);
			// Wait for exit
			try {
				detected.onExit().get();
			}
			catch (InterruptedException e) {
				// We hit this branch if the user changes the setting - that's what we want, since they may have changed
				// the process name setting.
				log.info("Interrupted waiting for process");
				continue;
			}
			catch (ExecutionException e) {
				log.error("Error waiting for process exit", e);
				continue;
			}
			setState(State.DEAD);
			log.info("Process exited: {}", detected);
			long exitTimerStart = System.currentTimeMillis();
			while (true) {
				long now = System.currentTimeMillis();
				long exitDelayMs = exitDelay.get() * 1000L;
				// Still have time
				if (now < exitTimerStart + exitDelayMs) {
					if (findMatching() != null) {
						continue outer;
					}
					synchronized (lock) {
						try {
							lock.wait(1000);
						}
						catch (InterruptedException ignored) {
							continue outer;
						}
					}
				}
				// Out of time
				else {
					if (autoExitEnabled.get()) {
						if (config.isNotLive()) {
							log.info("Would have exited, but not live");
						}
						else {
							log.info("Exit");
							System.exit(0);
						}
					}
					else {
						log.info("Would have exited, but auto exit is disabled");
					}
					continue outer;
				}
			}
		}
	}

	/**
	 * Find an alive process matching the user's criteria
	 *
	 * @return The ProcessHandle
	 */
	private @Nullable ProcessHandle findMatching() {
		return ProcessHandle.allProcesses().filter(p -> {
			try {
				Optional<String> cmd = p.info().command();
				if (cmd.isEmpty()) {
					return false;
				}
				String cmdFull = cmd.get();
				String filename = Path.of(cmdFull).getFileName().toString();
				return filename.equalsIgnoreCase(processName.get()) && p.isAlive();
			}
			catch (Throwable e) {
				return false;
			}
		}).findFirst().orElse(null);
	}

	/**
	 * Wake up the loop if it is currently sleeping
	 */
	@SuppressWarnings("NakedNotify")
	private void checkNow() {
		log.info("checkNow");
		synchronized (lock) {
			lock.notifyAll();
		}
	}

	public BooleanSetting getAutoExitEnabled() {
		return autoExitEnabled;
	}

	public StringSetting getProcessName() {
		return processName;
	}

	public IntSetting getExitDelay() {
		return exitDelay;
	}

	public @Nullable ProcessHandle getDetectedProcess() {
		return detectedProcess;
	}

	public State getState() {
		return state;
	}

	public enum State implements HasFriendlyName {
		WATCHER_INACTIVE("Watcher Not Started"),
		NEVER_DETECTED("Process Not Detected"),
		ALIVE("Process Alive"),
		DEAD("Process Exited");

		private final String friendlyName;

		State(String friendlyName) {
			this.friendlyName = friendlyName;
		}

		@Override
		public String getFriendlyName() {
			return friendlyName;
		}
	}
}
