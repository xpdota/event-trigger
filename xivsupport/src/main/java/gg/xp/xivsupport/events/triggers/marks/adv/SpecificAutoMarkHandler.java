package gg.xp.xivsupport.events.triggers.marks.adv;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpecificAutoMarkHandler {

	private static final Logger log = LoggerFactory.getLogger(SpecificAutoMarkHandler.class);
	private static final ExecutorService exs = Executors.newSingleThreadExecutor();
	private final XivState state;

	public SpecificAutoMarkHandler(XivState state) {
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
			XivPlayerCharacter playerToMark;
			try {
				playerToMark = state.getPartyList().get(slot - 1);
			}
			catch (IndexOutOfBoundsException e) {
				log.error("Tried to mark slot {} but there were only {} party members.", slot, state.getPartyList().size());
				return;
			}
			MarkerSign marker = MarkerSign.of(args.get(1));
			context.accept(new SpecificAutoMarkRequest(playerToMark, marker));
		}
	}

	@HandleEvents
	public void findPartySlot(EventContext context, SpecificAutoMarkRequest event) {
		XivPlayerCharacter player = event.getPlayerToMark();
		int index = state.getPartySlotOf(player);
		if (index >= 0) {
			int partySlot = index + 1;
			log.info("Resolved player {} to party slot {} for marker {}", player.getName(), partySlot, event.getMarker());
			context.accept(new SpecificAutoMarkSlotRequest(partySlot, event.getMarker()));
		}
		else {
			log.error("Couldn't resolve player '{}' to party slot for marker {}", player.getName(), event.getMarker());
		}
	}
}
