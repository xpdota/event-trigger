package gg.xp.events.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.events.ACTLogLineEvent;
import gg.xp.events.Event;
import gg.xp.events.EventContext;
import gg.xp.events.actlines.data.Job;
import gg.xp.events.actlines.events.PlayerChangeEvent;
import gg.xp.events.actlines.events.WipeEvent;
import gg.xp.events.actlines.events.ZoneChangeEvent;
import gg.xp.events.models.XivEntity;
import gg.xp.events.models.XivPlayerCharacter;
import gg.xp.events.models.XivWorld;
import gg.xp.events.models.XivZone;
import gg.xp.events.state.CombatantInfo;
import gg.xp.events.state.CombatantsUpdateRaw;
import gg.xp.events.state.PartyChangeEvent;
import gg.xp.scan.HandleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ActWsHandlers {

	private static final Logger log = LoggerFactory.getLogger(ActWsHandlers.class);
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
		// Two types of messages - subscriptions, and responses to explicit requests.
		// Subscribed messages have a 'type' field that lets us know what it is.
		// Responses instead have an 'rseq' field that lets us match up the response to the request.
		JsonNode rseqNode = jsonNode.path("rseq");
		if (!rseqNode.isMissingNode()) {
			// For now, since this is the only request/response we're using, we can just look for it specifically and
			// not bother with actually matching it back up to a request.
			JsonNode combatantsNode = jsonNode.path("combatants");
			if (combatantsNode.isMissingNode()) {
				log.warn("I don't know how to handle response message: {}", rawMsg);
			}
			else {
				// Just fake the type
				ActWsJsonMsg actWsJsonMsg = new ActWsJsonMsg("combatants", jsonNode);
				context.accept(actWsJsonMsg);
			}
			return;
		}
		JsonNode typeNode = jsonNode.path("type");
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
				members.add(new XivPlayerCharacter(id, name, Job.getById(job), new XivWorld(), level));
			}
			context.enqueue(new PartyChangeEvent(members));
		}
	}

	@HandleEvents
	public static void actWsWipe(EventContext<Event> context, ActWsJsonMsg jsonMsg) {
		if ("onPartyWipe".equals(jsonMsg.getType())) {
			context.enqueue(new WipeEvent());
		}
	}

	@HandleEvents
	public static void actWsCombatants(EventContext<Event> context, ActWsJsonMsg jsonMsg) {
		if ("combatants".equals(jsonMsg.getType())) {
			JsonNode combatantsNode = jsonMsg.getJson().path("combatants");
			List<CombatantInfo> combatantMaps = mapper.convertValue(combatantsNode, new TypeReference<List<CombatantInfo>>() {
			});
			context.enqueue(new CombatantsUpdateRaw(combatantMaps));
		}
	}
//	@HandleEvents
//	public st
}
