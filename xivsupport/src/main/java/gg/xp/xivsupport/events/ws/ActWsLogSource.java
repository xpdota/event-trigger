package gg.xp.xivsupport.events.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.reevent.context.StateStore;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.EventSource;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.LiveOnly;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.state.RefreshCombatantsRequest;
import gg.xp.xivsupport.events.state.RefreshSpecificCombatantsRequest;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.WsURISetting;
import gg.xp.xivsupport.speech.TtsRequest;
import gg.xp.xivsupport.sys.KnownLogSource;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// TODO: EventSource interface
public class ActWsLogSource implements EventSource {

	private static final Logger log = LoggerFactory.getLogger(ActWsLogSource.class);
	private static final ExecutorService taskPool = Executors.newSingleThreadExecutor(new BasicThreadFactory.Builder().namingPattern("ActWsLogPool-%d").build());
	private static final ObjectMapper mapper = new ObjectMapper();
	private static final URI defaultUri = URI.create("ws://127.0.0.1:10501/ws");
	private final Object connectLock = new Object();
	private final WsURISetting uriSetting;
	private final BooleanSetting allowBadCert;

	private final class ActWsClientInternal extends WebSocketClient {

		public ActWsClientInternal() {
			super(uriSetting.get());
			if (allowBadCert.get() && "wss".equalsIgnoreCase(getURI().getScheme())) {


				TrustManager tm = new X509TrustManager() {
					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

					}

					@Override
					public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

					}

					@Override
					public X509Certificate[] getAcceptedIssuers() {
						return new X509Certificate[0];
					}
				};

				SSLContext sslContext;
				try {
					sslContext = SSLContext.getInstance("TLS");
					sslContext.init(null, new TrustManager[]{tm}, null);
				}
				catch (NoSuchAlgorithmException | KeyManagementException e) {
					throw new RuntimeException(e);
				}

				SSLSocketFactory factory = sslContext.getSocketFactory();

				setSocketFactory(factory);
			}
		}

		@Override
		public void onOpen(ServerHandshake serverHandshake) {
			log.info("Open: {}", serverHandshake);
			subscribeEvents();
			state.setConnected(true);
			eventConsumer.accept(new ActWsConnectedEvent());
		}

		@Override
		public void onMessage(String s) {
			log.trace("WS Message: {}", s);
			eventConsumer.accept(new ActWsRawMsg(s));
		}

		@Override
		public void onClose(int i, String s, boolean b) {
			log.info("Close: {};{};{}", i, s, b);
			state.setConnected(false);
			eventConsumer.accept(new ActWsDisconnectedEvent());
		}

		@Override
		public void onError(Exception e) {
			if (e.getMessage().equals("Connection refused: connect")) {
				log.info("WS Connection refused, waiting then trying again");
			}
			else {
				log.error("WS Error!", e);
			}
			// TODO: hmmm.....is an "error" always fatal here? We *should* reconnect just in case, but I just don't
			// know how to differentiate between fatal (which we should ignore, because onClose will get called next)
			// and non-fatal (we need to reconnect it outselves).
			eventConsumer.accept(new ActWsReconnectRequest());
		}

		private void subscribeEvents() {
			log.info("Subscribing to WS events");
			send("{\"call\":\"subscribe\",\"events\":[\"ChangePrimaryPlayer\"]}");
			send("{\"call\":\"subscribe\",\"events\":[\"ChangeZone\"]}");
			send("{\"call\":\"subscribe\",\"events\":[\"PartyChanged\"]}");
//		send("{\"call\":\"subscribe\",\"events\":[\"onPlayerChangedEvent\"]}");
			// TODO: there does not seem to be a non-cactbot alternative to this
//			send("{\"call\":\"subscribe\",\"events\":[\"onInCombatChangedEvent\"]}");
			send("{\"call\":\"subscribe\",\"events\":[\"LogLine\"]}");
			send("{\"call\":\"subscribe\",\"events\":[\"InCombat\"]}");
			// EnmityTargetData is spammy even if there is no change
//			send("{\"call\":\"subscribe\",\"events\":[\"EnmityTargetData\"]}");
			send("{\"call\":\"subscribe\",\"events\":[\"OnlineStatusChanged\"]}");
			log.info("Subscribed to WS events");
		}
	}

	private final Consumer<Event> eventConsumer;
	private final PrimaryLogSource pls;
	private final ActWsClientInternal client;
	private final WsState state = new WsState();

	public ActWsLogSource(EventMaster master, StateStore stateStore, PersistenceProvider pers, PrimaryLogSource pls) {
		this.uriSetting = new WsURISetting(pers, "actws-uri", defaultUri);
		this.allowBadCert = new BooleanSetting(pers, "acts-allow-bad-cert", false);
		this.eventConsumer = master::pushEvent;
		this.pls = pls;
		this.client = new ActWsClientInternal();
		stateStore.putCustom(WsState.class, state);
	}

	public void sendMessage(Object message) {
		try {
			sendMessage(mapper.writeValueAsString(message));
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public void sendMessage(String message) {
		client.send(message);
	}


	@LiveOnly
	@HandleEvents
	public void getCombatantsDbg(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("combatants")) {
			context.accept(new RefreshCombatantsRequest());
		}
	}

	@LiveOnly
	@HandleEvents
	public void forceReconnect(EventContext context, DebugCommand event) {
		if (event.getCommand().equals("reconnect_ws")) {
			log.info("Reconnect requested");
			context.accept(new ActWsReconnectRequest());
		}
	}

	@LiveOnly
	@HandleEvents
	public void getCombatants(EventContext context, RefreshCombatantsRequest event) {
		client.send(allCbtRequest);
	}

	@LiveOnly
	@HandleEvents
	public void getCombatant(EventContext context, RefreshSpecificCombatantsRequest event) {
		// Trying to do this to avoid some overhead, not sure if it's actually better
		String myRequest = specificCbtRequestTemplate.replaceAll("123456", event.getCombatants().stream().map(Object::toString).collect(Collectors.joining(", ")));
		client.send(myRequest);
	}


	@LiveOnly
	@HandleEvents
	public void requestReconnect(EventContext context, ActWsReconnectRequest event) {
		log.info("Forcing connection closed, should auto-reconnect");
		doClose();
	}

	@LiveOnly
	@HandleEvents
	public void reconnectActWs(EventContext context, ActWsDisconnectedEvent event) {
		log.info("Disconnected, reconnecting");
		doReconnect();
	}


	private void doClose() {
		taskPool.submit(() -> {
			synchronized (connectLock) {
				if (!state.isConnected()) {
					log.info("Ignoring request to close because we are already disconnected");
					return;
				}
				log.info("Disconnecting");
				try {
					client.closeBlocking();
					log.info("Disconnected");
				}
				catch (InterruptedException e) {
					log.error("Interrupted", e);
				}
			}
		});
	}

	private void doOpen() {
		taskPool.submit(() -> {
			synchronized (connectLock) {
				if (state.isConnected()) {
					log.info("Ignoring request to open because we are already connected");
					return;
				}
				log.info("Connecting");
				try {
					boolean connected = client.connectBlocking();
					if (connected) {
						log.info("Connected");
					}
					else {
						log.warn("Not Connected");
					}
				}
				catch (InterruptedException e) {
					log.error("Interrupted", e);
				}
			}
		});
	}

	@SuppressWarnings("SleepWhileHoldingLock")
	private void doReconnect() {
		taskPool.submit(() -> {
			synchronized (connectLock) {
				if (state.isConnected()) {
					log.info("Ignoring request to open because we are already connected");
					return;
				}
				try {
					Thread.sleep(1000);
					log.info("Reconnecting");
					boolean connected = client.reconnectBlocking();
					if (connected) {
						log.info("Reconnected");
					}
					else {
						log.warn("Not Reconnected");
					}
				}
				catch (InterruptedException e) {
					log.error("Interrupted", e);
				}
			}
		});
	}

	public void start() {
		// TODO: auto retry and reconnection
		log.info("Attempting connection");
		doOpen();
		pls.setLogSource(KnownLogSource.WEBSOCKET_LIVE);
	}

	private static final String allCbtRequest;
	private static final String specificCbtRequestTemplate;

	static {
		try {
			String[] cbtProps = {
					"CurrentWorldID",
					"WorldID",
					"WorldName",
					"BNpcID",
					"BNpcNameID",
					"PartyType",
					"ID",
					"OwnerID",
					"type",
					"Job",
					"Level",
					"Name",
					"CurrentHP",
					"MaxHP",
					"CurrentMP",
					"MaxMP",
					"PosX",
					"PosY",
					"PosZ",
					"Heading",
					"TargetID"
			};
			allCbtRequest = mapper.writeValueAsString(
					Map.ofEntries(
							Map.entry("call", "getCombatants"),
							Map.entry("rseq", "allCombatants"),
							Map.entry("props", cbtProps)
					));
			specificCbtRequestTemplate = mapper.writeValueAsString(
					Map.ofEntries(
							Map.entry("call", "getCombatants"),
							Map.entry("rseq", "specificCombatants"),
							Map.entry("ids", List.of(123456)),
							Map.entry("props", cbtProps)
					));
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Could not build JSON request", e);
		}
	}

	public WsURISetting getUriSetting() {
		return uriSetting;
	}

	public BooleanSetting getAllowBadCert() {
		return allowBadCert;
	}
}
