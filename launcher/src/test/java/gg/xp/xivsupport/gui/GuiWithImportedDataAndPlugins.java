package gg.xp.xivsupport.gui;

import gg.xp.xivsupport.eventstorage.EventReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GuiWithImportedDataAndPlugins {
	private static final Logger log = LoggerFactory.getLogger(LaunchImportedSession.class);

	private GuiWithImportedDataAndPlugins() {
	}

	public static void main(String[] args) throws InterruptedException {
		LaunchImportedSession.fromEvents(EventReader.readEventsFromResource("/testsession5.oos.gz"));
	}
}
