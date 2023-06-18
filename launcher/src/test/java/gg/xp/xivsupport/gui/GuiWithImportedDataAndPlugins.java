package gg.xp.xivsupport.gui;

import gg.xp.reevent.events.Event;
import gg.xp.xivsupport.eventstorage.EventReader;
import gg.xp.xivsupport.gui.imprt.ListEventIterator;

public final class GuiWithImportedDataAndPlugins {

	private GuiWithImportedDataAndPlugins() {
	}

	@Deprecated
	public static void main(String[] args) {
		LaunchImportedSession.fromEvents(new ListEventIterator<Event>(EventReader.readEventsFromResource("/testsession5.oos.gz")));
	}
}
