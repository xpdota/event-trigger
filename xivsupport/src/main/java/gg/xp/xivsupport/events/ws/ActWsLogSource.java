package gg.xp.xivsupport.events.ws;

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
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
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

/*
Simplified way of making this work:

The inner client is replaceable rather than final.
When you disconnect or change settings, create a new client.
 */
public class ActWsLogSource implements EventSource {

	private static final Logger log = LoggerFactory.getLogger(ActWsLogSource.class);
	private static final ExecutorService taskPool = Executors.newCachedThreadPool(Threading.namedDaemonThreadFactory("OpWs"));
	private static final ObjectMapper mapper = new ObjectMapper();
	private static final URI defaultUri = URI.create("ws://127.0.0.1:10501/ws");

	// Settings
	private final WsURISetting uriSetting;
	private final BooleanSetting allowBadCert;
	private final BooleanSetting allowTts;
	private final BooleanSetting allowSound;
	private final BooleanSetting fastRefreshNonCombatant;

	private final Object stateLock = new Object();

	private enum ClientState {
		NOT_STARTED,
		OPENING,
		CONNECTED,
		DISCONNECTED,
	}

	private record WsConnectingSettings(
			URI uri,
			boolean allowBadCerts
	) {
	}

	private final class ActWsClientInternal extends WebSocketClient {

		private volatile ClientState clientState = ClientState.NOT_STARTED;
		private final WsConnectingSettings settings;

		public ActWsClientInternal(WsConnectingSettings settings) {
			super(settings.uri);
			this.settings = settings;
			if (settings.allowBadCerts && "wss".equalsIgnoreCase(getURI().getScheme())) {


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

		@Override
		public void connect() {
			this.clientState = ClientState.OPENING;
			super.connect();
		}

		@Override
		public void onOpen(ServerHandshake serverHandshake) {
			log.info("Open: {}", serverHandshake);
			subscribeEvents();
			this.clientState = ClientState.CONNECTED;
			onClientConnected(this);
		}

		@Override
		public void onMessage(String s) {
			log.trace("WS Message: {}", s);
			submitEvent(new ActWsRawMsg(s));
		}

		@Override
		public void onClose(int i, String s, boolean b) {
			log.info("Close: {};{};{}", i, s, b);
			onClientDisconnected(this);
		}

		@Override
		public void onError(Exception e) {
			if (e.getMessage().equals("Connection refused: connect")) {
				log.info("WS Connection refused, waiting then trying again ({})", this.uri);
			}
			else {
				log.error("WS Error!", e);
			}
			onClientDisconnected(this);
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

		private void submitEvent(Event event) {
			submitEventFrom(this, event);
		}
	}

	private final Consumer<Event> eventConsumer;
	private final PrimaryLogSource pls;
	private final WsState state = new WsState();

	private void submitEventFrom(ActWsClientInternal client, Event event) {
		if (client == currentClient) {
			eventConsumer.accept(event);
		}
		else {
			log.warn("Event rejected due to stale WS client: {}", event);
		}
	}

	// TODO: is volatile needed?
	private volatile @Nullable ActWsClientInternal currentClient;

	private void recheckState() {
		ActWsClientInternal cc = currentClient;
		state.setConnected(cc != null && cc.clientState == ClientState.CONNECTED);
	}

	private void onClientConnected(ActWsClientInternal client) {
		// Only care if this is the current client
		if (client == currentClient) {
			log.info("onClientConnected");
			eventConsumer.accept(new ActWsConnectedEvent());
		}
		recheckState();
	}

	private void onClientDisconnected(ActWsClientInternal client) {
		if (client == currentClient) {
			currentClient = null;
			log.info("onClientDisconnected");
			eventConsumer.accept(new ActWsDisconnectedEvent());
		}
		recheckState();
	}

	public ActWsLogSource(EventMaster master, StateStore stateStore, PersistenceProvider pers, PrimaryLogSource pls) {
		this.uriSetting = new WsURISetting(pers, "actws-uri", defaultUri);
		this.allowBadCert = new BooleanSetting(pers, "acts-allow-bad-cert", false);
		this.eventConsumer = master::pushEvent;
		this.allowTts = new BooleanSetting(pers, "actws-allow-tts", true);
		this.allowSound = new BooleanSetting(pers, "actws-allow-sound", false);
		this.fastRefreshNonCombatant = new BooleanSetting(pers, "actws-fast-noncombatants-refresh", false);
		this.pls = pls;
		uriSetting.addListener(this::makeConnected);
		allowBadCert.addListener(this::makeConnected);
		// TODO: drop this
		stateStore.putCustom(WsState.class, state);
	}

	private final AtomicInteger rseqCounter = new AtomicInteger();

	private void makeConnected() {
		makeConnected(false);
	}

	private WsConnectingSettings currentSettings() {
		return new WsConnectingSettings(uriSetting.get(), allowBadCert.get());
	}

	private void clearOldClient() {
		// Does an old client need to be shut down?
		@Nullable ActWsClientInternal oldClient;
		synchronized (stateLock) {
			oldClient = currentClient;
			currentClient = null;
		}
		if (oldClient != null) {
			log.info("Clearing old client");
			eventConsumer.accept(new ActWsDisconnectedEvent());
			@Nullable ActWsClientInternal finalOldClient = oldClient;
			taskPool.submit(() -> {
				finalOldClient.close();
			});
		}
		else {
			log.info("No old client to clear");
		}
	}

	private void makeConnected(boolean forceReconnect) {
		log.info("makeConnected: {}", forceReconnect);
		// Does a new client need to be opened?
		@Nullable ActWsClientInternal newClient = null;
		synchronized (stateLock) {
			WsConnectingSettings newSettings = currentSettings();
			ActWsClientInternal cc = currentClient;
			WsConnectingSettings oldSettings = cc == null ? null : cc.settings;
			if (forceReconnect || !newSettings.equals(oldSettings)) {
				clearOldClient();
				log.info("Creating new WS client");
				newClient = new ActWsClientInternal(newSettings);
				this.currentClient = newClient;
			}
		}
		if (newClient != null) {
			newClient.connect();
		}
	}

	private void sendImmediate(String value) {
		ActWsClientInternal cc = currentClient;
		if (cc != null) {
			cc.send(value);
		}
	}

	// TODO: Request* methods should only work if we are running live
	@LiveOnly
	@HandleEvents
	public void requestVersion(EventContext context, ActWsConnectedEvent event) {
		try {
			sendImmediate(mapper.writeValueAsString(Map.of("call", "getVersion", "rseq", "getVersion")));
		}
		catch (JacksonException e) {
			throw new RuntimeException(e);
		}
	}

	@LiveOnly
	@HandleEvents
	public void requestLanguage(EventContext context, ActWsConnectedEvent event) {
		try {
			sendImmediate(mapper.writeValueAsString(Map.of("call", "getLanguage", "rseq", "getLanguage")));
		}
		catch (JacksonException e) {
			throw new RuntimeException(e);
		}
	}


	@LiveOnly
	@HandleEvents
	public void getCombatants(EventContext context, RefreshCombatantsRequest event) {
		sendImmediate(allCbtRequest);
	}

	@LiveOnly
	@HandleEvents
	public void getCombatant(EventContext context, RefreshSpecificCombatantsRequest event) {
		// Trying to do this to avoid some overhead, not sure if it's actually better
		String myRequest = specificCbtRequestTemplate.replaceAll("123456", event.getCombatants().stream().map(Object::toString).collect(Collectors.joining(", ")));
		sendImmediate(myRequest);
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
		sendImmediate(string);
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
	@Deprecated
	public void requestReconnect(EventContext context, ActWsReconnectRequest event) {
		log.info("Forcing connection closed, should auto-reconnect");
		doReconnect();
	}

	@LiveOnly
	@HandleEvents
	public void reconnectActWs(EventContext context, ActWsDisconnectedEvent event) {
		log.info("Disconnected, reconnecting");
		doReconnect();
	}

	private final AtomicInteger reconnectRequestCounter = new AtomicInteger();

	private void doReconnect() {
		// Only want one such task running
		int expected = reconnectRequestCounter.incrementAndGet();
		taskPool.submit(() -> {
			try {
				log.info("doReconnect start");
				if (expected != reconnectRequestCounter.get()) {
					log.info("doReconnect early out 1");
					return;
				}
				clearOldClient();
				Thread.sleep(2_500);
				if (expected != reconnectRequestCounter.get()) {
					log.info("doReconnect early out 2");
					return;
				}
				makeConnected(false);
				log.info("doReconnect end");
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public void start() {
		pls.setLogSource(KnownLogSource.WEBSOCKET_LIVE);
		makeConnected();
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

	// Debug commands
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

}
