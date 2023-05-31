package gg.xp.telestosupport;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.state.PartyForceOrderChangeEvent;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static gg.xp.telestosupport.TelestoMain.PARTY_UPDATE_ID;

public class TelestoPartyListHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(TelestoPartyListHandler.class);

	private final BooleanSetting enablePartyList;
	private List<Long> partyActorIds;

	public TelestoPartyListHandler(TelestoMain main) {
		enablePartyList = main.getEnablePartyList();
	}

	@Override
	public boolean enabled(EventContext context) {
		return enablePartyList.get();
	}

	@HandleEvents
	public void handlePartyReponse(EventContext context, TelestoResponse event) {
		if (event.getId() == PARTY_UPDATE_ID) {
			log.trace("Received Telesto party list");
			List<Map<String, Object>> partyData = (List<Map<String, Object>>) event.getResponse();
			List<Long> partyActorIds = partyData.stream()
					.sorted(Comparator.comparing(entry -> Integer.parseInt(entry.get("order").toString(), 16)))
					.map(entry -> entry.get("actor"))
					.filter(Objects::nonNull)
					.map(Object::toString)
					.filter(s -> !s.isBlank())
					.map(str -> Long.parseLong(str, 16))
					.toList();
			if (!Objects.equals(this.partyActorIds, partyActorIds)) {
				this.partyActorIds = partyActorIds;
				log.info("New Telesto Party List: {}", partyActorIds);
				context.accept(new PartyForceOrderChangeEvent(partyActorIds.isEmpty() ? null : partyActorIds));
			}
			else {
				log.trace("Ignored Telesto party list update because it is identical to the previous.");
			}
		}
	}
}
