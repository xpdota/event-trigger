package gg.xp.xivsupport.events.triggers.marks.adv;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpecificAutoMarkHandler {

	private static final Logger log = LoggerFactory.getLogger(SpecificAutoMarkHandler.class);
	private static final ExecutorService exs = Executors.newSingleThreadExecutor();
	private final XivState state;

	public SpecificAutoMarkHandler(PersistenceProvider persistence, XivState state) {
		this.state = state;
	}

	@HandleEvents
	public void amTest(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("samtest")) {

			List<String> args = event.getArgs();
			if (args.size() != 3) {
				log.error("Wrong number of arguments. Syntax: samtest MARKER_TYPE PARTY_SLOT");
				return;
			}

			int slot = Integer.parseInt(args.get(2));
			MarkerSign marker = MarkerSign.of(args.get(1));
			context.accept(new SpecificAutoMarkSlotRequest(slot, marker));
		}
	}

	@HandleEvents
	public void findPartySlot(EventContext context, SpecificAutoMarkRequest event) {
		XivPlayerCharacter player = event.getPlayerToMark();
		int index = state.getPartySlotOf(player);
		if (index >= 0) {
			int partySlot = index + 1;
			log.info("Resolved player {} to party slot {}", player.getName(), partySlot);
			context.accept(new SpecificAutoMarkSlotRequest(partySlot, event.getMarker()));
		}
		else {
			log.error("Couldn't resolve player '{}' to party slot", player.getName());
		}
	}
}
