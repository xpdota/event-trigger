package gg.xp.xivsupport.gui;

import gg.xp.xivsupport.eventstorage.EventReader;

public final class GuiWithImportedDataAndPlugins {

	private GuiWithImportedDataAndPlugins() {
	}

	public static void main(String[] args) {
		LaunchImportedSession.fromEvents(EventReader.readEventsFromResource("/testsession5.oos.gz"));
	}
}
