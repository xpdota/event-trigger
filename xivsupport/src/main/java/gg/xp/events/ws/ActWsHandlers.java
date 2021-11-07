package gg.xp.events.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.events.ACTLogLineEvent;
import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.actlines.events.PlayerChangeEvent;
import gg.xp.events.actlines.events.ZoneChangeEvent;
import gg.xp.events.models.XivEntity;
import gg.xp.events.models.XivJob;
import gg.xp.events.models.XivPlayerCharacter;
import gg.xp.events.models.XivWorld;
import gg.xp.events.models.XivZone;
import gg.xp.events.state.PartyChangeEvent;
import gg.xp.scan.HandleEvents;

import java.util.ArrayList;
import java.util.List;

public class ActWsHandlers {

	private static final ObjectMapper mapper = new ObjectMapper();

	@HandleEvents
	public static void actWsRawToJson(EventContext<Event> context, ActWsRawMsg rawMsg) {
		JsonNode jsonNode;
		try {
			jsonNode = mapper.readTree(rawMsg.getRawMsgData());
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		JsonNode typeNode = jsonNode.get("type");
		String type;
		if (typeNode.isTextual()) {
			type = typeNode.textValue();
		} else {
			type = null;
		}
		ActWsJsonMsg actWsJsonMsg = new ActWsJsonMsg(type, jsonNode);
		context.accept(actWsJsonMsg);
	}

	@HandleEvents
	public static void actWsLogLine(EventContext<Event> context, ActWsJsonMsg jsonMsg) {
		if ("LogLine".equals(jsonMsg.getType())) {
			context.enqueue(new ACTLogLineEvent(jsonMsg.getJson().get("rawLine").textValue()));
		}
	}

	// Player/Zone/Party uses specific WS feeds rather than normal log lines, since the WS endpoint will send us the
	// current values when we subscribe to the events, unlike log lines which will only trigger when there is a change.
	// i.e. it would only work after a zone change.
	@HandleEvents
	public static void actWsPlayerChange(EventContext<Event> context, ActWsJsonMsg jsonMsg) {
		if ("ChangePrimaryPlayer".equals(jsonMsg.getType())) {
			long id = jsonMsg.getJson().get("charID").intValue();
			String name = jsonMsg.getJson().get("charName").textValue();
			context.enqueue(new PlayerChangeEvent(new XivEntity(id, name)));
		}
	}

	@HandleEvents
	public static void actWsZoneChange(EventContext<Event> context, ActWsJsonMsg jsonMsg) {
		if ("ChangeZone".equals(jsonMsg.getType())) {
			long id = jsonMsg.getJson().get("zoneID").intValue();
			String name = jsonMsg.getJson().get("zoneName").textValue();
			context.enqueue(new ZoneChangeEvent(new XivZone(id, name)));
		}
	}

	@HandleEvents
	public static void actWsPartyChange(EventContext<Event> context, ActWsJsonMsg jsonMsg) {
		if ("PartyChanged".equals(jsonMsg.getType())) {
			List<XivPlayerCharacter> members = new ArrayList<>();
			// TODO: consider using automatic deserialization rather than doing it manually
			for (JsonNode partyMember : jsonMsg.getJson().get("party")) {
				String name = partyMember.get("name").textValue();
				long id = Long.parseLong(partyMember.get("id").textValue(), 16);
				int world = partyMember.get("worldId").intValue();
				int job = partyMember.get("job").intValue();
				int level = partyMember.get("level").intValue();
				members.add(new XivPlayerCharacter(id, name, new XivJob(), new XivWorld(), level));
			}
			context.enqueue(new PartyChangeEvent(members));
		}
	}


//	@HandleEvents
//	public st
}
