package gg.xp.telestosupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.ActorControlEvent;
import gg.xp.xivsupport.events.actlines.events.MapChangeEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.state.PartyChangeEvent;
import gg.xp.xivsupport.events.state.PartyForceOrderChangeEvent;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.HttpURISetting;
import gg.xp.xivsupport.sys.KnownLogSource;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelestoMain implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(TelestoMain.class);
	// Being used as a queue
	private static final ExecutorService queueExs = Executors.newSingleThreadExecutor();
	// Handles the actual execution
	private static final ExecutorService exs = Executors.newCachedThreadPool();
	private final HttpClient http = HttpClient.newBuilder().build();
	private final ObjectMapper mapper = new ObjectMapper();
	private final HttpURISetting uriSetting;
	private final BooleanSetting enablePartyList;

	private static final int VERSION = 1;
	private static final int GAME_CMD_ID = 1_000_000;
	private static final int PARTY_UPDATE_ID = 1_000_001;
	private final EventMaster master;

	private volatile TelestoStatus status = TelestoStatus.UNKNOWN;
	private final PrimaryLogSource pls;

	public TelestoMain(EventMaster master, PersistenceProvider pers, PrimaryLogSource pls) {
		this.master = master;
		this.pls = pls;
		try {
			uriSetting = new HttpURISetting(pers, "telesto-support.uri", new URI("http://localhost:51323/"));
			// TODO: Telesto bug....
//			uriSetting = new HttpURISetting(pers, "telesto-support.uri", new URI("http://127.0.0.1:51323/"));
			enablePartyList = new BooleanSetting(pers, "telesto-support.pull-party-list", true);
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@HandleEvents
	public void handleGameCommand(EventContext context, TelestoGameCommand event) {
		String cmd = event.getCommand();
		context.accept(makeMessage(GAME_CMD_ID, "ExecuteCommand", Map.of("command", cmd), true));
	}

	private TelestoOutgoingMessage makeMessage(int id, @NotNull String type, @Nullable Object payload, boolean delay) {
		if (payload == null) {
			//noinspection AssignmentToMethodParameter
			payload = Collections.emptyMap();
		}
		return makeMessage(
				Map.of(
						"version", VERSION,
						"id", id,
						"type", type,
						"payload", payload
				), delay);
	}

	private TelestoOutgoingMessage makeMessage(Map<String, Object> payload, boolean delay) {
		return new TelestoOutgoingMessage(mapper.valueToTree(payload), delay);
	}

	@HandleEvents
	public void handlePartyChange(EventContext context, PartyChangeEvent pce) {
		if (enablePartyList.get()) {
			context.accept(makePartyMemberMsg());
		}
	}

	private volatile ActorControlEvent lastAce;

	@HandleEvents
	public void handlePartyChange(EventContext context, ActorControlEvent ace) {
		if (enablePartyList.get()) {
			if (lastAce == null || lastAce.getEffectiveTimeSince().compareTo(Duration.ofSeconds(5)) > 0) {
				context.accept(makePartyMemberMsg());
				lastAce = ace;
			}
		}
	}

	@HandleEvents
	public void handlePartyChange(EventContext context, ZoneChangeEvent zce) {
		if (enablePartyList.get()) {
			context.accept(makePartyMemberMsg());
		}
	}

	@HandleEvents
	public void handlePartyChange(EventContext context, MapChangeEvent mce) {
		if (enablePartyList.get()) {
			context.accept(makePartyMemberMsg());
		}
	}

	private TelestoOutgoingMessage makePartyMemberMsg() {
		return makeMessage(PARTY_UPDATE_ID, "GetPartyMembers", null, false);
	}


	@HandleEvents
	public void handlePartyReponse(EventContext context, TelestoResponse event) {
		if (event.getId() == PARTY_UPDATE_ID) {
			log.info("Received Telesto party list");
			List<Map<String, Object>> partyData = (List<Map<String, Object>>) event.getResponse();
			List<Long> partyActorIds = partyData.stream()
					.sorted(Comparator.comparing(entry -> Integer.parseInt(entry.get("order").toString(), 16)))
					.map(entry -> entry.get("actor"))
					.filter(Objects::nonNull)
					.map(Object::toString)
					.filter(s -> !s.isBlank())
					.map(str -> Long.parseLong(str, 16))
					.toList();
			log.info("New Telesto Party List: {}", partyActorIds);
			context.accept(new PartyForceOrderChangeEvent(partyActorIds.isEmpty() ? null : partyActorIds));
		}
	}

	@HandleEvents
	public void handleMessage(EventContext context, TelestoOutgoingMessage msg) {
		Runnable task = () -> {
			try {
				String body = mapper.writeValueAsString(msg.getJson());
				log.info("Sending Telesto message: {}", body);
				HttpResponse<String> response = http.send(
						HttpRequest
								.newBuilder(uriSetting.get())
								.POST(
										HttpRequest.BodyPublishers
												.ofString(
														body)).build(),
						HttpResponse.BodyHandlers.ofString());
				log.info("Telesto message done");
				if (response.statusCode() == 200) {
					TelestoResponse event = new TelestoResponse(mapper.readValue(response.body(), new TypeReference<>() {
					}));
					event.setParent(msg);
					master.pushEvent(event);
				}
				else {
					TelestoHttpError error = new TelestoHttpError(response);
					error.setParent(msg);
					master.pushEvent(error);
					log.error("Error in Telesto response: {} {}", response.statusCode(), response.body());
				}
				updateStatus(TelestoStatus.GOOD);
			}
			catch (Throwable e) {
				log.error("Error sending Telesto message {}", e.toString());
				TelestoConnectionError error = new TelestoConnectionError(e);
				error.setParent(msg);
				master.pushEvent(error);
				updateStatus(TelestoStatus.BAD);
			}
		};
		if (msg.shouldDelay()) {
			queueExs.submit(() -> {
				exs.submit(task);
				try {
					// Insert delay to avoid spamming
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
					log.error("Interrupted", e);
				}
			});
		}
		else {
			exs.submit(task);
		}

	}

	private void updateStatus(TelestoStatus newStatus) {
		TelestoStatus oldStatus = status;
		if (newStatus != oldStatus) {
			status = newStatus;
			master.pushEvent(new TelestoStatusUpdatedEvent(oldStatus, status));

		}
	}

	public HttpURISetting getUriSetting() {
		return uriSetting;
	}

	public TelestoStatus getStatus() {
		return status;
	}

	public BooleanSetting getEnablePartyList() {
		return enablePartyList;
	}

	@Override
	public boolean enabled(EventContext context) {
		return pls.getLogSource() == KnownLogSource.WEBSOCKET_LIVE;
	}
}
