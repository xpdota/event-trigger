package gg.xp.telestosupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.InitEvent;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.actlines.events.ActorControlEvent;
import gg.xp.xivsupport.events.actlines.events.MapChangeEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.state.PartyChangeEvent;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.HttpURISetting;
import gg.xp.xivsupport.sys.KnownLogSource;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
	public static final int GAME_CMD_ID = 1_000_000;
	public static final int PARTY_UPDATE_ID = 1_000_001;
	private final EventMaster master;
	private final RefreshLoop<TelestoMain> refresher;

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
		refresher = new RefreshLoop<>("TelestoPartyRefresh", this, TelestoMain::refreshPartyList, tm -> getStatus() == TelestoStatus.GOOD ? 10_000L : 60_000L);
	}

	private void refreshPartyList() {
		if (enablePartyList.get()) {
			master.pushEvent(makePartyMemberMsg());
		}
	}

	@HandleEvents
	public void handleGameCommand(EventContext context, TelestoGameCommand event) {
		String cmd = event.getCommand();
		context.accept(makeMessage(GAME_CMD_ID, "ExecuteCommand", Map.of("command", cmd), true));
	}

	public TelestoOutgoingMessage makeMessage(int id, @NotNull String type, @Nullable Object payload, boolean delay) {
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

	public TelestoOutgoingMessage makeMessage(Map<String, Object> payload, boolean delay) {
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

	public @Nullable HttpResponse<String> sendMessageDirectly(TelestoOutgoingMessage msg) {
		if (!enabled()) {
			return null;
		}
		String body;
		try {
			body = mapper.writeValueAsString(msg.getJson());
			log.trace("Sending Telesto message: {}", body);
			HttpResponse<String> response = http.send(
					HttpRequest
							.newBuilder(uriSetting.get())
							.POST(
									HttpRequest.BodyPublishers
											.ofString(
													body)).build(),
					HttpResponse.BodyHandlers.ofString());
			return response;
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}

	}

	@HandleEvents
	public void handleMessage(EventContext context, TelestoOutgoingMessage msg) {
		Runnable task = () -> {
			try {
				log.trace("Telesto message done");
				HttpResponse<String> response = sendMessageDirectly(msg);
				if (response == null) {
					return;
				}
				if (response.statusCode() == 200) {
					TelestoResponse event = new TelestoResponse(mapper.readValue(response.body(), new TypeReference<>() {
					}));
					event.setResponseTo(msg);
					master.pushEvent(event);
				}
				else {
					TelestoHttpError error = new TelestoHttpError(response);
					error.setResponseTo(msg);
					master.pushEvent(error);
					log.error("Error in Telesto response: {} {}", response.statusCode(), response.body());
				}
				updateStatus(TelestoStatus.GOOD);
			}
			catch (Throwable e) {
				log.error("Error sending Telesto message {}", e.toString());
				TelestoConnectionError error = new TelestoConnectionError(e);
				error.setResponseTo(msg);
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
			if (newStatus == TelestoStatus.GOOD) {
				refresher.startIfNotStarted();
			}

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

	private boolean enabled() {
		return pls.getLogSource() == KnownLogSource.WEBSOCKET_LIVE;
	}

	@Override
	public boolean enabled(EventContext context) {
		return enabled();
	}
}
