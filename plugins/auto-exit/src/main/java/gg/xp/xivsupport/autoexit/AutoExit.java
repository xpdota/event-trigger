package gg.xp.xivsupport.autoexit;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@ScanMe
public class AutoExit {

	private static final Logger log = LoggerFactory.getLogger(AutoExit.class);

	private final BooleanSetting exitOnParentExit;

	public AutoExit(PersistenceProvider pers) {
		exitOnParentExit = new BooleanSetting(pers, "autoexit.exit-on-parent-exit", true);
	}

	private @Nullable ProcessHandle findRealParent() {
		ProcessHandle thisProcess = ProcessHandle.current();
		ProcessHandle possibleParent;
		while (true) {
			Optional<ProcessHandle> parentMaybe = thisProcess.parent();
			if (parentMaybe.isEmpty()) {
				return null;
			}
			ProcessHandle.Info parentInfo = parentMaybe.get().info();
//			if (parentInfo.)

		}
	}

	static {
		ProcessHandle thisProcess = ProcessHandle.current();
		thisProcess.parent().ifPresentOrElse(p -> {
			log.info("Parent process ID: {} name {}", p.pid(), p.info());
		}, () -> {
			log.info("No parent process found");
		});
	}

}
