package gg.xp.xivsupport.events.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.xivsupport.events.actlines.events.RawPlayerChangeEvent;
import gg.xp.xivsupport.events.actlines.events.WipeEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.events.state.CombatantsUpdateRaw;
import gg.xp.xivsupport.events.state.PartyChangeEvent;
import gg.xp.xivsupport.events.state.RawXivCombatantInfo;
import gg.xp.xivsupport.events.state.RawXivPartyInfo;
import gg.xp.xivsupport.events.state.RefreshCombatantsRequest;
import gg.xp.reevent.scan.HandleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ActWsHandlers {

	private static final Logger log = LoggerFactory.getLogger(ActWsHandlers.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	private final EventMaster master;

	public ActWsHandlers(EventMaster master) {
		this.master = master;
	}

	// Memory saving hacks
	private String lastRawMsg;
	private JsonNode lastJson;

	private Thread combatantsLoopThread;

	@HandleEvents
	public void startCombatantsLoop(EventContext context, ActWsConnectedEvent connected) {
		if (combatantsLoopThread == null) {
			combatantsLoopThread = new Thread(() -> {
				while (true) {
					try {
						Thread.sleep(1000);
						master.pushEvent(new RefreshCombatantsRequest());
					}
					catch (Throwable t) {
						log.error("Error", t);
					}
				}
			});
			combatantsLoopThread.start();
		}

	}

	@HandleEvents(order = -100)
	public void actWsRawToJson(EventContext context, ActWsRawMsg rawMsg) {
		JsonNode jsonNode;
		String raw = rawMsg.getRawMsgData();
		if (raw.equals(lastRawMsg)) {
			jsonNode = lastJson;
		}
		else {
			try {
				jsonNode = mapper.readTree(raw);
				lastJson = jsonNode;
			}
			catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}
		lastRawMsg = raw;
		// Two types of messages - subscriptions, and responses to explicit requests.
		// Subscribed messages have a 'type' field that lets us know what it is.
		// Responses instead have an 'rseq' field that lets us match up the response to the request.
		JsonNode rseqNode = jsonNode.path("rseq");
		if (!rseqNode.isMissingNode()) {
			// Null response - TODO try to match it up with the rseq
			if (!jsonNode.path("$isNull").isMissingNode()) {
				log.debug("Got null ActWS response for rseq {}", rseqNode.intValue());
				return;
			}
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
		}
		else {
			type = null;
		}
		ActWsJsonMsg actWsJsonMsg = new ActWsJsonMsg(type, jsonNode);
		context.accept(actWsJsonMsg);
	}

	@HandleEvents(order = -100)
	public static void actWsLogLine(EventContext context, ActWsJsonMsg jsonMsg) {
		if ("LogLine".equals(jsonMsg.getType())) {
			context.enqueue(new ACTLogLineEvent(jsonMsg.getJson().get("rawLine").textValue()));
		}
	}

	// Player/Zone/Party uses specific WS feeds rather than normal log lines, since the WS endpoint will send us the
	// current values when we subscribe to the events, unlike log lines which will only trigger when there is a change.
	// i.e. it would only work after a zone change.
	@HandleEvents(order = -100)
	public static void actWsPlayerChange(EventContext context, ActWsJsonMsg jsonMsg) {
		if ("ChangePrimaryPlayer".equals(jsonMsg.getType())) {
			long id = jsonMsg.getJson().get("charID").intValue();
			String name = jsonMsg.getJson().get("charName").textValue();
			context.enqueue(new RawPlayerChangeEvent(new XivEntity(id, name)));
		}
	}

	@HandleEvents(order = -100)
	public static void actWsZoneChange(EventContext context, ActWsJsonMsg jsonMsg) {
		if ("ChangeZone".equals(jsonMsg.getType())) {
			long id = jsonMsg.getJson().get("zoneID").intValue();
			String name = jsonMsg.getJson().get("zoneName").textValue();
			context.enqueue(new ZoneChangeEvent(new XivZone(id, name)));
		}
	}

	@HandleEvents(order = -100)
	public static void actWsPartyChange(EventContext context, ActWsJsonMsg jsonMsg) {
		if ("PartyChanged".equals(jsonMsg.getType())) {
			List<RawXivPartyInfo> members = mapper.convertValue(jsonMsg.getJson().path("party"), new TypeReference<>() {
			});
//			// TODO: consider using automatic deserialization rather than doing it manually
//
//			for (JsonNode partyMember : jsonMsg.getJson().get("party")) {
//				mapper.convertValue(partyMember)
//				String name = partyMember.get("name").textValue();
//				long id = Long.parseLong(partyMember.get("id").textValue(), 16);
//				int world = partyMember.get("worldId").intValue();
//				int job = partyMember.get("job").intValue();
//				int level = partyMember.get("level").intValue();
//				members.add(new RawXivPartyInfo(id, name, world, job, level));
//			}
			log.info("Party changed: {}", jsonMsg);
			context.enqueue(new PartyChangeEvent(members));
		}
	}

	// Disabled - trying to get off of cactbot events
//	@HandleEvents(order = -100)
//	public static void actWsWipe(EventContext context, ActWsJsonMsg jsonMsg) {
//		if ("onPartyWipe".equals(jsonMsg.getType())) {
//			context.enqueue(new WipeEvent());
//		}
//	}

	// TODO: clear on zone change
	private final Map<Long, RawXivCombatantInfo> rawCbtCache = new HashMap<>();

	@HandleEvents(order = -100)
	public void actWsCombatants(EventContext context, ActWsJsonMsg jsonMsg) {
		if ("combatants".equals(jsonMsg.getType())) {
			JsonNode combatantsNode = jsonMsg.getJson().path("combatants");
			List<RawXivCombatantInfo> combatantMaps = mapper.convertValue(combatantsNode, new TypeReference<>() {
			});
			List<RawXivCombatantInfo> optimizedCombatantMaps = combatantMaps.stream().map(combatant -> {
				long id = combatant.getId();
				if (id == 0xE0000000) {
					// Don't bother with these. Since we map by ID anyway, they'd end up overwriting each other. Waste of memory.
					return null;
				}
				else {
					RawXivCombatantInfo cached = rawCbtCache.get(id);
					if (combatant.equals(cached)) {
						return cached;
					}
					else {
						rawCbtCache.put(id, combatant);
						return combatant;
					}
				}
			}).filter(Objects::nonNull).collect(Collectors.toList());
			context.enqueue(new CombatantsUpdateRaw(optimizedCombatantMaps));
		}
	}
//	@HandleEvents
//	public st
}
