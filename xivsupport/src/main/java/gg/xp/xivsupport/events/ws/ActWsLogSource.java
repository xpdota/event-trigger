package gg.xp.xivsupport.events.ws;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import gg.xp.reevent.context.StateStore;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.events.EventSource;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.LiveOnly;
import gg.xp.xivsupport.callouts.audio.PlaySoundFileRequest;
import gg.xp.xivsupport.events.debug.DebugCommand;
import gg.xp.xivsupport.events.state.RefreshCombatantsRequest;
import gg.xp.xivsupport.events.state.RefreshSpecificCombatantsRequest;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import gg.xp.xivsupport.persistence.settings.WsURISetting;
import gg.xp.xivsupport.speech.TtsRequest;
import gg.xp.xivsupport.sys.KnownLogSource;
import gg.xp.xivsupport.sys.PrimaryLogSource;
import gg.xp.xivsupport.sys.Threading;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// TODO: EventSource interface
// TODO: persistent setting of overridden remote player
public class ActWsLogSource implements EventSource {

	private static final Logger log = LoggerFactory.getLogger(ActWsLogSource.class);
	private static final ExecutorService taskPool = Executors.newSingleThreadExecutor(Threading.namedDaemonThreadFactory("OpWs"));
	private static final ExecutorService subTaskPool = Executors.newCachedThreadPool(Threading.namedDaemonThreadFactory("OpWsSub"));
	private static final ObjectMapper mapper = new ObjectMapper();
	private static final URI defaultUri = URI.create("ws://127.0.0.1:10501/ws");
	private final Object connectLock = new Object();
	private final WsURISetting uriSetting;
	private final BooleanSetting allowBadCert;
	private final BooleanSetting allowTts;
	private final BooleanSetting allowSound;
	private final BooleanSetting fastRefreshNonCombatant;

	private final class ActWsClientInternal extends WebSocketClient {

		public ActWsClientInternal() {
			super(uriSetting.get());
			if (allowBadCert.get() && "wss".equalsIgnoreCase(getURI().getScheme())) {


				TrustManager tm = new X509TrustManager() {
					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType) {

					}

					@Override
					public void checkServerTrusted(X509Certificate[] chain, String authType) {

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

		void recheckUri() {
			this.uri = uriSetting.get();
			subTaskPool.submit(this::reconnect);
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
			subscribeEvent("ChangePrimaryPlayer");
			subscribeEvent("ChangeZone");
			subscribeEvent("ChangeMap");
			subscribeEvent("PartyChanged");
			subscribeEvent("LogLine");
			subscribeEvent("InCombat");
			subscribeEvent("OnlineStatusChanged");
			subscribeEvent("GameVersion");
			// EnmityTargetData is spammy even if there is no change
//			send("{\"call\":\"subscribe\",\"events\":[\"EnmityTargetData\"]}");
			log.info("Subscribed to WS events");
		}

		private void subscribeEvent(String event) {
			send("{\"call\":\"subscribe\",\"events\":[\"" + event + "\"]}");
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
		this.allowTts = new BooleanSetting(pers, "actws-allow-tts", true);
		this.allowSound = new BooleanSetting(pers, "actws-allow-sound", false);
		this.fastRefreshNonCombatant = new BooleanSetting(pers, "actws-fast-noncombatants-refresh", false);
		this.pls = pls;
		this.client = new ActWsClientInternal();
		uriSetting.addListener(client::recheckUri);
		// TODO: drop this
		stateStore.putCustom(WsState.class, state);
	}

	private final AtomicInteger rseqCounter = new AtomicInteger();

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

	// TODO: Request* methods should only work if we are running live
	@HandleEvents
	public void requestVersion(EventContext context, ActWsConnectedEvent event) {
		try {
			client.send(mapper.writeValueAsString(Map.of("call", "getVersion", "rseq", "getVersion")));
		}
		catch (JacksonException e) {
			throw new RuntimeException(e);
		}
	}

	@HandleEvents
	public void requestLanguage(EventContext context, ActWsConnectedEvent event) {
		try {
			client.send(mapper.writeValueAsString(Map.of("call", "getLanguage", "rseq", "getLanguage")));
		}
		catch (JacksonException e) {
			throw new RuntimeException(e);
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

	// TODO: this probably doesn't belong here

	@LiveOnly
	@HandleEvents
	public void sayTts(EventContext context, TtsRequest event) {
		if (allowTts.get() && state.isConnected() && !event.isHandled()) {
			String ttsString = event.getTtsString();
			log.info("Sending TTS to ACT: {}", ttsString);
			sendObject(Map.ofEntries(
					Map.entry("call", "say"),
					Map.entry("text", ttsString),
					Map.entry("rseq", rseqCounter.getAndIncrement())));
			event.setHandled();
		}
	}

	@LiveOnly
	@HandleEvents
	public void playSound(EventContext context, PlaySoundFileRequest event) {
		if (allowSound.get() && state.isConnected() && !event.isHandled()) {
			String soundFile = event.getFile().toString();
			log.info("Playing sound via ACT: {}", soundFile);
			sendObject(Map.ofEntries(
					Map.entry("call", "playSound"),
					Map.entry("file", soundFile),
					Map.entry("rseq", rseqCounter.getAndIncrement())));
			event.setHandled();
		}
	}

	public void sendString(String string) {
		client.send(string);
	}

	public void sendObject(Object object) {
		String asString;
		try {
			asString = mapper.writeValueAsString(object);
		}
		catch (JacksonException e) {
			throw new RuntimeException(e);
		}
		sendString(asString);
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
					Thread.sleep(2500);
					if (state.isConnected()) {
						log.info("Ignoring request to open because we are already connected");
						return;
					}
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
					"Type",
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
					"TargetID",
					// I think whether or not it is visible might be in here
					"ModelStatus",
					"IsTargetable",
					"TransformationId",
					"WeaponId",
					"Radius"
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
		catch (JacksonException e) {
			throw new RuntimeException("Could not build JSON request", e);
		}
	}

	public WsURISetting getUriSetting() {
		return uriSetting;
	}

	public BooleanSetting getAllowBadCert() {
		return allowBadCert;
	}

	public BooleanSetting getAllowTts() {
		return allowTts;
	}

	public BooleanSetting getAllowSound() {
		return allowSound;
	}

	public BooleanSetting getFastRefreshNonCombatant() {
		return fastRefreshNonCombatant;
	}

	public boolean isConnected() {
		return state.isConnected();
	}
}
