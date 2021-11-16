package gg.xp.xivsupport.events.jails;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.models.XivPlayerCharacter;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.reevent.scan.DisableInTest;
import gg.xp.reevent.scan.HandleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoMarkHandler {

	private static final Logger log = LoggerFactory.getLogger(AutoMarkHandler.class);
	private static final ExecutorService exs = Executors.newSingleThreadExecutor();

	@HandleEvents
	public static void amTest(EventContext<Event> context, DebugCommand event) {
		if (event.getCommand().equals("amtest")) {
			List<String> args = event.getArgs();
			args.subList(1, args.size())
					.stream()
					.mapToInt(Integer::parseInt)
					.forEach(AutoMarkHandler::doAutoMarkForSlot);
		}
	}

	@HandleEvents
	@DisableInTest
	public static void clearMarks(EventContext<Event> context, ClearAutoMarkRequest event) {
		log.info("Clearing marks");
		clearAutoMark();
	}

	@HandleEvents
	@DisableInTest
	public static void doAutoMark(EventContext<Event> context, AutoMarkRequest event) {
		XivState xivState = context.getStateInfo().get(XivState.class);
		List<XivPlayerCharacter> partyList = xivState.getPartyList();
		XivPlayerCharacter player = event.getPlayerToMark();
		int index = partyList.indexOf(player);
		int partySlot = index + 1;
		log.info("Resolved player {} to party slot {}", player.getName(), partySlot);
		event.setResolvedPartySlot(partySlot);
		doAutoMarkForSlot(partySlot);
	}

	private static void clearAutoMark() {
		exs.submit(() -> {
			try {
				new Robot().keyPress(KeyEvent.VK_NUMPAD1);
				Thread.sleep(50);
				new Robot().keyRelease(KeyEvent.VK_NUMPAD9);
				Thread.sleep(50);
			}
			catch (AWTException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static void doAutoMarkForSlot(int i) {
		exs.submit(() -> {
			try {
				new Robot().keyPress(KeyEvent.VK_NUMPAD1 - 1 + i);
				Thread.sleep(50);
				new Robot().keyRelease(KeyEvent.VK_NUMPAD1 - 1 + i);
				Thread.sleep(50);
			}
			catch (AWTException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
	}


}
