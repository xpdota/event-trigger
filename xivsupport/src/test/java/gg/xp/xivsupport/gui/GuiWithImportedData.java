package gg.xp.xivsupport.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventMaster;
import gg.xp.xivsupport.eventstorage.EventReader;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;

import javax.swing.*;
import java.util.List;

public class GuiWithImportedData {
	public static void main(String[] args) throws InterruptedException {
		try {
			UIManager.setLookAndFeel(new FlatDarculaLaf());
		}
		catch (Throwable t) {
			throw new RuntimeException(t);
		}
		MutablePicoContainer pico = XivMain.testingMasterInit();
		EventMaster master = pico.getComponent(EventMaster.class);
		pico.addComponent(GuiMain.class);
		AutoEventDistributor dist = pico.getComponent(AutoEventDistributor.class);
		dist.acceptEvent(new InitEvent());
		pico.getComponent(GuiMain.class);

		List<Event> events = EventReader.readEventsFromResource("/testsession2.oos");
		for (Event event : events) {
			Thread.sleep(100);
//			master.pushEvent(event);
			dist.acceptEvent(event);
		}


	}
}
