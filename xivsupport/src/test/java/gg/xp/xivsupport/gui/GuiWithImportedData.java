package gg.xp.xivsupport.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import gg.xp.reevent.events.AutoEventDistributor;
import gg.xp.reevent.events.BasicEventQueue;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventMaster;
import gg.xp.xivsupport.events.misc.RawEventStorage;
import gg.xp.xivsupport.eventstorage.EventReader;
import gg.xp.xivsupport.sys.XivMain;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.List;

public class GuiWithImportedData {
	private static final Logger log = LoggerFactory.getLogger(GuiWithImportedData.class);
	public static void main(String[] args) throws InterruptedException {
		try {
			UIManager.setLookAndFeel(new FlatDarculaLaf());
		}
		catch (Throwable t) {
			throw new RuntimeException(t);
		}
		MutablePicoContainer pico = XivMain.testingMasterInit();
		AutoEventDistributor dist = pico.getComponent(AutoEventDistributor.class);
		dist.acceptEvent(new InitEvent());
		RawEventStorage raw = pico.getComponent(RawEventStorage.class);
		raw.getMaxEventsStoredSetting().set(1_000_000);
		BasicEventQueue queue = pico.getComponent(BasicEventQueue.class);
		pico.addComponent(GuiMain.class);
		pico.getComponent(GuiMain.class);

		long start = System.currentTimeMillis();
		List<Event> events = EventReader.readEventsFromResource("/testsession5.oos.gz");
		long read = System.currentTimeMillis();
		events.stream()
//				.limit(10000)
				.peek((e) -> doSleep(1))
				.forEach(dist::acceptEvent);
		queue.waitDrain();
		long finish = System.currentTimeMillis();
		log.info("Imported Event Count: {}", events.size());
		log.info("Processed Event Count: {}", raw.getEvents().size());
		log.info("Time to read, decompress, and deserialize: {}ms", read - start);
		log.info("Time to replay: {}ms", finish - read);


	}
	private static void doSleep(long ms) {
		try {
			Thread.sleep(ms);
		}
		catch (InterruptedException e) {

		}
	}
}
