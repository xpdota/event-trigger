package gg.xp.xivsupport.gui;

import gg.xp.xivsupport.eventstorage.EventReader;

public final class GuiWithImportedActLogAndPlugins {

	private GuiWithImportedActLogAndPlugins() {
	}

	public static void main(String[] args) {
		LaunchImportedActLog.fromEvents(EventReader.readActLogResource("/uwu.log"));
	}
}

