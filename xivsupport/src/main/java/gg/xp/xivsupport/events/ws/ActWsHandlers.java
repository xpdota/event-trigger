package gg.xp.xivsupport.events.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.LiveOnly;
import gg.xp.xivsupport.events.ACTLogLineEvent;
import gg.xp.xivsupport.events.actlines.events.RawOnlineStatusChanged;
import gg.xp.xivsupport.events.actlines.events.RawPlayerChangeEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.misc.pulls.PullStatus;
import gg.xp.xivsupport.events.misc.pulls.PullTracker;
import gg.xp.xivsupport.events.state.CombatantsUpdateRaw;
import gg.xp.xivsupport.events.state.InCombatChangeEvent;
import gg.xp.xivsupport.events.state.PartyChangeEvent;
import gg.xp.xivsupport.events.state.RawXivCombatantInfo;
import gg.xp.xivsupport.events.state.RawXivPartyInfo;
import gg.xp.xivsupport.events.state.RefreshCombatantsRequest;
import gg.xp.xivsupport.events.state.RefreshSpecificCombatantsRequest;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivZone;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ActWsHandlers {

	private static final Logger log = LoggerFactory.getLogger(ActWsHandlers.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	private final EventMaster master;
	private final XivState state;
	private final PullTracker pulls;

	public ActWsHandlers(EventMaster master, XivState state, PullTracker pulls) {
		this.master = master;
		this.state = state;
		this.pulls = pulls;
	}

	// Memory saving hacks
	private String lastRawMsg;
	private JsonNode lastJson;

	private Thread combatantsLoopThread;

	@SuppressWarnings("BusyWait")
	@LiveOnly
	@HandleEvents
	public void startCombatantsLoop(EventContext context, ActWsConnectedEvent connected) {
		if (combatantsLoopThread == null) {
			combatantsLoopThread = new Thread(() -> {
				while (true) {
					try {
						// TODO: consider having this refresh rate be dynamic based on things like
						// number of current combatants, or whether the current zone is a raid, or
						// whether a pull is actually started.
						Thread.sleep(2_000);
						master.pushEvent(new RefreshCombatantsRequest());
						for (int i = 0; i < 3; i++) {
							Thread.sleep(2_000);
							Set<Long> fastRefreshEntities = state.getPartyList().stream().map(XivEntity::getId).collect(Collectors.toSet());
							if (pulls.getCurrentStatus() == PullStatus.COMBAT) {
								state.getCombatantsListCopy().stream()
										.filter(XivCombatant::isCombative)
										.map(XivCombatant::getId)
										.forEach(fastRefreshEntities::add);
							}
							master.pushEvent(new RefreshSpecificCombatantsRequest(fastRefreshEntities));
						}
					}
					catch (Throwable t) {
						log.error("Error", t);
					}
				}
			});
			// TODO: should this be turned back on?
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
		Object rseqObj = mapper.convertValue(rseqNode, Object.class);
		if (!rseqNode.isMissingNode()) {
			// Null response - TODO try to match it up with the rseq
			if (!jsonNode.path("$isNull").isMissingNode()) {
				log.trace("Got null ActWS response for rseq {}", rseqNode.intValue());
				return;
			}
			// For now, since this is the only request/response we're using, we can just look for it specifically and
			// not bother with actually matching it back up to a request.
			JsonNode combatantsNode = jsonNode.path("combatants");
			if (combatantsNode.isMissingNode()) {
				log.warn("I don't know how to handle response message: {}", rawMsg);
			}
			else {
				if (rseqObj instanceof String str) {
					rseqObj = str.intern();
				}
				// Just fake the type
				ActWsJsonMsg actWsJsonMsg = new ActWsJsonMsg("combatants", rseqObj, jsonNode);
				context.accept(actWsJsonMsg);
			}
			return;
		}
		JsonNode typeNode = jsonNode.path("type");
		String type;
		if (typeNode.isTextual()) {
			type = typeNode.textValue().intern();
			try {
				((ObjectNode) jsonNode).set("type", new TextNode(type));
			}
			catch (Throwable t) {
				log.error("Error optimizing JsonNode", t);
			}
		}
		else {
			type = null;
		}
		ActWsJsonMsg actWsJsonMsg = new ActWsJsonMsg(type, rseqObj, jsonNode);
		context.accept(actWsJsonMsg);
	}

	@HandleEvents(order = -100)
	public static void actWsLogLine(EventContext context, ActWsJsonMsg jsonMsg) {
		if ("LogLine".equals(jsonMsg.getType())) {
			String rawLine = jsonMsg.getJson().get("rawLine").textValue();
			context.accept(new ACTLogLineEvent(rawLine));
		}
	}

	@HandleEvents(order = -100)
	public static void actWsOnlineStatusChanged(EventContext context, ActWsJsonMsg jsonMsg) {
		if ("OnlineStatusChanged".equals(jsonMsg.getType())) {
			JsonNode json = jsonMsg.getJson();
			long targetId = json.get("target").longValue();
			int rawStatusId = json.get("rawStatus").intValue();
			String statusName = json.get("status").textValue();

			context.accept(new RawOnlineStatusChanged(targetId, rawStatusId, statusName));
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
			context.accept(new RawPlayerChangeEvent(new XivEntity(id, name)));
		}
	}

	@HandleEvents(order = -100)
	public static void actWsZoneChange(EventContext context, ActWsJsonMsg jsonMsg) {
		if ("ChangeZone".equals(jsonMsg.getType())) {
			long id = jsonMsg.getJson().get("zoneID").intValue();
			String name = jsonMsg.getJson().get("zoneName").textValue();
			context.accept(new ZoneChangeEvent(new XivZone(id, name)));
		}
	}

	@HandleEvents(order = -100)
	public static void actWsPartyChange(EventContext context, ActWsJsonMsg jsonMsg) {
		if ("PartyChanged".equals(jsonMsg.getType())) {
			List<RawXivPartyInfo> members = mapper.convertValue(jsonMsg.getJson().path("party"), new TypeReference<>() {
			});
			log.info("Party changed: {}", jsonMsg);
			context.accept(new PartyChangeEvent(members));
		}
	}

	@HandleEvents(order = -100)
	public static void inCombatChange(EventContext context, ActWsJsonMsg jsonMsg) {
		if ("InCombat".equals(jsonMsg.getType())) {
			boolean inCombat = jsonMsg.getJson().get("inGameCombat").booleanValue();
			context.accept(new InCombatChangeEvent(inCombat));
		}
	}

	// Disabled - trying to get off of cactbot events
//	@HandleEvents(order = -100)
//	public static void actWsWipe(EventContext context, ActWsJsonMsg jsonMsg) {
//		if ("onPartyWipe".equals(jsonMsg.getType())) {
//			context.accept(new WipeEvent());
//		}
//	}

	// TODO: clear on zone change
	private final Map<Long, RawXivCombatantInfo> rawCbtCache = new HashMap<>();

	@HandleEvents(order = -100)
	public void actWsCombatants(EventContext context, ActWsJsonMsg jsonMsg) {
		if ("combatants".equals(jsonMsg.getType())) {
			JsonNode combatantsNode = jsonMsg.getJson().path("combatants");
			boolean fullRefresh = "allCombatants".equals(jsonMsg.getRseq());
			List<RawXivCombatantInfo> combatantMaps = mapper.convertValue(combatantsNode, new TypeReference<>() {
			});
			MutableBoolean hasUpdate = new MutableBoolean();
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
						hasUpdate.setTrue();
						return combatant;
					}
				}
			}).filter(Objects::nonNull).collect(Collectors.toList());
			if (hasUpdate.getValue()) {
				context.accept(new CombatantsUpdateRaw(optimizedCombatantMaps, fullRefresh));
			}
		}
	}
//	@HandleEvents
//	public st
}
