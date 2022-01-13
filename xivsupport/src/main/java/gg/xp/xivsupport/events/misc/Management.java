package gg.xp.xivsupport.events.misc;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.Platform;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@SuppressWarnings("CallToSystemGC")
public class Management {

	private static final Logger log = LoggerFactory.getLogger(Management.class);

	private final BooleanSetting gcOnNewPullEnabled;

	public Management(PersistenceProvider pers) {
		gcOnNewPullEnabled = new BooleanSetting(pers, "management.gc_on_new_pull", true);

	}

	@HandleEvents
	public void forceGc(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("gc")) {
			log.info("Explicit GC requested");
			System.gc();
		}
	}

	@HandleEvents
	public void gcOnNewPull(EventContext context, PullStartedEvent event) {
		if (gcOnNewPullEnabled.get()) {
			log.info("GC on new pull");
			System.gc();
		}
	}

	public BooleanSetting getGcOnNewPullEnabled() {
		return gcOnNewPullEnabled;
	}

	private static String makeHeapDumpFilename() {
		String template = "heapdump%s.hprof";
		return template.formatted(System.currentTimeMillis());

	}

	public String dumpHeap() {
		log.info("Heap dump requested");
		try {
			String heapDumpFileName = makeHeapDumpFilename();
			HeapDumper.dumpHeap("./" + heapDumpFileName, true);
			return "Heap dumped to " + Platform.getInstallDir() + File.separator + heapDumpFileName;
		}
		catch (Throwable t) {
			log.error("Error dumping heap", t);
			return "Error dumping heap: " + t + "\n\n" + ExceptionUtils.getStackTrace(t);
		}
	}

}
