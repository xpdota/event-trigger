package gg.xp.telestosupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.ActorControlEvent;
import gg.xp.xivsupport.events.actlines.events.MapChangeEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.state.PartyChangeEvent;
import gg.xp.xivsupport.events.state.PartyForceOrderChangeEvent;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.HttpURISetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TelestoMain {

	private static final Logger log = LoggerFactory.getLogger(TelestoMain.class);
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

	public TelestoMain(EventMaster master, PersistenceProvider pers) {
		this.master = master;
		try {
			uriSetting = new HttpURISetting(pers, "telesto-support.uri", new URI("http://localhost:51323/"));
			enablePartyList = new BooleanSetting(pers, "telesto-support.pull-party-list", true);
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@HandleEvents
	public void handleGameCommand(EventContext context, TelestoGameCommand event) {
		String cmd = event.getCommand();
		context.accept(makeMessage(GAME_CMD_ID, "ExecuteCommand", Map.of("command", cmd)));
	}

	private TelestoMessage makeMessage(int id, @NotNull String type, @Nullable Object payload) {
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
				)
		);
	}

	private TelestoMessage makeMessage(Map<String, Object> payload) {
		return new TelestoMessage(mapper.valueToTree(payload));
	}

	@HandleEvents
	public void handlePartyChange(EventContext context, PartyChangeEvent pce) {
		if (enablePartyList.get()) {
			context.accept(makeMessage(PARTY_UPDATE_ID, "GetPartyMembers", null));
		}
	}

	@HandleEvents
	public void handlePartyChange(EventContext context, ActorControlEvent ace) {
		if (enablePartyList.get()) {
			context.accept(makeMessage(PARTY_UPDATE_ID, "GetPartyMembers", null));
		}
	}

	@HandleEvents
	public void handlePartyChange(EventContext context, ZoneChangeEvent zce) {
		if (enablePartyList.get()) {
			context.accept(makeMessage(PARTY_UPDATE_ID, "GetPartyMembers", null));
		}
	}

	@HandleEvents
	public void handlePartyChange(EventContext context, MapChangeEvent mce) {
		if (enablePartyList.get()) {
			context.accept(makeMessage(PARTY_UPDATE_ID, "GetPartyMembers", null));
		}
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
	public void handleMessage(EventContext context, TelestoMessage msg) {
		exs.submit(() -> {
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
					master.pushEvent(new TelestoResponse(mapper.readValue(response.body(), new TypeReference<>() {
					})));
				}
				else {
					log.error("Error in Telesto response: {} {}", response.statusCode(), response.body());
				}
				updateStatus(TelestoStatus.GOOD);
			}
			catch (Throwable e) {
				log.error("Error sending Telesto message", e);
				updateStatus(TelestoStatus.BAD);
			}
		});

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
}
