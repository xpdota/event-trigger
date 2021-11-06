package gg.xp.sys;

import gg.xp.events.ACTLogLineEvent;
import gg.xp.events.Event;
import gg.xp.events.EventDistributor;
import gg.xp.events.EventMaster;
import gg.xp.logread.DirTailer;
import gg.xp.scan.AutoHandlerScan;
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
		Path logDir = Paths.get(System.getenv("APPDATA"), "Advanced Combat Tracker", "FFXIVLogs");
		log.info("Log dir guess: {}", logDir);
		if (!logDir.toFile().exists()) {
			throw new RuntimeException("Log directory does not exist: " + logDir);
		}
		if (!logDir.toFile().exists()) {
			throw new RuntimeException("Log directory does not exist: " + logDir);
		}


		EventDistributor<Event> eventDistributor = AutoHandlerScan.create();

		EventMaster master = new EventMaster(eventDistributor);

		DirTailer tailer = new DirTailer(logDir.toFile(), line -> master.pushEvent(new ACTLogLineEvent(line)));
		tailer.start();

		master.start();

		log.info("Everything seems to have started successfully");


//		while (true) {
//			Thread.sleep(10000);
//		}

	}

}
