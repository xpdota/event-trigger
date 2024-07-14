package gg.xp.xivsupport.events.ws;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.state.PartyChangeEvent;
import gg.xp.xivsupport.events.state.XivState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Extra logic for getting sorted party list from ACT
 */
public class ActWsPartyExtra {

	private static final String SORTED_PARTY_RSEQ = "SortedParty";
	private static final Logger log = LoggerFactory.getLogger(ActWsPartyExtra.class);
	private final ActWsLogSource ws;
	private final XivState state;

	public ActWsPartyExtra(ActWsLogSource ws, XivState state) {
		this.ws = ws;
		this.state = state;
	}

	@HandleEvents(order = -100)
	public void requestSortedPartyList(PartyChangeEvent ignored) {
		refreshNow();
	}

	public void refreshNow() {
		ws.sendObject(Map.of(
				"call", "getSortedPartyList",
				"rseq", SORTED_PARTY_RSEQ
		));
	}

	@HandleEvents(order = -100)
	public void receiveSortedPartyList(EventContext context, ActWsJsonMsg jsonMsg) {
		if (SORTED_PARTY_RSEQ.equals(jsonMsg.getRseq())) {
			log.info("Received sorted party list: {}", jsonMsg.getJson());
		}
	}
}
