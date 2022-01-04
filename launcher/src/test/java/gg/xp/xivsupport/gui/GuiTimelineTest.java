package gg.xp.xivsupport.gui;

import gg.xp.xivsupport.eventstorage.EventReader;

public final class GuiTimelineTest {

	private GuiTimelineTest() {
	}

	public static void main(String[] args) {
		LaunchImportedActLog.fromEvents(EventReader.readActLogResource("/cww.log"));
	}
}

